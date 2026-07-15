package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15TimedRollingPrefixResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** CD15定时滚动前缀已排资源占用加载实现。 */
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingPrefixResourceServiceImpl implements Cd15TimedRollingPrefixResourceService {

    private static final int CLASS_COUNT = 8;

    private final Cd15ScheduleResultMapper resultMapper;
    private final Cd15ScheduleLaneAllocationMapper laneMapper;

    @Override
    public List<Cd15RollingPrefixResourceUsage> loadPrefixResourceUsages(Cd15RollingTarget target) {
        if (target == null || target.getTargetClassIndex() <= 1) {
            return Collections.emptyList();
        }
        List<Cd15ScheduleResult> prefixResults = resultMapper.selectList(new LambdaQueryWrapper<Cd15ScheduleResult>()
                .eq(Cd15ScheduleResult::getFactoryCode, target.getFactoryCode())
                .eq(Cd15ScheduleResult::getScheduleDate, this.date(target.getScheduleDate()))
                .eq(Cd15ScheduleResult::getCd15BatchNo, target.getBatchNo()));
        if (prefixResults == null || prefixResults.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Cd15ScheduleLaneAllocation>> allocationsByResultId = this.loadAllocationsByResultId(prefixResults);
        return prefixResults.stream()
                .filter(Objects::nonNull)
                .flatMap(result -> this.toUsages(result, allocationsByResultId.get(result.getId()), target.getTargetClassIndex()))
                .collect(Collectors.toList());
    }

    private Map<Long, List<Cd15ScheduleLaneAllocation>> loadAllocationsByResultId(List<Cd15ScheduleResult> results) {
        List<Long> resultIds = results.stream()
                .filter(Objects::nonNull)
                .map(Cd15ScheduleResult::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (resultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Cd15ScheduleLaneAllocation> allocations = laneMapper.selectList(
                new LambdaQueryWrapper<Cd15ScheduleLaneAllocation>()
                        .in(Cd15ScheduleLaneAllocation::getScheduleResultId, resultIds));
        return allocations == null ? Collections.emptyMap() : allocations.stream()
                .filter(Objects::nonNull)
                .filter(allocation -> allocation.getScheduleResultId() != null)
                .collect(Collectors.groupingBy(Cd15ScheduleLaneAllocation::getScheduleResultId));
    }

    private Stream<Cd15RollingPrefixResourceUsage> toUsages(Cd15ScheduleResult result,
                                                             List<Cd15ScheduleLaneAllocation> allocations,
                                                             int targetClassIndex) {
        return IntStream.range(1, Math.min(targetClassIndex, CLASS_COUNT + 1))
                .mapToObj(classIndex -> this.toClassUsages(result, allocations, classIndex))
                .flatMap(List::stream);
    }

    private List<Cd15RollingPrefixResourceUsage> toClassUsages(Cd15ScheduleResult result,
                                                                List<Cd15ScheduleLaneAllocation> allocations,
                                                                int classIndex) {
        BigDecimal planQty = this.readBigDecimal(result, String.format("class%dPlanQty", classIndex));
        BigDecimal cxPlanQty = this.readBigDecimal(result, String.format("class%dCxPlanQty", classIndex));
        if (planQty.signum() <= 0 && cxPlanQty.signum() <= 0) {
            return Collections.emptyList();
        }
        String classField = "CLASS" + classIndex;
        List<Cd15ScheduleLaneAllocation> matchedAllocations = allocations == null ? Collections.emptyList() : allocations.stream()
                .filter(Objects::nonNull)
                .filter(allocation -> classField.equalsIgnoreCase(allocation.getClassField()))
                .collect(Collectors.toList());
        if (matchedAllocations.isEmpty()) {
            return Collections.singletonList(this.toUsage(result, classField, classIndex, result.getStorageLaneCode(),
                    planQty, null));
        }
        return matchedAllocations.stream()
                .map(allocation -> this.toUsage(result, classField, classIndex, allocation.getStorageLaneCode(),
                        this.positive(allocation.getAllocatedQty()) ? allocation.getAllocatedQty() : planQty,
                        allocation.getAllocatedCartCount()))
                .collect(Collectors.toList());
    }

    private Cd15RollingPrefixResourceUsage toUsage(Cd15ScheduleResult result, String classField, int classIndex,
                                                   String storageLaneCode, BigDecimal consumeMeters,
                                                   Integer allocatedCartCount) {
        BigDecimal safeConsumeMeters = this.value(consumeMeters);
        return Cd15RollingPrefixResourceUsage.builder()
                .scheduleResultId(result.getId())
                .classField(classField)
                .classIndex(classIndex)
                .steelStripCode(this.trim(result.getSteelStripCode()))
                .bigRollCode(this.trim(result.getBigRollCode()))
                .storageLaneCode(this.trim(storageLaneCode))
                .steelStripConsumeMeters(safeConsumeMeters)
                .bigRollConsumeMeters(safeConsumeMeters)
                .allocatedCartCount(allocatedCartCount)
                .build();
    }

    private BigDecimal readBigDecimal(Cd15ScheduleResult result, String fieldName) {
        Serializable value = result.getFieldValueByFieldName(fieldName);
        return value instanceof Number ? BigDecimal.valueOf(((Number) value).doubleValue()) : BigDecimal.ZERO;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}