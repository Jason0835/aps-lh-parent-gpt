package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleRollingAdjustLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStatus;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15RollingAdjustmentDraft;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15RollingInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleRollingAdjustLogMapper;
import com.zlt.aps.cd15.mapper.Cd15UnscheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15TimedRollingPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** CD15定时滚动排程最终短事务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingPersistServiceImpl implements Cd15TimedRollingPersistService {

    private static final String SNAPSHOT_SCHEMA_VERSION = "1";

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;
    private final Cd15UnscheduleResultMapper unscheduleMapper;
    private final Cd15ScheduleRollingAdjustLogMapper adjustLogMapper;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15RollingInputVersionService versionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(String taskId, Cd15RollingTarget target,
                        Cd15TimedRollingOutput output, RLock lock) {
        validateCommitState(taskId, target, output, lock);
        String currentVersion = versionService.fingerprint(target);
        if (!Objects.equals(output.getInputVersion(), currentVersion)) {
            throw new IllegalStateException("定时滚动期间输入数据已变化");
        }

        List<Cd15ScheduleLaneAllocation> replacementLanes =
                safe(output.getReplacementLaneAllocations());
        applyStorageLaneCodes(output, replacementLanes);
        safe(output.getInsertedResults()).forEach(this::insertResult);
        safe(output.getUpdatedResults()).forEach(this::updateResult);
        safe(output.getLogicallyDeletedResults()).stream()
                .map(Cd15ScheduleResult::getId).filter(Objects::nonNull)
                .forEach(resultMapper::deleteById);
        replaceLaneAllocations(output, replacementLanes);
        replaceUnscheduled(target, output);
        saveAdjustments(taskId, target, output);
        if (!taskService.markSuccessInCurrentTransaction(taskId, target.getBatchNo())) {
            throw new IllegalStateException("定时滚动任务状态已变化，不能提交结果");
        }
        log.info("[斜裁定时滚动] 最终事务提交完成, taskId={}, batchNo={}, inserted={}, updated={}, deleted={}, unscheduled={}, adjustments={}",
                taskId, target.getBatchNo(), safe(output.getInsertedResults()).size(),
                safe(output.getUpdatedResults()).size(),
                safe(output.getLogicallyDeletedResults()).size(),
                safe(output.getUnscheduledResults()).size(), safe(output.getAdjustments()).size());
    }

    private void validateCommitState(String taskId, Cd15RollingTarget target,
                                     Cd15TimedRollingOutput output, RLock lock) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd15ScheduleTaskType.ROLLING_SCHEDULE.equals(task.getTaskType())
                || !Cd15ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
            throw new IllegalStateException("定时滚动任务不是执行中状态");
        }
        if (lock == null || !lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("定时滚动执行锁已失效");
        }
        if (target == null || output == null
                || !Objects.equals(target.getBatchNo(), output.getBatchNo())) {
            throw new IllegalStateException("定时滚动目标批次与输出批次不一致");
        }
    }

    private void insertResult(Cd15ScheduleResult result) {
        if (resultMapper.insert(result) != 1 || result.getId() == null) {
            throw new IllegalStateException("新增定时滚动排程结果失败");
        }
    }

    private void updateResult(Cd15ScheduleResult result) {
        if (result.getId() == null) {
            throw new IllegalStateException("定时滚动更新结果缺少主键");
        }
        if (result.getReleaseStatus() != null && !"0".equals(result.getReleaseStatus())) {
            result.setReleaseStatus("5");
            result.setRemark("ROLLING_DEGRADE");
        }
        if (resultMapper.updateById(result) != 1) {
            throw new IllegalStateException("更新定时滚动排程结果失败");
        }
    }

    private void applyStorageLaneCodes(Cd15TimedRollingOutput output,
                                       List<Cd15ScheduleLaneAllocation> lanes) {
        Map<String, String> laneCodesByResult = lanes.stream()
                .filter(item -> item.getOrderNo() != null)
                .filter(item -> item.getStorageLaneCode() != null)
                .collect(Collectors.groupingBy(
                        item -> this.resultKey(
                                item.getOrderNo(), item.getSteelStripCode()),
                        LinkedHashMap::new, Collectors.mapping(
                                Cd15ScheduleLaneAllocation::getStorageLaneCode,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        values -> String.join(",", values)))));
        safe(output.getInsertedResults()).forEach(result ->
                result.setStorageLaneCode(laneCodesByResult.get(this.resultKey(
                        result.getOrderNo(), result.getSteelStripCode()))));
        safe(output.getUpdatedResults()).forEach(result ->
                result.setStorageLaneCode(laneCodesByResult.get(this.resultKey(
                        result.getOrderNo(), result.getSteelStripCode()))));
    }

    private void replaceLaneAllocations(Cd15TimedRollingOutput output,
                                        List<Cd15ScheduleLaneAllocation> lanes) {
        Set<Long> affectedIds = new LinkedHashSet<>();
        safe(output.getUpdatedResults()).stream().map(Cd15ScheduleResult::getId)
                .filter(Objects::nonNull).forEach(affectedIds::add);
        safe(output.getLogicallyDeletedResults()).stream().map(Cd15ScheduleResult::getId)
                .filter(Objects::nonNull).forEach(affectedIds::add);
        if (!affectedIds.isEmpty()) {
            laneMapper.delete(new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                    .in(Cd15ScheduleLaneAllocation::getScheduleResultId, affectedIds));
        }
        List<Cd15ScheduleResult> parents = new ArrayList<>();
        parents.addAll(safe(output.getInsertedResults()));
        parents.addAll(safe(output.getUpdatedResults()));
        Map<String, Cd15ScheduleResult> parentByResult = parents.stream()
                .filter(item -> item.getOrderNo() != null)
                .collect(Collectors.toMap(
                        item -> this.resultKey(
                                item.getOrderNo(), item.getSteelStripCode()),
                        Function.identity(), (left, right) -> left));
        lanes.stream()
                .filter(lane -> parentByResult.containsKey(this.resultKey(
                        lane.getOrderNo(), lane.getSteelStripCode())))
                .forEach(lane -> {
                    Cd15ScheduleResult parent = parentByResult.get(this.resultKey(
                            lane.getOrderNo(), lane.getSteelStripCode()));
                    if (parent.getId() == null) {
                        throw new IllegalStateException("滚动库排主结果缺少ID: " + lane.getOrderNo());
                    }
                    lane.setScheduleResultId(parent.getId());
                    if (laneMapper.insert(lane) != 1) {
                        throw new IllegalStateException("保存定时滚动库排分配失败");
                    }
                });
    }

    private void replaceUnscheduled(Cd15RollingTarget target,
                                    Cd15TimedRollingOutput output) {
        Set<String> steelStripCodes = new LinkedHashSet<>();
        safe(output.getAdjustments()).stream().map(Cd15RollingAdjustmentDraft::getSteelStripCode)
                .filter(Objects::nonNull).forEach(steelStripCodes::add);
        safe(output.getUnscheduledResults()).stream().map(Cd15UnscheduleResult::getSteelStripCode)
                .filter(Objects::nonNull).forEach(steelStripCodes::add);
        if (!steelStripCodes.isEmpty()) {
            unscheduleMapper.delete(new LambdaQueryWrapper<Cd15UnscheduleResult>()
                    .eq(Cd15UnscheduleResult::getFactoryCode, target.getFactoryCode())
                    .eq(Cd15UnscheduleResult::getScheduleDate, date(target))
                    .eq(Cd15UnscheduleResult::getBatchNo, target.getBatchNo())
                    .in(Cd15UnscheduleResult::getSteelStripCode, steelStripCodes));
        }
        safe(output.getUnscheduledResults()).forEach(result -> {
            if (unscheduleMapper.insert(result) != 1) {
                throw new IllegalStateException("保存定时滚动未排结果失败");
            }
        });
    }

    private void saveAdjustments(String taskId, Cd15RollingTarget target,
                                 Cd15TimedRollingOutput output) {
        Map<String, Long> insertedIdByResult = safe(output.getInsertedResults()).stream()
                .filter(item -> item.getOrderNo() != null && item.getId() != null)
                .collect(Collectors.toMap(
                        item -> this.resultKey(
                                item.getOrderNo(), item.getSteelStripCode()),
                        Cd15ScheduleResult::getId, (left, right) -> left));
        safe(output.getAdjustments()).forEach(draft -> {
            Cd15ScheduleRollingAdjustLog entity = new Cd15ScheduleRollingAdjustLog();
            entity.setFactoryCode(target.getFactoryCode());
            entity.setTaskId(taskId);
            entity.setBatchNo(target.getBatchNo());
            entity.setScheduleDate(date(target));
            entity.setTargetShiftCode(target.getTargetShiftCode());
            entity.setRollingItemKey(draft.getRollingItemKey());
            entity.setScheduleResultId(draft.getScheduleResultId() == null
                    ? insertedIdByResult.get(this.resultKey(
                            draft.getRollingItemKey(), draft.getSteelStripCode()))
                    : draft.getScheduleResultId());
            entity.setSteelStripCode(draft.getSteelStripCode());
            entity.setBigRollCode(draft.getBigRollCode());
            entity.setAdjustType(draft.getAdjustType());
            entity.setOldClassIndex(classIndex(draft.getBeforeClassField()));
            entity.setNewClassIndex(classIndex(draft.getAfterClassField()));
            entity.setOldProduceOrder(draft.getBeforeProduceOrder());
            entity.setNewProduceOrder(draft.getAfterProduceOrder());
            entity.setOldPlanQty(draft.getBeforeQuantity());
            entity.setNewPlanQty(draft.getAfterQuantity());
            entity.setOldMachineCode(draft.getBeforeMachineCode());
            entity.setNewMachineCode(draft.getAfterMachineCode());
            entity.setReasonCode(draft.getReasonCode());
            entity.setReasonDetail(draft.getReasonDetail());
            entity.setInputVersion(output.getInputVersion());
            entity.setSnapshotSchemaVersion(SNAPSHOT_SCHEMA_VERSION);
            entity.setBeforeSnapshotJson(snapshotJson(
                    draft.getBeforeSnapshot(), output, draft.getSteelStripCode()));
            entity.setAfterSnapshotJson(snapshotJson(
                    draft.getAfterSnapshot(), output, draft.getSteelStripCode()));
            if (adjustLogMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存定时滚动调整日志失败");
            }
        });
    }

    private String snapshotJson(Object source, Cd15TimedRollingOutput output,
                                String steelStripCode) {
        Map<String, Object> snapshot = source instanceof Map
                ? new LinkedHashMap<>((Map<String, Object>) source)
                : new LinkedHashMap<>();
        snapshot.putIfAbsent("scheduleResult", source);
        snapshot.putIfAbsent("laneAllocations", Collections.emptyList());
        snapshot.put("unscheduledResults", safe(output.getUnscheduledResults()).stream()
                .filter(item -> Objects.equals(steelStripCode, item.getSteelStripCode()))
                .collect(Collectors.toList()));
        snapshot.putIfAbsent("publishState", null);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("定时滚动双快照序列化失败", exception);
        }
    }

    private String resultKey(String orderNo, String steelStripCode) {
        return String.valueOf(orderNo) + "|" + String.valueOf(steelStripCode);
    }

    private Integer classIndex(String classField) {
        if (classField == null || !classField.startsWith("CLASS")) {
            return null;
        }
        try {
            return Integer.valueOf(classField.substring(5));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Date date(Cd15RollingTarget target) {
        return Date.from(target.getScheduleDate().atStartOfDay(
                ZoneId.systemDefault()).toInstant());
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
