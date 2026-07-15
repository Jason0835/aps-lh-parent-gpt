package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15UnscheduleResultMapper;
import com.zlt.aps.cd15.model.Cd15ScheduleOverwriteDecision;
import com.zlt.aps.cd15.service.Cd15AutoScheduleDraftMapper;
import com.zlt.aps.cd15.service.Cd15AutoSchedulePersistService;
import com.zlt.aps.cd15.service.Cd15ScheduleOverwriteValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** CD15自动排程最终短事务落库实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoSchedulePersistServiceImpl implements Cd15AutoSchedulePersistService {

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15UnscheduleResultMapper unscheduleMapper;
    private final Cd15ScheduleResultLogMapper logMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15AutoScheduleDraftMapper draftMapper;
    private final Cd15ScheduleOverwriteValidator overwriteValidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String persist(String taskId, String factoryCode, LocalDate scheduleDate,
                          Cd15MultiShiftScheduleResult output, RLock lock) {
        this.validateCommitState(taskId, lock);
        Date scheduleDateValue = this.date(scheduleDate);
        List<Cd15ScheduleResult> oldResults = resultMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .eq(Cd15ScheduleResult::getFactoryCode, factoryCode)
                        .eq(Cd15ScheduleResult::getScheduleDate, scheduleDateValue));
        Cd15ScheduleOverwriteDecision overwriteDecision = overwriteValidator.validate(oldResults, true);
        if (overwriteDecision.isRejected()) {
            throw new IllegalStateException(overwriteDecision.getMessage());
        }
        String batchNo = this.nextBatchNo(scheduleDate);
        this.saveResults(taskId, factoryCode, scheduleDate, batchNo, output);
        this.saveUnscheduled(factoryCode, scheduleDate, batchNo, output);
        this.invalidateOldResults(oldResults, batchNo);
        if (!taskService.markSuccessInCurrentTransaction(taskId, batchNo)) {
            throw new IllegalStateException("CD15自动排程任务状态已变化，不能提交结果");
        }
        log.info("[斜裁自动排程] 最终落库完成, taskId={}, batchNo={}, resultCount={}, unscheduledCount={}",
                taskId, batchNo, this.safeScheduled(output).size(), this.safeUnscheduled(output).size());
        return batchNo;
    }

    private void validateCommitState(String taskId, RLock lock) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("CD15自动排程任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("CD15自动排程执行锁已失效");
        }
    }

    private void saveResults(String taskId, String factoryCode, LocalDate scheduleDate,
                             String batchNo, Cd15MultiShiftScheduleResult output) {
        this.safeScheduled(output).forEach(draft -> {
            Cd15ScheduleResult entity = draftMapper.toScheduleResult(factoryCode, scheduleDate, batchNo, draft);
            if (resultMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存CD15排程结果失败");
            }
            this.saveLanes(batchNo, entity, draft);
            Cd15ScheduleResultLog log = draftMapper.toCreateLog(taskId, batchNo, entity, draft);
            if (logMapper.insert(log) != 1) {
                throw new IllegalStateException("保存CD15排程结果日志失败");
            }
        });
    }


    private void saveLanes(String batchNo, Cd15ScheduleResult entity, Cd15ScheduleResultDraft draft) {
        List<Cd15LaneAllocationDraft> laneAllocations = draft.getLaneAllocations() == null
                ? java.util.Collections.emptyList() : draft.getLaneAllocations();
        AtomicInteger allocationOrder = new AtomicInteger(1);
        laneAllocations.forEach(laneDraft -> {
            Cd15ScheduleLaneAllocation lane = draftMapper.toLaneAllocation(
                    batchNo, entity, draft, laneDraft, allocationOrder.getAndIncrement());
            if (laneMapper.insert(lane) != 1) {
                throw new IllegalStateException("保存CD15库排分配明细失败");
            }
        });
    }
    private void saveUnscheduled(String factoryCode, LocalDate scheduleDate,
                                 String batchNo, Cd15MultiShiftScheduleResult output) {
        AtomicInteger reasonOrder = new AtomicInteger(1);
        this.safeUnscheduled(output).stream()
                .sorted(Comparator.comparingInt(this::reasonPriority)
                        .thenComparing(source -> this.safeText(source.getSteelStripCode()))
                        .thenComparing(source -> this.safeText(source.getBigRollCode()))
                        .thenComparing(source -> this.safeText(source.getCuttingAngle()))
                        .thenComparing(source -> this.safeText(source.getClassField())))
                .forEach(source -> {
            Cd15UnscheduleResult entity = draftMapper.toUnscheduleResult(
                    factoryCode, scheduleDate, batchNo, source, reasonOrder.getAndIncrement());
            if (unscheduleMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存CD15未排结果失败");
            }
        });
    }

    private void invalidateOldResults(List<Cd15ScheduleResult> oldResults, String batchNo) {
        List<Long> ids = oldResults.stream()
                .filter(item -> item.getId() != null)
                .map(Cd15ScheduleResult::getId)
                .collect(Collectors.toList());
        List<String> oldBatchNos = oldResults.stream()
                .map(Cd15ScheduleResult::getCd15BatchNo)
                .filter(value -> value != null && !batchNo.equals(value))
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            resultMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleResult>()
                    .in(Cd15ScheduleResult::getId, ids)
                    .ne(Cd15ScheduleResult::getCd15BatchNo, batchNo)
                    .set(Cd15ScheduleResult::getIsDelete, 1));
            laneMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleLaneAllocation>()
                    .in(Cd15ScheduleLaneAllocation::getScheduleResultId, ids)
                    .set(Cd15ScheduleLaneAllocation::getIsDelete, 1));
            logMapper.update(null, new LambdaUpdateWrapper<Cd15ScheduleResultLog>()
                    .in(Cd15ScheduleResultLog::getScheduleResultId, ids)
                    .set(Cd15ScheduleResultLog::getIsDelete, 1));
        }
        if (!oldBatchNos.isEmpty()) {
            unscheduleMapper.update(null, new LambdaUpdateWrapper<Cd15UnscheduleResult>()
                    .in(Cd15UnscheduleResult::getBatchNo, oldBatchNos)
                    .set(Cd15UnscheduleResult::getIsDelete, 1));
        }
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
    private List<Cd15ScheduleResultDraft> safeScheduled(Cd15MultiShiftScheduleResult output) {
        return output == null || output.getScheduledDrafts() == null
                ? java.util.Collections.emptyList() : output.getScheduledDrafts();
    }

    private List<com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult> safeUnscheduled(
            Cd15MultiShiftScheduleResult output) {
        return output == null || output.getUnscheduledResults() == null
                ? java.util.Collections.emptyList() : output.getUnscheduledResults();
    }

    private String nextBatchNo(LocalDate scheduleDate) {
        String datePart = scheduleDate == null ? "UNKNOWN" : scheduleDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        return "CD15" + datePart + DateTimeFormatter.ofPattern("HHmmssSSS")
                .format(java.time.LocalDateTime.now());
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}