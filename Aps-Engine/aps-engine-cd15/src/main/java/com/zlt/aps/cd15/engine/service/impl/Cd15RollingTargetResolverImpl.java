package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftWindowResolver;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleShiftMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15RollingParameters;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.service.Cd15RollingTargetResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** 根据启用班次和当前有效批次解析交班滚动目标。 */
@Service
@RequiredArgsConstructor
public class Cd15RollingTargetResolverImpl implements Cd15RollingTargetResolver {

    private final Cd15AutoScheduleShiftMapper shiftMapper;
    private final Cd15EngineScheduleResultMapper resultMapper;
    private final Cd15ShiftWindowResolver shiftWindowResolver;

    /** 解析触发时刻所在滚动窗口；窗口外或无有效批次时返回空。 */
    @Override
    public Optional<Cd15RollingTarget> resolve(String factoryCode,
                                               LocalDateTime triggerTime,
                                               Cd15RollingParameters parameters) {
        if (!StringUtils.hasText(factoryCode) || triggerTime == null || parameters == null) {
            return Optional.empty();
        }
        List<Cd15ShiftConfig> shifts = shiftMapper.selectList(
                new LambdaQueryWrapper<Cd15ShiftConfig>()
                        .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd15ShiftConfig::getIsActive, 1));
        if (shifts.isEmpty()) {
            return Optional.empty();
        }
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
                .filter(item -> item.getScheduleDate() != null
                        && StringUtils.hasText(item.getCd15BatchNo()))
                .collect(Collectors.toMap(this::batchKey, item -> item,
                        (first, second) -> first, LinkedHashMap::new));
        return batches.values().stream()
                .flatMap(batch -> this.targets(factoryCode, batch, shifts, parameters).stream())
                .filter(target -> !triggerTime.isBefore(target.getWindowStart())
                        && !triggerTime.isAfter(target.getWindowEnd()))
                .sorted(Comparator.comparing(Cd15RollingTarget::getScheduleDate,
                        Comparator.reverseOrder()))
                .findFirst();
    }

    /** 把一个有效批次展开为全部物理班次滚动窗口。 */
    private List<Cd15RollingTarget> targets(String factoryCode,
                                            Cd15ScheduleResult batch,
                                            List<Cd15ShiftConfig> shifts,
                                            Cd15RollingParameters parameters) {
        LocalDate scheduleDate = this.localDate(batch.getScheduleDate());
        return shiftWindowResolver.resolve(scheduleDate, shifts).stream()
                .map(shift -> this.target(factoryCode, scheduleDate, batch.getCd15BatchNo(),
                        shift, parameters))
                .collect(Collectors.toList());
    }

    /** 构造单个目标班次窗口。 */
    private Cd15RollingTarget target(String factoryCode,
                                     LocalDate scheduleDate,
                                     String batchNo,
                                     Cd15ShiftDescriptor shift,
                                     Cd15RollingParameters parameters) {
        LocalDateTime handoverTime = shift.getStartTime();
        LocalDateTime windowStart = handoverTime.minusMinutes(parameters.getEarlyMinutes());
        LocalDateTime windowEnd = handoverTime.plusMinutes(parameters.getLateMinutes());
        return Cd15RollingTarget.builder()
                .factoryCode(factoryCode)
                .scheduleDate(scheduleDate)
                .batchNo(batchNo)
                .targetShiftCode(shift.getShiftCode())
                .targetClassField(shift.getClassField())
                .resourceBaselineDate(shift.getScheduleDate())
                .handoverTime(handoverTime)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .build();
    }

    /** 同一排程日期和批次只保留最新一条结果作为批次代表。 */
    private String batchKey(Cd15ScheduleResult item) {
        return item.getScheduleDate().getTime() + ":" + item.getCd15BatchNo();
    }

    /** 将数据库日期统一转换为系统时区LocalDate。 */
    private LocalDate localDate(java.util.Date value) {
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
