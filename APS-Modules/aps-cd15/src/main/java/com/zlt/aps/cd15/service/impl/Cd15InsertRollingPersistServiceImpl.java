package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15InsertRollingOutput;
import com.zlt.aps.cd15.engine.model.Cd15InsertLaneAllocationDraft;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15UnscheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15InsertRollingPersistService;
import com.zlt.aps.cd15.service.Cd15AutoScheduleVersionVerifier;
import com.zlt.aps.cd15.service.Cd15ScheduleNumberService;
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
public class Cd15InsertRollingPersistServiceImpl implements Cd15InsertRollingPersistService {

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;
    private final Cd15ScheduleResultLogMapper logMapper;
    private final Cd15UnscheduleResultMapper unscheduleMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleVersionVerifier versionVerifier;
    private final Cd15ScheduleNumberService numberService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(String taskId, Cd15InsertOrderRequest request,
                        Cd15InsertRollingOutput output, RLock lock) {
        this.persistInternal(taskId, request, output, lock, "INSERT_ORDER",
                "INSERT_ORDER_ROLLING", "INSERT_ORDER_DEGRADE", "插单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistTransfer(String taskId, Cd15TransferMachineRequest request,
                                Cd15InsertRollingOutput output, RLock lock) {
        this.persistInternal(taskId, request, output, lock, "TRANSFER_MACHINE",
                "TRANSFER_MACHINE_ROLLING", "TRANSFER_MACHINE_DEGRADE", "转机台");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistChangeQty(String taskId, Cd15ChangeQtyRequest request,
                                 Cd15InsertRollingOutput output, RLock lock) {
        this.persistInternal(taskId, request, output, lock, "CHANGE_QTY",
                "CHANGE_QTY_ROLLING", "CHANGE_QTY_DEGRADE", "调量");
    }

    private void persistInternal(String taskId, Object request,
                                 Cd15InsertRollingOutput output, RLock lock,
                                 String createReason, String updateReason,
                                 String degradeReason, String actionName) {
        this.validateCommitState(taskId, lock);
        versionVerifier.verify(output.getContext());
        Cd15ScheduleResult insertResult = output.getInsertResult();
        this.applyStorageLaneCodes(output);
        boolean insertSaved = this.hasScheduledQuantity(insertResult);
        if (insertSaved) {
            insertResult.setOrderNo(numberService.nextOrderNo(output.getBatchNo()));
            insertResult.setGroupNo(insertResult.getOrderNo());
            if (resultMapper.insert(insertResult) != 1) {
                throw new IllegalStateException("保存斜裁" + actionName + "结果失败");
            }
            this.saveLog(insertResult, "CREATE", createReason, request);
        }
        List<Cd15ScheduleResult> deletedResults = output.getDeletedResults() == null
                ? Collections.emptyList() : output.getDeletedResults();
        for (Cd15ScheduleResult result : output.getUpdatedResults()) {
            if (result.getId() == null || this.updateScheduleResultForcibly(result) != 1) {
                throw new IllegalStateException("更新" + actionName + "受影响排程结果失败");
            }
            if (result.getReleaseStatus() != null && !"0".equals(result.getReleaseStatus())) {
                result.setReleaseStatus("5");
                if (this.updateScheduleResultForcibly(result) != 1) {
                    throw new IllegalStateException("更新" + actionName + "受影响发布状态失败");
                }
                this.saveLog(result, "UPDATE", degradeReason, request);
            } else {
                this.saveLog(result, "UPDATE", updateReason, request);
            }
        }
        for (Cd15ScheduleResult result : deletedResults) {
            if (result.getId() == null) {
                throw new IllegalStateException("删除" + actionName + "转出排程结果失败");
            }
            this.saveLog(result, "DELETE", updateReason, request);
            laneMapper.delete(new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                    .eq(Cd15ScheduleLaneAllocation::getScheduleResultId, result.getId()));
            if (resultMapper.deleteById(result.getId()) != 1) {
                throw new IllegalStateException("删除" + actionName + "转出排程结果失败");
            }
        }
        for (Cd15UnscheduleResult unscheduled : output.getUnscheduledResults()) {
            if (unscheduleMapper.insert(unscheduled) != 1) {
                throw new IllegalStateException("保存" + actionName + "未排结果失败");
            }
        }
        this.replaceLaneAllocations(output, insertSaved);
        if (!taskService.markSuccessInCurrentTransaction(taskId, output.getBatchNo())) {
            throw new IllegalStateException(actionName + "任务状态已变化，不能提交结果");
        }
        log.info("[斜裁{}] 最终事务提交完成, taskId={}, batchNo={}, insertSaved={}, updatedCount={}, deletedCount={}, unscheduledCount={}",
                actionName, taskId, output.getBatchNo(), insertSaved, output.getUpdatedResults().size(),
                deletedResults.size(), output.getUnscheduledResults().size());
    }
    private void applyStorageLaneCodes(Cd15InsertRollingOutput output) {
        List<Cd15InsertLaneAllocationDraft> lanes = output.getLaneAllocations() == null
                ? Collections.emptyList() : output.getLaneAllocations();
        if (output.getInsertResult() != null) {
            output.getInsertResult().setStorageLaneCode(joinLaneCodes(lanes.stream()
                    .filter(Cd15InsertLaneAllocationDraft::isInsertResult)
                    .collect(Collectors.toList())));
        }
        output.getUpdatedResults().forEach(result -> result.setStorageLaneCode(
                joinLaneCodes(lanes.stream()
                        .filter(item -> !item.isInsertResult())
                        .filter(item -> result.getId().equals(item.getScheduleResultId()))
                        .collect(Collectors.toList()))));
    }

    private String joinLaneCodes(List<Cd15InsertLaneAllocationDraft> lanes) {
        String value = lanes.stream().map(Cd15InsertLaneAllocationDraft::getLaneCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .distinct().collect(Collectors.joining(","));
        return value.isEmpty() ? null : value;
    }

    private void replaceLaneAllocations(Cd15InsertRollingOutput output, boolean insertSaved) {
        List<Cd15InsertLaneAllocationDraft> drafts = output.getLaneAllocations() == null
                ? Collections.emptyList() : output.getLaneAllocations();
        Set<Long> updatedIds = output.getUpdatedResults().stream()
                .map(Cd15ScheduleResult::getId).collect(Collectors.toSet());
        if (!updatedIds.isEmpty()) {
            laneMapper.delete(new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                    .in(Cd15ScheduleLaneAllocation::getScheduleResultId, updatedIds));
        }
        for (Cd15InsertLaneAllocationDraft draft : drafts) {
            Long resultId = draft.isInsertResult()
                    ? (insertSaved ? output.getInsertResult().getId() : null)
                    : draft.getScheduleResultId();
            if (resultId == null) {
                continue;
            }
            Cd15ScheduleResult parent = draft.isInsertResult()
                    ? output.getInsertResult() : output.getUpdatedResults().stream()
                            .filter(item -> resultId.equals(item.getId())).findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "插单库排明细找不到主结果: " + resultId));
            if (parent == null) {
                continue;
            }
            Cd15ScheduleLaneAllocation entity = new Cd15ScheduleLaneAllocation();
            entity.setFactoryCode(parent.getFactoryCode());
            entity.setScheduleDate(parent.getScheduleDate());
            entity.setBatchNo(parent.getCd15BatchNo());
            entity.setScheduleResultId(resultId);
            entity.setOrderNo(parent.getOrderNo());
            entity.setClassField(draft.getClassField());
            entity.setShiftScheduleDate(draft.getShiftScheduleDate());
            entity.setStorageLaneCode(draft.getLaneCode());
            entity.setSteelStripCode(parent.getSteelStripCode());
            entity.setBigRollCode(parent.getBigRollCode());
            entity.setCuttingAngle(parent.getCuttingAngle());
            entity.setMachineCode(parent.getMachineCode());
            entity.setGroupNo(parent.getGroupNo());
            entity.setAllocatedQty(draft.getAllocationQuantity());
            entity.setAllocatedCartCount(draft.getVehicleCount());
            entity.setAllocationOrder(draft.getAllocationOrder());
            if (laneMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存插单库排分配失败");
            }
        }
    }

    private void validateCommitState(String taskId, RLock lock) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("插单任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("插单执行锁已失效");
        }
    }

    /**
     * 强制更新插单受影响排程结果的所有班次字段（包括 null）。
     * <p>MyBatis-Plus updateById 默认 NOT_NULL 策略会跳过 null 字段，
     * 但 clearAdjustableClassFields 清空后未走 writeClass 的字段需要保持为 null，
     * 否则会保留自动排程旧值导致 PRODUCE_ORDER 等字段重复（例如 G1301 出现两个 4）。
     * 因此用 UpdateWrapper 显式 set，确保 null 字段也被更新。</p>
     *
     * @param result 待更新的排程结果
     * @return 受影响行数
     */
    private int updateScheduleResultForcibly(Cd15ScheduleResult result) {
        UpdateWrapper<Cd15ScheduleResult> wrapper = new UpdateWrapper<>();
        wrapper.eq("ID", result.getId());
        // 动态设置 6 个 class 班次的 ScheduleDate/PlanQty/ProduceOrder/Analysis/AnalysisInput 字段
        for (int classIndex = 1; classIndex <= 8; classIndex++) {
            String dbPrefix = String.format("CLASS%d_", classIndex);
            String fieldPrefix = String.format("class%d", classIndex);
            wrapper.set(dbPrefix + "SCHEDULE_DATE",
                    result.getFieldValueByFieldName(fieldPrefix + "ScheduleDate"));
            wrapper.set(dbPrefix + "PLAN_QTY",
                    result.getFieldValueByFieldName(fieldPrefix + "PlanQty"));
            wrapper.set(dbPrefix + "PRODUCE_ORDER",
                    result.getFieldValueByFieldName(fieldPrefix + "ProduceOrder"));
            wrapper.set(dbPrefix + "ANALYSIS",
                    result.getFieldValueByFieldName(fieldPrefix + "Analysis"));
            wrapper.set(dbPrefix + "ANALYSIS_INPUT",
                    result.getFieldValueByFieldName(fieldPrefix + "AnalysisInput"));
        }
        wrapper.set("STORAGE_LANE_CODE", result.getStorageLaneCode());
        wrapper.set("RELEASE_STATUS", result.getReleaseStatus());
        return resultMapper.update(null, wrapper);
    }

    private boolean hasScheduledQuantity(Cd15ScheduleResult result) {
        if (result == null) {
            return false;
        }
        for (int classIndex = 1; classIndex <= 8; classIndex++) {
            Double quantity = (Double) result.getFieldValueByFieldName(
                    String.format("class%dPlanQty", classIndex));
            if (quantity != null && quantity > 0D) {
                return true;
            }
        }
        return false;
    }

    private void saveLog(Cd15ScheduleResult result, String logType,
                         String reasonCode, Object request) {
        Cd15ScheduleResultLog entity = new Cd15ScheduleResultLog();
        entity.setScheduleResultId(result.getId());
        entity.setFactoryCode(result.getFactoryCode());
        entity.setScheduleDate(result.getScheduleDate());
        entity.setBatchNo(result.getCd15BatchNo());
        entity.setOrderNo(result.getOrderNo());
        entity.setBigRollCode(result.getBigRollCode());
        entity.setSteelStripCode(result.getSteelStripCode());
        entity.setCuttingAngle(result.getCuttingAngle());
        entity.setMachineCode(result.getMachineCode());
        entity.setGroupNo(result.getGroupNo());
        entity.setStorageLaneCode(result.getStorageLaneCode());
        entity.setLogType(logType);
        entity.setLogTime(new Date());
        entity.setReasonCode(reasonCode);
        entity.setReasonDetail(this.reasonDetail(result, request));
        if (logMapper.insert(entity) != 1) {
            throw new IllegalStateException("保存插单调整日志失败");
        }
    }

    private String reasonDetail(Cd15ScheduleResult result, Object request) {
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
