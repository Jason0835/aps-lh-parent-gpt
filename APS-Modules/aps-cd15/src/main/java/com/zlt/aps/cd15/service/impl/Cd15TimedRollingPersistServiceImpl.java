package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15UnscheduleResultMapper;
import com.zlt.aps.cd15.model.Cd15ScheduleOverwriteDecision;
import com.zlt.aps.cd15.service.Cd15AutoScheduleDraftMapper;
import com.zlt.aps.cd15.service.Cd15ScheduleOverwriteValidator;
import com.zlt.aps.cd15.service.Cd15TimedRollingPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** CD15定时滚动排程最终短事务持久化。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingPersistServiceImpl implements Cd15TimedRollingPersistService {

    private static final int CLASS_COUNT = 8;
    private static final String ROLLING_LOG_TYPE = "TIMED_ROLLING";

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;
    private final Cd15UnscheduleResultMapper unscheduleMapper;
    private final Cd15ScheduleResultLogMapper logMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleInputVersionService inputVersionService;
    private final Cd15AutoScheduleDraftMapper draftMapper;
    private final Cd15ScheduleOverwriteValidator overwriteValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(String taskId, Cd15RollingTarget target, Cd15TimedRollingOutput output, RLock lock) {
        this.validateCommitState(taskId, target, output, lock);
        String currentVersion = inputVersionService.fingerprint(target.getFactoryCode(), target.getScheduleDate());
        if (!Objects.equals(output.getInputVersion(), currentVersion)) {
            throw new IllegalStateException("CD15定时滚动期间输入数据已变化");
        }
        this.replaceSuffixResults(taskId, target, output);
        this.replaceUnscheduled(target, output);
        if (!taskService.markSuccessInCurrentTransaction(taskId, target.getBatchNo())) {
            throw new IllegalStateException("CD15定时滚动任务状态已变化，不能提交结果");
        }
        log.info("[斜裁定时滚动] 最终事务提交完成 taskId={}, batchNo={}, targetClassIndex={}, replacement={}, unscheduled={}",
                taskId, target.getBatchNo(), target.getTargetClassIndex(),
                this.safeReplacement(output).size(), this.safeUnscheduled(output).size());
    }

    private boolean supportedRollingTaskType(String taskType) {
        return Cd15ScheduleTaskType.TIMED_ROLLING.equals(taskType)
                || Cd15ScheduleTaskType.INSERT_ORDER.equals(taskType)
                || Cd15ScheduleTaskType.TRANSFER_MACHINE.equals(taskType)
                || Cd15ScheduleTaskType.CHANGE_QTY.equals(taskType);
    }
    private void validateCommitState(String taskId, Cd15RollingTarget target,
                                     Cd15TimedRollingOutput output, RLock lock) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !this.supportedRollingTaskType(task.getTaskType())
                || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("CD15定时滚动任务不是执行中状态");
        }
        if (target == null || target.getTargetClassIndex() <= 0) {
            throw new IllegalStateException("CD15定时滚动目标班次无效");
        }
        if (output == null) {
            throw new IllegalStateException("CD15定时滚动试排输出为空");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("CD15定时滚动执行锁已失效");
        }
    }

    private void replaceSuffixResults(String taskId, Cd15RollingTarget target, Cd15TimedRollingOutput output) {
        List<Cd15ScheduleResult> oldResults = resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, target.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, this.date(target.getScheduleDate()))
                .eq(Cd15ScheduleResult::getCd15BatchNo, target.getBatchNo()));
        List<Cd15ScheduleResult> suffixResults = oldResults.stream()
                .filter(item -> this.resultClassIndex(item) >= target.getTargetClassIndex())
                .collect(Collectors.toList());
        Cd15ScheduleOverwriteDecision overwriteDecision = overwriteValidator.validate(suffixResults, true);
        if (overwriteDecision.isRejected()) {
            throw new IllegalStateException(overwriteDecision.getMessage());
        }
        this.deleteSuffixResults(suffixResults);
        this.safeReplacement(output).forEach(draft -> this.saveResult(taskId, target, draft));
    }

    private void deleteSuffixResults(List<Cd15ScheduleResult> suffixResults) {
        List<Long> ids = suffixResults.stream()
                .map(Cd15ScheduleResult::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        laneMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleLaneAllocation>()
                .in(Cd15ScheduleLaneAllocation::getScheduleResultId, ids)
                .set(Cd15ScheduleLaneAllocation::getIsDelete, 1));
        logMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleResultLog>()
                .in(Cd15ScheduleResultLog::getScheduleResultId, ids)
                .set(Cd15ScheduleResultLog::getIsDelete, 1));
        ids.forEach(resultMapper::deleteById);
    }

    private void saveResult(String taskId, Cd15RollingTarget target,
                            Cd15ScheduleResultDraft draft) {
        Cd15ScheduleResult entity = draftMapper.toScheduleResult(target.getFactoryCode(),
                target.getScheduleDate(), target.getBatchNo(), draft);
        if (resultMapper.insert(entity) != 1) {
            throw new IllegalStateException("保存CD15定时滚动排程结果失败");
        }
        this.replaceLaneAllocations(target, entity, draft);
        Cd15ScheduleResultLog createLog = draftMapper.toCreateLog(taskId, target.getBatchNo(), entity, draft);
        createLog.setLogType(ROLLING_LOG_TYPE);
        createLog.setReasonCode(ROLLING_LOG_TYPE);
        createLog.setChangeReason("CD15定时滚动生成目标班次后缀结果");
        if (logMapper.insert(createLog) != 1) {
            throw new IllegalStateException("保存CD15定时滚动操作日志失败");
        }
    }

    private void replaceLaneAllocations(Cd15RollingTarget target, Cd15ScheduleResult entity,
                                        Cd15ScheduleResultDraft draft) {
        List<Cd15LaneAllocationDraft> laneAllocations = draft.getLaneAllocations() == null
                ? Collections.emptyList() : draft.getLaneAllocations();
        AtomicInteger allocationOrder = new AtomicInteger(1);
        laneAllocations.forEach(laneDraft -> {
            Cd15ScheduleLaneAllocation lane = draftMapper.toLaneAllocation(
                    target.getBatchNo(), entity, draft, laneDraft, allocationOrder.getAndIncrement());
            if (laneMapper.insert(lane) != 1) {
                throw new IllegalStateException("保存CD15定时滚动库排分配明细失败");
            }
        });
    }

    private void replaceUnscheduled(Cd15RollingTarget target, Cd15TimedRollingOutput output) {
        List<String> classFields = this.suffixClassFields(target.getTargetClassIndex());
        if (!classFields.isEmpty()) {
            unscheduleMapper.update(null, new LambdaUpdateWrapper<Cd15UnscheduleResult>()
                    .eq(Cd15UnscheduleResult::getFactoryCode, target.getFactoryCode())
                    .eq(Cd15UnscheduleResult::getScheduleDate, this.date(target.getScheduleDate()))
                    .eq(Cd15UnscheduleResult::getBatchNo, target.getBatchNo())
                    .in(Cd15UnscheduleResult::getClassField, classFields)
                    .set(Cd15UnscheduleResult::getIsDelete, 1));
        }
        AtomicInteger reasonOrder = new AtomicInteger(1);
        this.safeUnscheduled(output).stream()
                .sorted(Comparator.comparingInt(this::reasonPriority)
                        .thenComparing(source -> this.safeText(source.getSteelStripCode()))
                        .thenComparing(source -> this.safeText(source.getBigRollCode()))
                        .thenComparing(source -> this.safeText(source.getCuttingAngle()))
                        .thenComparing(source -> this.safeText(source.getClassField())))
                .forEach(source -> {
                    Cd15UnscheduleResult entity = draftMapper.toUnscheduleResult(target.getFactoryCode(),
                            target.getScheduleDate(), target.getBatchNo(), source, reasonOrder.getAndIncrement());
                    if (unscheduleMapper.insert(entity) != 1) {
                        throw new IllegalStateException("保存CD15定时滚动未排结果失败");
                    }
                });
    }

    private List<String> suffixClassFields(int targetClassIndex) {
        return IntStream.rangeClosed(targetClassIndex, CLASS_COUNT)
                .boxed()
                .flatMap(index -> Stream.of("class" + index, "CLASS" + index))
                .collect(Collectors.toList());
    }

    private int resultClassIndex(Cd15ScheduleResult result) {
        return IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(index -> this.positive(result, index))
                .findFirst()
                .orElse(0);
    }

    private boolean positive(Cd15ScheduleResult result, int classIndex) {
        Serializable value = result.getFieldValueByFieldName(String.format("class%dPlanQty", classIndex));
        if (value instanceof Number) {
            return ((Number) value).doubleValue() > 0D;
        }
        Serializable scheduleDate = result.getFieldValueByFieldName(String.format("class%dScheduleDate", classIndex));
        return scheduleDate != null;
    }

    private int reasonPriority(Cd15SingleShiftScheduleResult source) {
        String reasonCode = source == null ? null : source.getUnscheduledReasonCode();
        if ("DATA_MISSING".equals(reasonCode) || "ANGLE_WIDTH_CONFIG_MISSING".equals(reasonCode)) {
            return 10;
        }
        if ("NO_BIG_ROLL_STOCK".equals(reasonCode) || "AGING_PERIOD_LIMIT".equals(reasonCode)) {
            return 20;
        }
        if ("STORAGE_LANE_LIMIT".equals(reasonCode)) {
            return 30;
        }
        if ("WIDTH_MISMATCH".equals(reasonCode) || "NO_AVAILABLE_MACHINE".equals(reasonCode)) {
            return 40;
        }
        return 90;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<Cd15ScheduleResultDraft> safeReplacement(Cd15TimedRollingOutput output) {
        return output == null || output.getReplacementResults() == null
                ? Collections.emptyList() : output.getReplacementResults();
    }

    private List<Cd15SingleShiftScheduleResult> safeUnscheduled(Cd15TimedRollingOutput output) {
        return output == null || output.getUnscheduledResults() == null
                ? Collections.emptyList() : output.getUnscheduledResults();
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}