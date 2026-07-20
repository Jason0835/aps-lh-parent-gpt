package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 按逐钢带备库深度截取成型需求窗口，并处理半班等小数深度。
 */
@Component
public class Cd15FractionalDemandWindowSelector {

    /**
     * 截取需求窗口。小数部分只按比例计入窗口末班需求，不改变自然班次边界。
     *
     * @param availableShifts 从当前供应起点开始的自然成型班次
     * @param depthClassQty 当前钢带匹配到的备库班数
     * @return 截取并按窗口权重换算后的班次副本
     */
    public List<Cd15DemandShift> select(List<Cd15DemandShift> availableShifts,
                                        BigDecimal depthClassQty) {
        if (depthClassQty == null || depthClassQty.signum() <= 0) {
            throw new IllegalArgumentException("钢带备库班数必须大于0");
        }
        List<Cd15DemandShift> shifts = availableShifts == null
                ? Collections.emptyList() : availableShifts;
        int requiredShiftCount;
        try {
            requiredShiftCount = depthClassQty.setScale(0, RoundingMode.CEILING).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("钢带备库班数超出支持范围", exception);
        }
        int fullShiftCount = depthClassQty.setScale(0, RoundingMode.FLOOR).intValue();
        BigDecimal fraction = depthClassQty.subtract(BigDecimal.valueOf(fullShiftCount));
        int selectedCount = Math.min(requiredShiftCount, shifts.size());

        return IntStream.range(0, selectedCount)
                .mapToObj(index -> this.copyWithWeight(shifts.get(index),
                        index == fullShiftCount && fraction.signum() > 0
                                ? fraction : BigDecimal.ONE))
                .collect(Collectors.toList());
    }

    /** 按窗口权重复制班次，避免修改输入快照。 */
    private Cd15DemandShift copyWithWeight(Cd15DemandShift source, BigDecimal weight) {
        if (source == null) {
            throw new IllegalArgumentException("成型需求班次不能为空");
        }
        return Cd15DemandShift.builder()
                .steelStripCode(source.getSteelStripCode())
                .materialKey(source.getMaterialKey())
                .bigRollCode(source.getBigRollCode())
                .cuttingAngle(source.getCuttingAngle())
                .craftWidth(source.getCraftWidth())
                .unitConsumeMillimeter(source.getUnitConsumeMillimeter())
                .cordWidth(source.getCordWidth())
                .curlLength(source.getCurlLength())
                .classField(source.getClassField())
                .shiftKey(source.getShiftKey())
                .startTime(source.getStartTime())
                .formingQuantity(this.normalize(this.value(source.getFormingQuantity()).multiply(weight)))
                .steelStripDemandQuantity(this.normalize(this.value(source.getSteelStripDemandQuantity()).multiply(weight)))
                .shiftHours(source.getShiftHours())
                .windowWeight(weight)
                .included(source.isIncluded())
                .stopped(source.isStopped())
                .build();
    }

    private BigDecimal normalize(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
