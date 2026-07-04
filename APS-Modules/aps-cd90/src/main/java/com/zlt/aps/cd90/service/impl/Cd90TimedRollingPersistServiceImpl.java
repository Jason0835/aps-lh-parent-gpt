package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleRollingAdjustLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskStatus;
import com.zlt.aps.cd90.engine.constant.Cd90ScheduleTaskType;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90RollingAdjustmentDraft;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90TimedRollingOutput;
import com.zlt.aps.cd90.engine.service.Cd90RollingInputVersionService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleLaneAllocationMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleRollingAdjustLogMapper;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.service.Cd90TimedRollingPersistService;
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

/** CD90定时滚动排程最终短事务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90TimedRollingPersistServiceImpl implements Cd90TimedRollingPersistService {

    private static final String SNAPSHOT_SCHEMA_VERSION = "1";

    private final Cd90ScheduleResultMapper resultMapper;
    private final Cd90ScheduleLaneAllocationMapper laneMapper;
    private final Cd90UnscheduleResultMapper unscheduleMapper;
    private final Cd90ScheduleRollingAdjustLogMapper adjustLogMapper;
    private final Cd90ScheduleTaskService taskService;
    private final Cd90RollingInputVersionService versionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persist(String taskId, Cd90RollingTarget target,
                        Cd90TimedRollingOutput output, RLock lock) {
        validateCommitState(taskId, target, output, lock);
        String currentVersion = versionService.fingerprint(target);
        if (!Objects.equals(output.getInputVersion(), currentVersion)) {
            throw new IllegalStateException("定时滚动期间输入数据已变化");
        }

        List<Cd90ScheduleLaneAllocation> replacementLanes =
                safe(output.getReplacementLaneAllocations());
        applyStorageLaneCodes(output, replacementLanes);
        safe(output.getInsertedResults()).forEach(this::insertResult);
        safe(output.getUpdatedResults()).forEach(this::updateResult);
        safe(output.getLogicallyDeletedResults()).stream()
                .map(Cd90ScheduleResult::getId).filter(Objects::nonNull)
                .forEach(resultMapper::deleteById);
        replaceLaneAllocations(output, replacementLanes);
        replaceUnscheduled(target, output);
        saveAdjustments(taskId, target, output);
        if (!taskService.markSuccessInCurrentTransaction(taskId, target.getBatchNo())) {
            throw new IllegalStateException("定时滚动任务状态已变化，不能提交结果");
        }
        log.info("[直裁定时滚动] 最终事务提交完成, taskId={}, batchNo={}, inserted={}, updated={}, deleted={}, unscheduled={}, adjustments={}",
                taskId, target.getBatchNo(), safe(output.getInsertedResults()).size(),
                safe(output.getUpdatedResults()).size(),
                safe(output.getLogicallyDeletedResults()).size(),
                safe(output.getUnscheduledResults()).size(), safe(output.getAdjustments()).size());
    }

    private void validateCommitState(String taskId, Cd90RollingTarget target,
                                     Cd90TimedRollingOutput output, RLock lock) {
        Cd90ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || !Cd90ScheduleTaskType.ROLLING_SCHEDULE.equals(task.getTaskType())
                || !Cd90ScheduleTaskStatus.RUNNING.equals(task.getTaskStatus())) {
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

    private void insertResult(Cd90ScheduleResult result) {
        if (resultMapper.insert(result) != 1 || result.getId() == null) {
            throw new IllegalStateException("新增定时滚动排程结果失败");
        }
    }

    private void updateResult(Cd90ScheduleResult result) {
        if (result.getId() == null) {
            throw new IllegalStateException("定时滚动更新结果缺少主键");
        }
        if (result.getPublishSuccessCount() != null && result.getPublishSuccessCount() > 0) {
            result.setIsRelease("5");
            result.setRemark("ROLLING_DEGRADE");
        }
        if (resultMapper.updateById(result) != 1) {
            throw new IllegalStateException("更新定时滚动排程结果失败");
        }
    }

    private void applyStorageLaneCodes(Cd90TimedRollingOutput output,
                                       List<Cd90ScheduleLaneAllocation> lanes) {
        Map<String, String> laneCodesByOrder = lanes.stream()
                .filter(item -> item.getOrderNo() != null)
                .filter(item -> item.getStorageLaneCode() != null)
                .collect(Collectors.groupingBy(Cd90ScheduleLaneAllocation::getOrderNo,
                        LinkedHashMap::new, Collectors.mapping(
                                Cd90ScheduleLaneAllocation::getStorageLaneCode,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        values -> String.join(",", values)))));
        safe(output.getInsertedResults()).forEach(result ->
                result.setStorageLaneCode(laneCodesByOrder.get(result.getOrderNo())));
        safe(output.getUpdatedResults()).forEach(result ->
                result.setStorageLaneCode(laneCodesByOrder.get(result.getOrderNo())));
    }

    private void replaceLaneAllocations(Cd90TimedRollingOutput output,
                                        List<Cd90ScheduleLaneAllocation> lanes) {
        Set<Long> affectedIds = new LinkedHashSet<>();
        safe(output.getUpdatedResults()).stream().map(Cd90ScheduleResult::getId)
                .filter(Objects::nonNull).forEach(affectedIds::add);
        safe(output.getLogicallyDeletedResults()).stream().map(Cd90ScheduleResult::getId)
                .filter(Objects::nonNull).forEach(affectedIds::add);
        if (!affectedIds.isEmpty()) {
            laneMapper.delete(new LambdaQueryWrapper<Cd90ScheduleLaneAllocation>()
                    .in(Cd90ScheduleLaneAllocation::getScheduleResultId, affectedIds));
        }
        List<Cd90ScheduleResult> parents = new ArrayList<>();
        parents.addAll(safe(output.getInsertedResults()));
        parents.addAll(safe(output.getUpdatedResults()));
        Map<String, Cd90ScheduleResult> parentByOrder = parents.stream()
                .filter(item -> item.getOrderNo() != null)
                .collect(Collectors.toMap(Cd90ScheduleResult::getOrderNo,
                        Function.identity(), (left, right) -> left));
        lanes.stream()
                .filter(lane -> parentByOrder.containsKey(lane.getOrderNo()))
                .forEach(lane -> {
                    Cd90ScheduleResult parent = parentByOrder.get(lane.getOrderNo());
                    if (parent.getId() == null) {
                        throw new IllegalStateException("滚动库排主结果缺少ID: " + lane.getOrderNo());
                    }
                    lane.setScheduleResultId(parent.getId());
                    if (laneMapper.insert(lane) != 1) {
                        throw new IllegalStateException("保存定时滚动库排分配失败");
                    }
                });
    }

    private void replaceUnscheduled(Cd90RollingTarget target,
                                    Cd90TimedRollingOutput output) {
        Set<String> clothCodes = new LinkedHashSet<>();
        safe(output.getAdjustments()).stream().map(Cd90RollingAdjustmentDraft::getClothCode)
                .filter(Objects::nonNull).forEach(clothCodes::add);
        safe(output.getUnscheduledResults()).stream().map(Cd90UnscheduleResult::getClothCode)
                .filter(Objects::nonNull).forEach(clothCodes::add);
        if (!clothCodes.isEmpty()) {
            unscheduleMapper.delete(new LambdaQueryWrapper<Cd90UnscheduleResult>()
                    .eq(Cd90UnscheduleResult::getFactoryCode, target.getFactoryCode())
                    .eq(Cd90UnscheduleResult::getScheduleDate, date(target))
                    .eq(Cd90UnscheduleResult::getBatchNo, target.getBatchNo())
                    .in(Cd90UnscheduleResult::getClothCode, clothCodes));
        }
        safe(output.getUnscheduledResults()).forEach(result -> {
            if (unscheduleMapper.insert(result) != 1) {
                throw new IllegalStateException("保存定时滚动未排结果失败");
            }
        });
    }

    private void saveAdjustments(String taskId, Cd90RollingTarget target,
                                 Cd90TimedRollingOutput output) {
        safe(output.getAdjustments()).forEach(draft -> {
        Map<String, Long> insertedIdByOrder = safe(output.getInsertedResults()).stream()
                .filter(item -> item.getOrderNo() != null && item.getId() != null)
                .collect(Collectors.toMap(Cd90ScheduleResult::getOrderNo,
                        Cd90ScheduleResult::getId, (left, right) -> left));
            Cd90ScheduleRollingAdjustLog entity = new Cd90ScheduleRollingAdjustLog();
            entity.setFactoryCode(target.getFactoryCode());
            entity.setTaskId(taskId);
            entity.setBatchNo(target.getBatchNo());
            entity.setScheduleDate(date(target));
            entity.setTargetShiftCode(target.getTargetShiftCode());
            entity.setRollingItemKey(draft.getRollingItemKey());
            entity.setScheduleResultId(draft.getScheduleResultId() == null
                    ? insertedIdByOrder.get(draft.getRollingItemKey()) : draft.getScheduleResultId());
            entity.setClothCode(draft.getClothCode());
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
                    draft.getBeforeSnapshot(), output, draft.getClothCode()));
            entity.setAfterSnapshotJson(snapshotJson(
                    draft.getAfterSnapshot(), output, draft.getClothCode()));
            if (adjustLogMapper.insert(entity) != 1) {
                throw new IllegalStateException("保存定时滚动调整日志失败");
            }
        });
    }

    private String snapshotJson(Object source, Cd90TimedRollingOutput output,
                                String clothCode) {
        Map<String, Object> snapshot = source instanceof Map
                ? new LinkedHashMap<>((Map<String, Object>) source)
                : new LinkedHashMap<>();
        snapshot.putIfAbsent("scheduleResult", source);
        snapshot.putIfAbsent("laneAllocations", Collections.emptyList());
        snapshot.put("unscheduledResults", safe(output.getUnscheduledResults()).stream()
                .filter(item -> Objects.equals(clothCode, item.getClothCode()))
                .collect(Collectors.toList()));
        snapshot.putIfAbsent("publishState", null);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("定时滚动双快照序列化失败", exception);
        }
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

    private Date date(Cd90RollingTarget target) {
        return Date.from(target.getScheduleDate().atStartOfDay(
                ZoneId.systemDefault()).toInstant());
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
