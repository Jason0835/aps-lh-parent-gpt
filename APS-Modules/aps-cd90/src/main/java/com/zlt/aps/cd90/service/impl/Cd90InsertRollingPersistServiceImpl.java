package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResultLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;
import com.zlt.aps.cd90.engine.model.Cd90InsertLaneAllocationDraft;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultLogMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleLaneAllocationMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.service.Cd90InsertRollingPersistService;
import com.zlt.aps.cd90.service.Cd90AutoScheduleVersionVerifier;
import com.zlt.aps.cd90.service.Cd90ScheduleNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 插单滚动结果最终短事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90InsertRollingPersistServiceImpl implements Cd90InsertRollingPersistService {

    private final Cd90ScheduleResultMapper resultMapper;
    private final Cd90ScheduleLaneAllocationMapper laneMapper;
    private final Cd90ScheduleResultLogMapper logMapper;
    private final Cd90UnscheduleResultMapper unscheduleMapper;
    private final Cd90ScheduleTaskService taskService;
    private final Cd90AutoScheduleVersionVerifier versionVerifier;
    private final Cd90ScheduleNumberService numberService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(String taskId, Cd90InsertOrderRequest request,
                        Cd90InsertRollingOutput output, RLock lock) {
        this.validateCommitState(taskId, lock);
        versionVerifier.verify(output.getContext());
        Cd90ScheduleResult insertResult = output.getInsertResult();
        this.applyStorageLaneCodes(output);
        boolean insertSaved = this.hasScheduledQuantity(insertResult);
        if (insertSaved) {
            insertResult.setOrderNo(numberService.nextOrderNo(output.getBatchNo()));
            if (resultMapper.insert(insertResult) != 1) {
                throw new IllegalStateException("保存直裁插单结果失败");
            }
            this.saveLog(insertResult, "CREATE", "INSERT_ORDER", request);
        }
        for (Cd90ScheduleResult result : output.getUpdatedResults()) {
            if (result.getId() == null || resultMapper.updateById(result) != 1) {
                throw new IllegalStateException("更新插单受影响排程结果失败");
            }
            if (result.getPublishSuccessCount() != null && result.getPublishSuccessCount() > 0) {
                result.setIsRelease("5");
                if (resultMapper.updateById(result) != 1) {
                    throw new IllegalStateException("更新插单受影响发布状态失败");
                }
                this.saveLog(result, "UPDATE", "INSERT_ORDER_DEGRADE", request);
            } else {
                this.saveLog(result, "UPDATE", "INSERT_ORDER_ROLLING", request);
            }
        }
        for (Cd90UnscheduleResult unscheduled : output.getUnscheduledResults()) {
            if (unscheduleMapper.insert(unscheduled) != 1) {
                throw new IllegalStateException("保存插单未排结果失败");
            }
        }
        this.replaceLaneAllocations(output, insertSaved);
        if (!taskService.markSuccessInCurrentTransaction(taskId, output.getBatchNo())) {
            throw new IllegalStateException("插单任务状态已变化，不能提交结果");
        }
        log.info("[直裁插单] 最终事务提交完成, taskId={}, batchNo={}, insertSaved={}, "
                        + "updatedCount={}, unscheduledCount={}",
                taskId, output.getBatchNo(), insertSaved, output.getUpdatedResults().size(),
                output.getUnscheduledResults().size());
    }

    private void applyStorageLaneCodes(Cd90InsertRollingOutput output) {
        List<Cd90InsertLaneAllocationDraft> lanes = output.getLaneAllocations() == null
                ? Collections.emptyList() : output.getLaneAllocations();
        output.getInsertResult().setStorageLaneCode(joinLaneCodes(lanes.stream()
                .filter(Cd90InsertLaneAllocationDraft::isInsertResult)
                .collect(Collectors.toList())));
        output.getUpdatedResults().forEach(result -> result.setStorageLaneCode(
                joinLaneCodes(lanes.stream()
                        .filter(item -> !item.isInsertResult())
                        .filter(item -> result.getId().equals(item.getScheduleResultId()))
                        .collect(Collectors.toList()))));
    }

    private String joinLaneCodes(List<Cd90InsertLaneAllocationDraft> lanes) {
        String value = lanes.stream().map(Cd90InsertLaneAllocationDraft::getLaneCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .distinct().collect(Collectors.joining(","));
        return value.isEmpty() ? null : value;
    }

    private void replaceLaneAllocations(Cd90InsertRollingOutput output, boolean insertSaved) {
        List<Cd90InsertLaneAllocationDraft> drafts = output.getLaneAllocations() == null
                ? Collections.emptyList() : output.getLaneAllocations();
        Set<Long> updatedIds = output.getUpdatedResults().stream()
                .map(Cd90ScheduleResult::getId).collect(Collectors.toSet());
        if (!updatedIds.isEmpty()) {
            laneMapper.delete(new LambdaQueryWrapper<Cd90ScheduleLaneAllocation>()
                    .in(Cd90ScheduleLaneAllocation::getScheduleResultId, updatedIds));
        }
        for (Cd90InsertLaneAllocationDraft draft : drafts) {
            Long resultId = draft.isInsertResult()
                    ? (insertSaved ? output.getInsertResult().getId() : null)
                    : draft.getScheduleResultId();
            if (resultId == null) {
                continue;
            }
            Cd90ScheduleResult parent = draft.isInsertResult()
                    ? output.getInsertResult() : output.getUpdatedResults().stream()
                            .filter(item -> resultId.equals(item.getId())).findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "插单库排明细找不到主结果: " + resultId));
            Cd90ScheduleLaneAllocation entity = new Cd90ScheduleLaneAllocation();
            entity.setFactoryCode(parent.getFactoryCode());
            entity.setScheduleDate(parent.getScheduleDate());
            entity.setBatchNo(parent.getBatchNo());
            entity.setScheduleResultId(resultId);
            entity.setOrderNo(parent.getOrderNo());
            entity.setClassField(draft.getClassField());
            entity.setShiftScheduleDate(draft.getShiftScheduleDate());
            entity.setStorageLaneCode(draft.getLaneCode());
            entity.setClothCode(parent.getClothCode());
            entity.setAllocatedQty(draft.getAllocationQuantity().doubleValue());
            entity.setAllocatedCartCount(draft.getVehicleCount());
            entity.setAllocationOrder(draft.getAllocationOrder());
            if (laneMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存插单库排分配失败");
            }
        }
    }

    private void validateCommitState(String taskId, RLock lock) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("插单任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("插单执行锁已失效");
        }
    }

    private boolean hasScheduledQuantity(Cd90ScheduleResult result) {
        for (int classIndex = 1; classIndex <= 6; classIndex++) {
            Double quantity = (Double) result.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            if (quantity != null && quantity > 0D) {
                return true;
            }
        }
        return false;
    }

    private void saveLog(Cd90ScheduleResult result, String logType,
                         String reasonCode, Cd90InsertOrderRequest request) {
        Cd90ScheduleResultLog entity = new Cd90ScheduleResultLog();
        entity.setScheduleResultId(result.getId());
        entity.setFactoryCode(result.getFactoryCode());
        entity.setScheduleDate(result.getScheduleDate());
        entity.setBatchNo(result.getBatchNo());
        entity.setOrderNo(result.getOrderNo());
        entity.setBigRollCode(result.getBigRollCode());
        entity.setClothCode(result.getClothCode());
        entity.setMachineCode(result.getMachineCode());
        entity.setStorageLaneCode(result.getStorageLaneCode());
        entity.setLogType(logType);
        entity.setLogTime(new Date());
        entity.setReasonCode(reasonCode);
        entity.setReasonDetail(this.reasonDetail(result, request));
        if (logMapper.insert(entity) != 1) {
            throw new IllegalStateException("保存插单调整日志失败");
        }
    }

    private String reasonDetail(Cd90ScheduleResult result, Cd90InsertOrderRequest request) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("request", request);
        detail.put("result", result);
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("插单调整日志序列化失败", exception);
        }
    }
}
