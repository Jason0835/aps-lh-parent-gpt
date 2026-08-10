package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleShiftMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingParameters;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.service.Cd15RollingInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15RollingParameterService;
import com.zlt.aps.cd15.engine.service.Cd15RollingScheduleTaskService;
import com.zlt.aps.cd15.engine.service.Cd15RollingShiftStockService;
import com.zlt.aps.cd15.engine.service.Cd15RollingTargetResolver;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15StorageLaneLimitMapper;
import com.zlt.aps.cd15.service.Cd15RollingStabilityService;
import com.zlt.aps.cd15.service.Cd15TimedRollingAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15TimedRollingCheckService;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;

/** CD15定时滚动排程检查协调服务实现。 */
@Service
@Slf4j
@RequiredArgsConstructor
public class Cd15TimedRollingCheckServiceImpl implements Cd15TimedRollingCheckService {

    private final Cd15AutoScheduleShiftMapper shiftMapper;
    private final Cd15RollingParameterService rollingParameterService;
    private final Cd15RollingTargetResolver rollingTargetResolver;
    private final Cd15RollingInputVersionService rollingInputVersionService;
    private final Cd15RollingStabilityService rollingStabilityService;
    private final Cd15RollingScheduleTaskService rollingTaskService;
    private final Cd15RollingShiftStockService rollingShiftStockService;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15TimedRollingAsyncExecutor timedRollingAsyncExecutor;
    private final IMesItfService mesItfService;
    private final Cd15StorageLaneLimitMapper storageLaneLimitMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AjaxResult check(Cd15RollingCheckRequest request) {
        Date triggerDate = request == null || request.getTriggerTime() == null
                ? new Date() : request.getTriggerTime();
        LocalDateTime triggerTime = LocalDateTime.ofInstant(
                triggerDate.toInstant(), ZoneId.systemDefault());
        List<String> factoryCodes = resolveFactoryCodes(request);
        List<Map<String, Object>> createdTasks = new ArrayList<>();
        List<Map<String, Object>> skippedFactories = new ArrayList<>();

        factoryCodes.forEach(factoryCode -> checkFactory(factoryCode, triggerTime,
                createdTasks, skippedFactories));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedCount", factoryCodes.size());
        data.put("createdCount", createdTasks.size());
        data.put("skippedCount", skippedFactories.size());
        data.put("tasks", createdTasks);
        data.put("skipped", skippedFactories);
        return AjaxResult.success(data);
    }

    private void checkFactory(String factoryCode, LocalDateTime triggerTime,
                              List<Map<String, Object>> createdTasks,
                              List<Map<String, Object>> skippedFactories) {
        Cd15RollingParameters parameters = rollingParameterService.load(factoryCode);
        Optional<Cd15RollingTarget> optionalTarget = rollingTargetResolver.resolve(
                factoryCode, triggerTime, parameters);
        if (!optionalTarget.isPresent()) {
            skippedFactories.add(skip(factoryCode, "OUTSIDE_WINDOW_OR_NO_BATCH"));
            return;
        }
        Cd15RollingTarget target = optionalTarget.get();
        if (!this.syncTargetShiftStock(target)) {
            skippedFactories.add(skip(factoryCode, "SHIFT_STOCK_SYNC_FAILED"));
            return;
        }
        if (!this.syncTargetStorageLane(target)) {
            skippedFactories.add(skip(factoryCode, "STORAGE_LANE_SYNC_FAILED"));
            return;
        }
        if (!rollingShiftStockService.exists(target)) {
            skippedFactories.add(skip(factoryCode, "SHIFT_STOCK_NOT_READY"));
            return;
        }
        if (!this.isTargetStorageLaneReady(target)) {
            skippedFactories.add(skip(factoryCode, "STORAGE_LANE_NOT_READY"));
            return;
        }
        Date scheduleDate = Date.from(target.getScheduleDate().atStartOfDay(
                ZoneId.systemDefault()).toInstant());
        if (taskService.findActive(factoryCode, scheduleDate) != null) {
            skippedFactories.add(skip(factoryCode, "SCHEDULE_TASK_BUSY"));
            return;
        }
        String inputVersion = rollingInputVersionService.fingerprint(target);
        String stateKey = factoryCode + ":" + target.getScheduleDate()
                + ":" + target.getTargetShiftCode();
        boolean stable = rollingStabilityService.observe(stateKey, inputVersion,
                triggerTime.atZone(ZoneId.systemDefault()).toInstant(),
                parameters.getStableMinutes());
        if (!stable) {
            skippedFactories.add(skip(factoryCode, "INPUT_NOT_STABLE"));
            return;
        }
        String idempotencyKey = stateKey + ":" + inputVersion;
        if (rollingTaskService.findSuccessfulByIdempotencyKey(
                factoryCode, idempotencyKey) != null) {
            skippedFactories.add(skip(factoryCode, "SAME_VERSION_NO_CHANGE"));
            return;
        }
        Cd15ScheduleTask task = rollingTaskService.createPending(factoryCode, scheduleDate,
                requestSnapshot(target, inputVersion), idempotencyKey);
        if (!idempotencyKey.equals(task.getIdempotencyKey())) {
            skippedFactories.add(skip(factoryCode, "SCHEDULE_TASK_BUSY"));
            return;
        }
        timedRollingAsyncExecutor.execute(task.getTaskId(), target, inputVersion);
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("factoryCode", factoryCode);
        created.put("taskId", task.getTaskId());
        created.put("batchNo", target.getBatchNo());
        created.put("targetShiftCode", target.getTargetShiftCode());
        createdTasks.add(created);
    }

    /** 在读取库存和计算输入指纹前同步目标交班班次库存。 */
    private boolean syncTargetShiftStock(Cd15RollingTarget target) {
        ZoneId zoneId = ZoneId.systemDefault();
        MesShiftStockSyncRequest syncRequest = new MesShiftStockSyncRequest();
        syncRequest.setFactoryCode(target.getFactoryCode());
        syncRequest.setCompanyCode(target.getFactoryCode());
        syncRequest.setStockDate(Date.from(target.getHandoverTime().toLocalDate()
                .atStartOfDay(zoneId).toInstant()));
        syncRequest.setShiftCode(target.getTargetShiftCode());
        syncRequest.setShiftStartTime(Date.from(target.getHandoverTime().atZone(zoneId).toInstant()));
        try {
            AjaxResult result = FeignTokenHelper.callWithToken(
                    () -> this.mesItfService.syncCd15ShiftStock(syncRequest));
            return result != null
                    && Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, result.get(AjaxResult.CODE_TAG));
        } catch (Exception exception) {
            log.error("斜裁定时滚动前班次库存同步失败，factoryCode={}，shiftCode={}，shiftStartTime={}",
                    target.getFactoryCode(), target.getTargetShiftCode(),
                    target.getHandoverTime(), exception);
            return false;
        }
    }

    /** 在输入指纹计算前同步目标排程日期和班次的库排快照。 */
    private boolean syncTargetStorageLane(Cd15RollingTarget target) {
        AuxReqSyncDataLogs syncRequest = new AuxReqSyncDataLogs();
        syncRequest.setFactoryCode(target.getFactoryCode());
        HashMap<String, Object> queryParams = new HashMap<>();
        queryParams.put("laneDate", target.getHandoverTime().toLocalDate().toString());
        queryParams.put("shiftCode", target.getTargetShiftCode());
        syncRequest.setQueryParams(queryParams);
        try {
            AjaxResult result = FeignTokenHelper.callWithToken(
                    () -> this.mesItfService.syncCd15StorageLaneLimit(syncRequest));
            return result != null
                    && Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, result.get(AjaxResult.CODE_TAG));
        } catch (Exception exception) {
            log.error("斜裁定时滚动前库排同步失败，factoryCode={}，laneDate={}，shiftCode={}",
                    target.getFactoryCode(), target.getHandoverTime().toLocalDate(),
                    target.getTargetShiftCode(), exception);
            return false;
        }
    }

    /** 检查目标日期和班次至少存在一条有效库排。 */
    private boolean isTargetStorageLaneReady(Cd15RollingTarget target) {
        Long count = this.storageLaneLimitMapper.selectCount(
                new LambdaQueryWrapper<Cd15StorageLaneLimit>()
                        .eq(Cd15StorageLaneLimit::getFactoryCode, target.getFactoryCode())
                        .eq(Cd15StorageLaneLimit::getLaneDate,
                                java.sql.Date.valueOf(target.getHandoverTime().toLocalDate()))
                        .eq(Cd15StorageLaneLimit::getShiftCode, target.getTargetShiftCode()));
        return count != null && count > 0;
    }

    private List<String> resolveFactoryCodes(Cd15RollingCheckRequest request) {
        if (request != null && !isBlank(request.getFactoryCode())) {
            return Collections.singletonList(request.getFactoryCode().trim());
        }
        return shiftMapper.selectList(new LambdaQueryWrapper<Cd15ShiftConfig>()
                        .eq(Cd15ShiftConfig::getIsActive, 1))
                .stream()
                .map(Cd15ShiftConfig::getFactoryCode)
                .filter(factoryCode -> !isBlank(factoryCode))
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String requestSnapshot(Cd15RollingTarget target, String inputVersion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("target", target);
        snapshot.put("inputVersion", inputVersion);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("定时滚动排程请求快照序列化失败", exception);
        }
    }

    private Map<String, Object> skip(String factoryCode, String reasonCode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("factoryCode", factoryCode);
        item.put("reasonCode", reasonCode);
        return item;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
