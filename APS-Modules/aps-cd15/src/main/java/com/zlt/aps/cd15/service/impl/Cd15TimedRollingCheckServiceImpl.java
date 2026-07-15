package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskType;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15RollingStabilityService;
import com.zlt.aps.cd15.service.Cd15TimedRollingAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15TimedRollingCheckService;
import com.zlt.aps.common.core.enums.ThreeShiftEnum;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** CD15定时自动滚动排程检查协调服务。 */
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingCheckServiceImpl implements Cd15TimedRollingCheckService {

    private static final String EARLY_MINUTES_PARAM_CODE = "SYS0601036";
    private static final String LATE_MINUTES_PARAM_CODE = "SYS0601037";
    private static final String STABLE_MINUTES_PARAM_CODE = "SYS0601038";
    private static final int DEFAULT_EARLY_MINUTES = 30;
    private static final int DEFAULT_LATE_MINUTES = 15;
    private static final int DEFAULT_STABLE_MINUTES = 5;
    private static final List<ShiftPoint> SHIFT_POINTS = Arrays.asList(
            new ShiftPoint(ThreeShiftEnum.MIDDLE, "CLASS1", -1),
            new ShiftPoint(ThreeShiftEnum.NIGHT, "CLASS2", -1),
            new ShiftPoint(ThreeShiftEnum.MORNING, "CLASS3", 0),
            new ShiftPoint(ThreeShiftEnum.MIDDLE, "CLASS4", 0),
            new ShiftPoint(ThreeShiftEnum.NIGHT, "CLASS5", 0),
            new ShiftPoint(ThreeShiftEnum.MORNING, "CLASS6", 1));

    private final Cd15ParamsMapper paramsMapper;
    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15AutoScheduleInputVersionService inputVersionService;
    private final Cd15RollingStabilityService rollingStabilityService;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15TimedRollingAsyncExecutor timedRollingAsyncExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public AjaxResult check(Cd15RollingCheckRequest request) {
        java.util.Date triggerDate = request == null || request.getTriggerTime() == null
                ? new java.util.Date() : request.getTriggerTime();
        LocalDateTime triggerTime = LocalDateTime.ofInstant(
                triggerDate.toInstant(), ZoneId.systemDefault());
        List<String> factoryCodes = this.resolveFactoryCodes(request, triggerTime.toLocalDate());
        List<Map<String, Object>> createdTasks = new ArrayList<>();
        List<Map<String, Object>> skippedFactories = new ArrayList<>();
        factoryCodes.forEach(factoryCode -> this.checkFactory(factoryCode, triggerTime,
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
        RollingParameters parameters = this.loadParameters(factoryCode);
        Optional<Cd15RollingTarget> optionalTarget = this.resolveTarget(factoryCode, triggerTime, parameters);
        if (!optionalTarget.isPresent()) {
            skippedFactories.add(this.skip(factoryCode, "OUTSIDE_WINDOW_OR_NO_BATCH"));
            return;
        }
        Cd15RollingTarget target = optionalTarget.get();
        java.util.Date scheduleDate = this.date(target.getScheduleDate());
        if (taskService.findActive(factoryCode, scheduleDate) != null) {
            skippedFactories.add(this.skip(factoryCode, "SCHEDULE_TASK_BUSY"));
            return;
        }
        String inputVersion = inputVersionService.fingerprint(factoryCode, target.getScheduleDate());
        String stateKey = factoryCode + ":" + target.getScheduleDate() + ":"
                + target.getTargetClassField() + ":" + target.getTargetShiftCode();
        boolean stable = rollingStabilityService.observe(stateKey, inputVersion,
                triggerTime.atZone(ZoneId.systemDefault()).toInstant(), parameters.getStableMinutes());
        if (!stable) {
            skippedFactories.add(this.skip(factoryCode, "INPUT_NOT_STABLE"));
            return;
        }
        String idempotencyKey = stateKey + ":" + inputVersion;
        if (taskService.findSuccessfulByIdempotencyKey(factoryCode,
                Cd15ScheduleTaskType.TIMED_ROLLING, idempotencyKey) != null) {
            skippedFactories.add(this.skip(factoryCode, "SAME_VERSION_NO_CHANGE"));
            return;
        }
        Cd15ScheduleTask task = taskService.createPending(factoryCode, scheduleDate,
                Cd15ScheduleTaskType.TIMED_ROLLING, "TIMER",
                this.requestSnapshot(target, inputVersion), inputVersion, "aps-job", idempotencyKey);
        if (!idempotencyKey.equals(task.getIdempotencyKey())) {
            skippedFactories.add(this.skip(factoryCode, "SCHEDULE_TASK_BUSY"));
            return;
        }
        timedRollingAsyncExecutor.execute(task.getTaskId(), target, inputVersion);
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("factoryCode", factoryCode);
        created.put("taskId", task.getTaskId());
        created.put("batchNo", target.getBatchNo());
        created.put("targetShiftCode", target.getTargetShiftCode());
        created.put("targetClassField", target.getTargetClassField());
        createdTasks.add(created);
    }

    private List<String> resolveFactoryCodes(Cd15RollingCheckRequest request, LocalDate triggerDate) {
        if (request != null && StringUtils.hasText(request.getFactoryCode())) {
            return Collections.singletonList(request.getFactoryCode().trim());
        }
        Set<String> factoryCodes = new LinkedHashSet<>();
        paramsMapper.selectList(new LambdaQueryWrapper<Cd15Params>()
                        .in(Cd15Params::getParamCode, Arrays.asList(EARLY_MINUTES_PARAM_CODE,
                                LATE_MINUTES_PARAM_CODE, STABLE_MINUTES_PARAM_CODE)))
                .stream()
                .map(Cd15Params::getFactoryCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .forEach(factoryCodes::add);
        resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .select(Cd15ScheduleResult::getFactoryCode)
                        .between(Cd15ScheduleResult::getScheduleDate,
                                Date.valueOf(triggerDate.minusDays(1)),
                                Date.valueOf(triggerDate.plusDays(1))))
                .stream()
                .map(Cd15ScheduleResult::getFactoryCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .forEach(factoryCodes::add);
        return factoryCodes.stream().sorted().collect(Collectors.toList());
    }

    private Optional<Cd15RollingTarget> resolveTarget(String factoryCode, LocalDateTime triggerTime,
                                                  RollingParameters parameters) {
        LocalDate triggerDate = triggerTime.toLocalDate();
        List<Cd15ScheduleResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleResult>()
                        .select(Cd15ScheduleResult::getScheduleDate,
                                Cd15ScheduleResult::getCd15BatchNo,
                                Cd15ScheduleResult::getCreateTime)
                        .eq(Cd15ScheduleResult::getFactoryCode, factoryCode)
                        .between(Cd15ScheduleResult::getScheduleDate,
                                Date.valueOf(triggerDate.minusDays(1)),
                                Date.valueOf(triggerDate.plusDays(1)))
                        .orderByDesc(Cd15ScheduleResult::getCreateTime));
        Map<String, Cd15ScheduleResult> batches = results.stream()
                .filter(item -> item.getScheduleDate() != null && StringUtils.hasText(item.getCd15BatchNo()))
                .collect(Collectors.toMap(this::batchKey, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));
        return batches.values().stream()
                .flatMap(batch -> this.targets(factoryCode, batch, parameters).stream())
                .filter(target -> !triggerTime.isBefore(target.getWindowStart())
                        && !triggerTime.isAfter(target.getWindowEnd()))
                .sorted(Comparator
                        .comparingLong((Cd15RollingTarget target) -> Math.abs(
                                ChronoUnit.DAYS.between(triggerDate, target.getScheduleDate())))
                        .thenComparing(Cd15RollingTarget::getScheduleDate, Comparator.reverseOrder()))
                .findFirst();
    }

    private List<Cd15RollingTarget> targets(String factoryCode, Cd15ScheduleResult batch,
                                        RollingParameters parameters) {
        LocalDate scheduleDate = this.localDate(batch.getScheduleDate());
        return SHIFT_POINTS.stream()
                .map(shift -> this.target(factoryCode, scheduleDate, batch.getCd15BatchNo(), shift, parameters))
                .collect(Collectors.toList());
    }

    private Cd15RollingTarget target(String factoryCode, LocalDate scheduleDate, String batchNo,
                                 ShiftPoint shift, RollingParameters parameters) {
        LocalDateTime handoverTime = LocalDateTime.of(
                scheduleDate.plusDays(shift.getStartDateOffset()), shift.getShift().getStartTime());
        return Cd15RollingTarget.builder()
                .factoryCode(factoryCode)
                .scheduleDate(scheduleDate)
                .batchNo(batchNo)
                .targetShiftCode(shift.getShift().getCode())
                .targetClassField(shift.getClassField())
                .targetClassIndex(this.classIndex(shift.getClassField()))
                .handoverTime(handoverTime)
                .windowStart(handoverTime.minusMinutes(parameters.getEarlyMinutes()))
                .windowEnd(handoverTime.plusMinutes(parameters.getLateMinutes()))
                .build();
    }

    private RollingParameters loadParameters(String factoryCode) {
        Map<String, Cd15Params> values = paramsMapper.selectList(new LambdaQueryWrapper<Cd15Params>()
                        .eq(Cd15Params::getFactoryCode, factoryCode)
                        .in(Cd15Params::getParamCode, Arrays.asList(EARLY_MINUTES_PARAM_CODE,
                                LATE_MINUTES_PARAM_CODE, STABLE_MINUTES_PARAM_CODE)))
                .stream()
                .collect(Collectors.toMap(Cd15Params::getParamCode, Function.identity(),
                        (first, second) -> first));
        return RollingParameters.builder()
                .earlyMinutes(this.nonNegative(values, EARLY_MINUTES_PARAM_CODE, DEFAULT_EARLY_MINUTES))
                .lateMinutes(this.nonNegative(values, LATE_MINUTES_PARAM_CODE, DEFAULT_LATE_MINUTES))
                .stableMinutes(this.nonNegative(values, STABLE_MINUTES_PARAM_CODE, DEFAULT_STABLE_MINUTES))
                .build();
    }

    private int nonNegative(Map<String, Cd15Params> values, String code, int defaultValue) {
        Cd15Params parameter = values.get(code);
        if (parameter == null || parameter.getParamValue() == null) {
            return defaultValue;
        }
        try {
            return Math.max(0, Integer.parseInt(parameter.getParamValue().trim()));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private int classIndex(String classField) {
        if (!StringUtils.hasText(classField)) {
            return 0;
        }
        try {
            return Integer.parseInt(classField.trim().toUpperCase().replace("CLASS", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String requestSnapshot(Cd15RollingTarget target, String inputVersion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("target", target);
        snapshot.put("inputVersion", inputVersion);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("CD15定时滚动排程请求快照序列化失败", exception);
        }
    }

    private Map<String, Object> skip(String factoryCode, String reasonCode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("factoryCode", factoryCode);
        item.put("reasonCode", reasonCode);
        return item;
    }

    private String batchKey(Cd15ScheduleResult item) {
        return item.getScheduleDate().getTime() + ":" + item.getCd15BatchNo();
    }

    private java.util.Date date(LocalDate value) {
        return java.util.Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate localDate(java.util.Date value) {
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static class ShiftPoint {
        private final ThreeShiftEnum shift;
        private final String classField;
        private final int startDateOffset;

        ShiftPoint(ThreeShiftEnum shift, String classField, int startDateOffset) {
            this.shift = shift;
            this.classField = classField;
            this.startDateOffset = startDateOffset;
        }

        ThreeShiftEnum getShift() {
            return shift;
        }

        String getClassField() {
            return classField;
        }

        int getStartDateOffset() {
            return startDateOffset;
        }
    }

    @Data
    @Builder
    private static class RollingParameters {
        private int earlyMinutes;
        private int lateMinutes;
        private int stableMinutes;
    }
}