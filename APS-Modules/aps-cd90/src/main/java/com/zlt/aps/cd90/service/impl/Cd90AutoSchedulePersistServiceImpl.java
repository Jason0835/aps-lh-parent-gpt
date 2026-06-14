package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleParamSnapshot;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleDemandSnapshot;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResultLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.model.Cd90LaneAllocationDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleExplainLogDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleResultDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleShiftSlotDraft;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleLaneAllocationMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleParamSnapshotMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleDemandSnapshotMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultLogMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.service.Cd90AutoScheduleDraftMapper;
import com.zlt.aps.cd90.service.Cd90AutoSchedulePersistService;
import com.zlt.aps.cd90.service.Cd90AutoScheduleVersionVerifier;
import com.zlt.aps.cd90.service.Cd90ScheduleNumberService;
import com.zlt.aps.cd90.service.Cd90ScheduleOverwriteValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 自动排程最终短事务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoSchedulePersistServiceImpl implements Cd90AutoSchedulePersistService {

    private final Cd90ScheduleResultMapper resultMapper;
    private final Cd90ScheduleLaneAllocationMapper laneMapper;
    private final Cd90ScheduleResultLogMapper logMapper;
    private final Cd90UnscheduleResultMapper unscheduleMapper;
    private final Cd90ScheduleParamSnapshotMapper paramSnapshotMapper;
    private final Cd90ScheduleDemandSnapshotMapper demandSnapshotMapper;
    private final Cd90ScheduleTaskService taskService;
    private final Cd90AutoScheduleVersionVerifier versionVerifier;
    private final Cd90ScheduleOverwriteValidator overwriteValidator;
    private final Cd90ScheduleNumberService numberService;
    private final Cd90AutoScheduleDraftMapper draftMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String persist(String taskId, Cd90AutoScheduleContext context,
                          Cd90AutoScheduleOutputDraft output, RLock lock) {
        validateCommitState(taskId, lock);
        versionVerifier.verify(context);
        Date scheduleDate = date(context.getScheduleDate());
        List<Cd90ScheduleResult> oldResults = resultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode, context.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate, scheduleDate));
        if (overwriteValidator.validate(oldResults, true).isRejected()) {
            throw new IllegalStateException(overwriteValidator.validate(oldResults, true).getMessage());
        }

        String batchNo = numberService.nextBatchNo(context.getScheduleDate());
        Map<String, Cd90ScheduleResult> persistedByKey = saveResults(context, output, batchNo);
        saveLanes(context, output, batchNo, persistedByKey);
        saveLogs(context, output, batchNo, persistedByKey);
        saveUnscheduled(context, output, batchNo);
        saveParameterSnapshots(context, batchNo);
        saveDemandSnapshots(context, output, batchNo);
        invalidateOldResults(oldResults, batchNo);
        if (!taskService.markSuccessInCurrentTransaction(taskId, batchNo)) {
            throw new IllegalStateException("自动排程任务状态已变化，不能提交结果");
        }
        log.info("[直裁自动排程] 最终事务提交完成, taskId={}, batchNo={}, resultCount={}",
                taskId, batchNo, persistedByKey.size());
        return batchNo;
    }

    private void validateCommitState(String taskId, RLock lock) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("自动排程任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("自动排程执行锁已失效");
        }
    }

    private Map<String, Cd90ScheduleResult> saveResults(Cd90AutoScheduleContext context,
                                                         Cd90AutoScheduleOutputDraft output,
                                                         String batchNo) {
        Map<String, Cd90ScheduleResult> result = new HashMap<>();
        for (Cd90ScheduleResultDraft draft : output.getScheduleResults()) {
            Cd90ScheduleResult entity = draftMapper.toScheduleResult(context.getFactoryCode(),
                    context.getScheduleDate(), batchNo, numberService.nextOrderNo(batchNo), draft);
            if (resultMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存直裁排程主结果失败");
            }
            result.put(draft.getResultKey(), entity);
        }
        return result;
    }

    private void saveLanes(Cd90AutoScheduleContext context, Cd90AutoScheduleOutputDraft output,
                           String batchNo, Map<String, Cd90ScheduleResult> resultByKey) {
        Map<String, Integer> orderByResultClass = new HashMap<>();
        for (Cd90LaneAllocationDraft draft : output.getLaneAllocations()) {
            Cd90ScheduleResult parent = requiredParent(resultByKey, draft.getResultKey());
            String orderKey = draft.getResultKey() + "|" + draft.getClassField();
            int order = orderByResultClass.merge(orderKey, 1, Integer::sum);
            Cd90ScheduleLaneAllocation entity = new Cd90ScheduleLaneAllocation();
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setScheduleResultId(parent.getId());
            entity.setOrderNo(parent.getOrderNo());
            entity.setClassField(draft.getClassField());
            entity.setShiftScheduleDate(slotDate(output, draft));
            entity.setStorageLaneCode(draft.getLaneCode());
            entity.setClothCode(parent.getClothCode());
            entity.setAllocatedQty(draft.getAllocationQuantity().doubleValue());
            entity.setAllocatedCartCount(draft.getVehicleCount());
            entity.setAllocationOrder(order);
            if (laneMapper.insert(entity) != 1) throw new IllegalStateException("保存库排分配失败");
        }
    }

    private void saveLogs(Cd90AutoScheduleContext context, Cd90AutoScheduleOutputDraft output,
                          String batchNo, Map<String, Cd90ScheduleResult> resultByKey) {
        for (Cd90ScheduleExplainLogDraft draft : output.getExplainLogs()) {
            Cd90ScheduleResult parent = requiredParent(resultByKey, draft.getResultKey());
            Cd90ScheduleResultLog entity = new Cd90ScheduleResultLog();
            entity.setScheduleResultId(parent.getId());
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setOrderNo(parent.getOrderNo());
            entity.setBigRollCode(parent.getBigRollCode());
            entity.setClothCode(parent.getClothCode());
            entity.setMachineCode(parent.getMachineCode());
            entity.setStorageLaneCode(parent.getStorageLaneCode());
            entity.setLogType(draft.getLogType());
            entity.setLogTime(new Date());
            entity.setReasonDetail(json(draft.getShiftDetails()));
            if (logMapper.insert(entity) != 1) throw new IllegalStateException("保存排程解释日志失败");
        }
    }

    private void saveUnscheduled(Cd90AutoScheduleContext context, Cd90AutoScheduleOutputDraft output,
                                 String batchNo) {
        output.getUnscheduledResults().forEach(source -> {
            Cd90UnscheduleResult entity = draftMapper.toUnscheduleResult(
                    context.getFactoryCode(), date(context.getScheduleDate()), batchNo, source);
            if (unscheduleMapper.insert(entity) != 1) throw new IllegalStateException("保存未排结果失败");
        });
    }

    private void saveParameterSnapshots(Cd90AutoScheduleContext context, String batchNo) {
        context.getParameters().getSourceValues().forEach((code, value) -> {
            Cd90ScheduleParamSnapshot entity = new Cd90ScheduleParamSnapshot();
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setParamCode(code);
            entity.setParamValue(value);
            entity.setParamFingerprint(context.getParameters().getFingerprint());
            if (paramSnapshotMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存参数快照失败");
            }
        });
    }

    private void saveDemandSnapshots(Cd90AutoScheduleContext context,
                                     Cd90AutoScheduleOutputDraft output, String batchNo) {
        if (output.getDemandTraces() == null) return;
        for (Cd90ScheduleAttemptTrace trace : output.getDemandTraces()) {
            if (trace.getNetDemandQuantity() == null || trace.getNetDemandQuantity().signum() <= 0) continue;
            Cd90ScheduleDemandSnapshot entity = new Cd90ScheduleDemandSnapshot();
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setClothCode(trace.getClothCode());
            entity.setClassField(trace.getClassField());
            entity.setDemandTime(context.getShifts().stream()
                    .filter(item -> trace.getClassField().equals(item.getClassField()))
                    .map(item -> Date.from(item.getStartTime().atZone(ZoneId.systemDefault()).toInstant()))
                    .findFirst().orElse(date(context.getScheduleDate())));
            entity.setDemandQty(trace.getNetDemandQuantity());
            entity.setSourceVersion(context.getInputVersionFingerprint());
            if (demandSnapshotMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存需求快照失败");
            }
        }
    }

    private void invalidateOldResults(List<Cd90ScheduleResult> oldResults, String batchNo) {
        List<Long> ids = new ArrayList<>();
        List<String> oldBatchNos = new ArrayList<>();
        oldResults.forEach(item -> {
            if (item.getId() != null) ids.add(item.getId());
            if (item.getBatchNo() != null && !batchNo.equals(item.getBatchNo())) {
                oldBatchNos.add(item.getBatchNo());
            }
        });
        if (ids.isEmpty()) return;
        laneMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleLaneAllocation>()
                .in(Cd90ScheduleLaneAllocation::getScheduleResultId, ids)
                .set(Cd90ScheduleLaneAllocation::getIsDelete, 1));
        logMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleResultLog>()
                .in(Cd90ScheduleResultLog::getScheduleResultId, ids)
                .set(Cd90ScheduleResultLog::getIsDelete, 1));
        if (!oldBatchNos.isEmpty()) {
            unscheduleMapper.update(null, new LambdaUpdateWrapper<Cd90UnscheduleResult>()
                    .in(Cd90UnscheduleResult::getBatchNo, oldBatchNos)
                    .set(Cd90UnscheduleResult::getIsDelete, 1));
            paramSnapshotMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleParamSnapshot>()
                    .in(Cd90ScheduleParamSnapshot::getBatchNo, oldBatchNos)
                    .set(Cd90ScheduleParamSnapshot::getIsDelete, 1));
            demandSnapshotMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleDemandSnapshot>()
                    .in(Cd90ScheduleDemandSnapshot::getBatchNo, oldBatchNos)
                    .set(Cd90ScheduleDemandSnapshot::getIsDelete, 1));
        }
        resultMapper.update(null, new LambdaUpdateWrapper<Cd90ScheduleResult>()
                .in(Cd90ScheduleResult::getId, ids)
                .ne(Cd90ScheduleResult::getBatchNo, batchNo)
                .set(Cd90ScheduleResult::getIsDelete, 1));
    }

    private Cd90ScheduleResult requiredParent(Map<String, Cd90ScheduleResult> values, String key) {
        Cd90ScheduleResult result = values.get(key);
        if (result == null || result.getId() == null) {
            throw new IllegalStateException("输出草稿未找到已保存的主结果: " + key);
        }
        return result;
    }

    private Date slotDate(Cd90AutoScheduleOutputDraft output, Cd90LaneAllocationDraft lane) {
        return output.getScheduleResults().stream()
                .filter(item -> lane.getResultKey().equals(item.getResultKey()))
                .flatMap(item -> item.getShiftSlots().stream())
                .filter(slot -> lane.getClassField().equals(slot.getClassField()))
                .map(Cd90ScheduleShiftSlotDraft::getScheduleDate).findFirst()
                .map(this::date).orElseThrow(() -> new IllegalStateException("库排分配缺少班次日期"));
    }

    private Date date(LocalDate value) {
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("自动排程解释日志序列化失败", exception);
        }
    }
}
