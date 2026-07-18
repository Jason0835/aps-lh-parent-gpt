package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleParamSnapshot;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleDemandSnapshot;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleOutputDraft;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleExplainLogDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleShiftSlotDraft;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleParamSnapshotMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleDemandSnapshotMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15UnscheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15AutoScheduleDraftMapper;
import com.zlt.aps.cd15.service.Cd15AutoSchedulePersistService;
import com.zlt.aps.cd15.service.Cd15AutoScheduleVersionVerifier;
import com.zlt.aps.cd15.service.Cd15ScheduleNumberService;
import com.zlt.aps.cd15.service.Cd15ScheduleOverwriteValidator;
import com.zlt.aps.cd15.engine.algorithm.Cd15AutoScheduleRuntimeGuard;
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
public class Cd15AutoSchedulePersistServiceImpl implements Cd15AutoSchedulePersistService {

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;
    private final Cd15ScheduleResultLogMapper logMapper;
    private final Cd15UnscheduleResultMapper unscheduleMapper;
    private final Cd15ScheduleParamSnapshotMapper paramSnapshotMapper;
    private final Cd15ScheduleDemandSnapshotMapper demandSnapshotMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleVersionVerifier versionVerifier;
    private final Cd15ScheduleOverwriteValidator overwriteValidator;
    private final Cd15ScheduleNumberService numberService;
    private final Cd15AutoScheduleDraftMapper draftMapper;
    private final ObjectMapper objectMapper;
    private final Cd15AutoScheduleRuntimeGuard runtimeGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String persist(String taskId, Cd15AutoScheduleContext context,
                          Cd15AutoScheduleOutputDraft output, RLock lock) {
        validateCommitState(taskId, lock);
        runtimeGuard.checkNotTimedOut(context, "最终事务开始");
        versionVerifier.verify(context);
        Date scheduleDate = date(context.getScheduleDate());
        List<Cd15ScheduleResult> oldResults = resultMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .eq(Cd15ScheduleResult::getFactoryCode, context.getFactoryCode())
                        .eq(Cd15ScheduleResult::getScheduleDate, scheduleDate));
        if (overwriteValidator.validate(oldResults, true).isRejected()) {
            throw new IllegalStateException(overwriteValidator.validate(oldResults, true).getMessage());
        }

        String batchNo = numberService.nextBatchNo(context.getScheduleDate());
        Map<String, Cd15ScheduleResult> persistedByKey = saveResults(context, output, batchNo);
        saveLanes(context, output, batchNo, persistedByKey);
        saveLogs(context, output, batchNo, persistedByKey);
        saveUnscheduled(context, output, batchNo);
        saveParameterSnapshots(context, batchNo);
        saveDemandSnapshots(context, output, batchNo);
        invalidateOldResults(oldResults, batchNo);
        if (!taskService.markSuccessInCurrentTransaction(taskId, batchNo)) {
            throw new IllegalStateException("自动排程任务状态已变化，不能提交结果");
        }
        log.info("[斜裁自动排程] 最终事务提交完成, taskId={}, batchNo={}, resultCount={}",
                taskId, batchNo, persistedByKey.size());
        return batchNo;
    }

    private void validateCommitState(String taskId, RLock lock) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("自动排程任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("自动排程执行锁已失效");
        }
    }

    private Map<String, Cd15ScheduleResult> saveResults(Cd15AutoScheduleContext context,
                                                         Cd15AutoScheduleOutputDraft output,
                                                         String batchNo) {
        Map<String, Cd15ScheduleResult> result = new HashMap<>();
        Map<String, String> orderNoBySplitGroup = new HashMap<>();
        for (Cd15ScheduleResultDraft draft : output.getScheduleResults()) {
            String orderNo = draft.getSplitGroupKey() == null
                    ? numberService.nextOrderNo(batchNo)
                    : orderNoBySplitGroup.computeIfAbsent(draft.getSplitGroupKey(),
                            key -> numberService.nextOrderNo(batchNo));
            Cd15ScheduleResult entity = draftMapper.toScheduleResult(context.getFactoryCode(),
                    context.getScheduleDate(), batchNo, orderNo, draft);
            if (resultMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存斜裁排程主结果失败");
            }
            result.put(draft.getResultKey(), entity);
        }
        return result;
    }

    private void saveLanes(Cd15AutoScheduleContext context, Cd15AutoScheduleOutputDraft output,
                           String batchNo, Map<String, Cd15ScheduleResult> resultByKey) {
        Map<String, Integer> orderByResultClass = new HashMap<>();
        for (Cd15LaneAllocationDraft draft : output.getLaneAllocations()) {
            Cd15ScheduleResult parent = requiredParent(resultByKey, draft.getResultKey());
            String orderKey = draft.getResultKey() + "|" + draft.getClassField();
            int order = orderByResultClass.merge(orderKey, 1, Integer::sum);
            Cd15ScheduleLaneAllocation entity = new Cd15ScheduleLaneAllocation();
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setScheduleResultId(parent.getId());
            entity.setOrderNo(parent.getOrderNo());
            entity.setClassField(draft.getClassField());
            entity.setShiftScheduleDate(slotDate(output, draft));
            entity.setStorageLaneCode(draft.getLaneCode());
            entity.setSteelStripCode(parent.getSteelStripCode());
            entity.setBigRollCode(parent.getBigRollCode());
            entity.setCuttingAngle(parent.getCuttingAngle());
            entity.setMachineCode(parent.getMachineCode());
            entity.setGroupNo(parent.getGroupNo());
            entity.setAllocatedQty(draft.getAllocationQuantity());
            entity.setAllocatedCartCount(draft.getVehicleCount());
            entity.setAllocationOrder(order);
            if (laneMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存库排分配失败");
            }
        }
    }

    private void saveLogs(Cd15AutoScheduleContext context, Cd15AutoScheduleOutputDraft output,
                          String batchNo, Map<String, Cd15ScheduleResult> resultByKey) {
        for (Cd15ScheduleExplainLogDraft draft : output.getExplainLogs()) {
            Cd15ScheduleResult parent = requiredParent(resultByKey, draft.getResultKey());
            Cd15ScheduleResultLog entity = new Cd15ScheduleResultLog();
            entity.setScheduleResultId(parent.getId());
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setOrderNo(parent.getOrderNo());
            entity.setBigRollCode(parent.getBigRollCode());
            entity.setSteelStripCode(parent.getSteelStripCode());
            entity.setMachineCode(parent.getMachineCode());
            entity.setStorageLaneCode(parent.getStorageLaneCode());
            entity.setLogType(draft.getLogType());
            entity.setLogTime(new Date());
            entity.setReasonDetail(json(draft.getShiftDetails()));
            if (logMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存排程解释日志失败");
            }
        }
    }

    private void saveUnscheduled(Cd15AutoScheduleContext context, Cd15AutoScheduleOutputDraft output,
                                 String batchNo) {
        output.getUnscheduledResults().forEach(source -> {
            Cd15UnscheduleResult entity = draftMapper.toUnscheduleResult(
                    context.getFactoryCode(), date(context.getScheduleDate()), batchNo, source);
            if (unscheduleMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存未排结果失败");
            }
        });
    }

    private void saveParameterSnapshots(Cd15AutoScheduleContext context, String batchNo) {
        context.getParameters().getSourceValues().forEach((code, value) -> {
            Cd15ScheduleParamSnapshot entity = new Cd15ScheduleParamSnapshot();
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

    private void saveDemandSnapshots(Cd15AutoScheduleContext context,
                                     Cd15AutoScheduleOutputDraft output, String batchNo) {
        if (output.getDemandTraces() == null) {
            return;
        }
        for (Cd15ScheduleAttemptTrace trace : output.getDemandTraces()) {
            if (trace.getNetDemandQuantity() == null
                    || trace.getNetDemandQuantity().signum() <= 0) {
                continue;
            }
            Cd15ScheduleDemandSnapshot entity = new Cd15ScheduleDemandSnapshot();
            entity.setFactoryCode(context.getFactoryCode());
            entity.setScheduleDate(date(context.getScheduleDate()));
            entity.setBatchNo(batchNo);
            entity.setSteelStripCode(trace.getSteelStripCode());
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

    private void invalidateOldResults(List<Cd15ScheduleResult> oldResults, String batchNo) {
        List<Long> ids = new ArrayList<>();
        List<String> oldBatchNos = new ArrayList<>();
        oldResults.forEach(item -> {
            if (item.getId() != null) ids.add(item.getId());
            if (item.getCd15BatchNo() != null && !batchNo.equals(item.getCd15BatchNo())) {
                oldBatchNos.add(item.getCd15BatchNo());
            }
        });
        if (ids.isEmpty()) {
            return;
        }
        laneMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleLaneAllocation>()
                .in(Cd15ScheduleLaneAllocation::getScheduleResultId, ids)
                .set(Cd15ScheduleLaneAllocation::getIsDelete, 1));
        logMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleResultLog>()
                .in(Cd15ScheduleResultLog::getScheduleResultId, ids)
                .set(Cd15ScheduleResultLog::getIsDelete, 1));
        if (!oldBatchNos.isEmpty()) {
            unscheduleMapper.update(null, new LambdaUpdateWrapper<Cd15UnscheduleResult>()
                    .in(Cd15UnscheduleResult::getBatchNo, oldBatchNos)
                    .set(Cd15UnscheduleResult::getIsDelete, 1));
            paramSnapshotMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleParamSnapshot>()
                    .in(Cd15ScheduleParamSnapshot::getBatchNo, oldBatchNos)
                    .set(Cd15ScheduleParamSnapshot::getIsDelete, 1));
            demandSnapshotMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleDemandSnapshot>()
                    .in(Cd15ScheduleDemandSnapshot::getBatchNo, oldBatchNos)
                    .set(Cd15ScheduleDemandSnapshot::getIsDelete, 1));
        }
        resultMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleResult>()
                .in(Cd15ScheduleResult::getId, ids)
                .ne(Cd15ScheduleResult::getCd15BatchNo, batchNo)
                .set(Cd15ScheduleResult::getIsDelete, 1));
    }

    private Cd15ScheduleResult requiredParent(Map<String, Cd15ScheduleResult> values, String key) {
        Cd15ScheduleResult result = values.get(key);
        if (result == null || result.getId() == null) {
            throw new IllegalStateException("输出草稿未找到已保存的主结果: " + key);
        }
        return result;
    }

    private Date slotDate(Cd15AutoScheduleOutputDraft output, Cd15LaneAllocationDraft lane) {
        return output.getScheduleResults().stream()
                .filter(item -> lane.getResultKey().equals(item.getResultKey()))
                .flatMap(item -> item.getShiftSlots().stream())
                .filter(slot -> lane.getClassField().equals(slot.getClassField()))
                .map(Cd15ScheduleShiftSlotDraft::getScheduleDate).findFirst()
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
