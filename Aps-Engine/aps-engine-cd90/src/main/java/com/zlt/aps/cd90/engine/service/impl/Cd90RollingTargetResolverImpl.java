package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.algorithm.Cd90ShiftWindowResolver;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleResultMapper;
import com.zlt.aps.cd90.engine.model.Cd90RollingParameters;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.service.Cd90RollingTargetResolver;
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
public class Cd90RollingTargetResolverImpl implements Cd90RollingTargetResolver {

    private final Cd90AutoScheduleShiftMapper shiftMapper;
    private final Cd90EngineScheduleResultMapper resultMapper;
    private final Cd90ShiftWindowResolver shiftWindowResolver;

    /** 解析触发时刻所在滚动窗口；窗口外或无有效批次时返回空。 */
    @Override
    public Optional<Cd90RollingTarget> resolve(String factoryCode,
                                               LocalDateTime triggerTime,
                                               Cd90RollingParameters parameters) {
        if (!StringUtils.hasText(factoryCode) || triggerTime == null || parameters == null) {
            return Optional.empty();
        }
        List<Cd90ShiftConfig> shifts = shiftMapper.selectList(
                new LambdaQueryWrapper<Cd90ShiftConfig>()
                        .eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd90ShiftConfig::getIsActive, 1));
        if (shifts.isEmpty()) {
            return Optional.empty();
        }
        LocalDate triggerDate = triggerTime.toLocalDate();
        List<Cd90ScheduleResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .select(Cd90ScheduleResult::getScheduleDate,
                                Cd90ScheduleResult::getBatchNo,
                                Cd90ScheduleResult::getCreateTime)
                        .eq(Cd90ScheduleResult::getFactoryCode, factoryCode)
                        .between(Cd90ScheduleResult::getScheduleDate,
                                Date.valueOf(triggerDate.minusDays(1)),
                                Date.valueOf(triggerDate.plusDays(1)))
                        .orderByDesc(Cd90ScheduleResult::getCreateTime));
        Map<String, Cd90ScheduleResult> batches = results.stream()
                .filter(item -> item.getScheduleDate() != null
                        && StringUtils.hasText(item.getBatchNo()))
                .collect(Collectors.toMap(this::batchKey, item -> item,
                        (first, second) -> first, LinkedHashMap::new));
        return batches.values().stream()
                .flatMap(batch -> this.targets(factoryCode, batch, shifts, parameters).stream())
                .filter(target -> !triggerTime.isBefore(target.getWindowStart())
                        && !triggerTime.isAfter(target.getWindowEnd()))
                .sorted(Comparator.comparing(Cd90RollingTarget::getScheduleDate,
                        Comparator.reverseOrder()))
                .findFirst();
    }

    /** 把一个有效批次展开为全部物理班次滚动窗口。 */
    private List<Cd90RollingTarget> targets(String factoryCode,
                                            Cd90ScheduleResult batch,
                                            List<Cd90ShiftConfig> shifts,
                                            Cd90RollingParameters parameters) {
        LocalDate scheduleDate = this.localDate(batch.getScheduleDate());
        return shiftWindowResolver.resolve(scheduleDate, shifts).stream()
                .map(shift -> this.target(factoryCode, scheduleDate, batch.getBatchNo(),
                        shift, parameters))
                .collect(Collectors.toList());
    }

    /** 构造单个目标班次窗口。 */
    private Cd90RollingTarget target(String factoryCode,
                                     LocalDate scheduleDate,
                                     String batchNo,
                                     Cd90ShiftDescriptor shift,
                                     Cd90RollingParameters parameters) {
        LocalDateTime handoverTime = shift.getStartTime();
        LocalDateTime windowStart = handoverTime.minusMinutes(parameters.getEarlyMinutes());
        LocalDateTime windowEnd = handoverTime.plusMinutes(parameters.getLateMinutes());
        return Cd90RollingTarget.builder()
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
    private String batchKey(Cd90ScheduleResult item) {
        return item.getScheduleDate().getTime() + ":" + item.getBatchNo();
    }

    /** 将数据库日期统一转换为系统时区LocalDate。 */
    private LocalDate localDate(java.util.Date value) {
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
