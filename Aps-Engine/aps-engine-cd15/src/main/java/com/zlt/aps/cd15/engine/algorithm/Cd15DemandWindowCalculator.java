package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15DemandWindowResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成型需求窗口钢带需求量计算器。
 */
@Component
public class Cd15DemandWindowCalculator {

    private static final BigDecimal MILLIMETERS_PER_METER = new BigDecimal("1000");

    /**
     * 按AVERAGE或SUM口径计算钢带需求量。
     *
     * @param shifts 自然需求班次及额外班次
     * @param unitConsumeMillimeter 单耗，单位毫米/条
     * @param mode 计算方式：AVERAGE或SUM
     * @return 需求窗口结果
     */
    public Cd15DemandWindowResult calculate(List<Cd15DemandShift> shifts,
                                            BigDecimal unitConsumeMillimeter,
                                            String mode) {
        if (unitConsumeMillimeter == null || unitConsumeMillimeter.signum() < 0) {
            throw new IllegalArgumentException("钢带单耗不能小于0");
        }
        List<Cd15DemandShift> details = shifts == null
                ? Collections.emptyList() : new ArrayList<>(shifts);
        List<Cd15DemandShift> effective = details.stream()
                .filter(Cd15DemandShift::isIncluded)
                .filter(item -> value(item.getFormingQuantity()).signum() > 0)
                .collect(Collectors.toList());
        if (effective.isEmpty()) {
            return Cd15DemandWindowResult.builder()
                    .demandQuantity(BigDecimal.ZERO)
                    .effectiveShiftCount(0)
                    .shiftDetails(details)
                    .build();
        }

        BigDecimal formingQuantity = effective.stream()
                .map(item -> value(item.getFormingQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if ("AVERAGE".equals(mode)) {
            BigDecimal totalWeight = effective.stream()
                    .map(this::windowWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            formingQuantity = formingQuantity.divide(
                    totalWeight, 10, RoundingMode.HALF_UP);
        } else if (!"SUM".equals(mode)) {
            throw new IllegalArgumentException("需求计算方式只能取AVERAGE或SUM");
        }
        BigDecimal demand = formingQuantity.multiply(unitConsumeMillimeter)
                .divide(MILLIMETERS_PER_METER, 10, RoundingMode.HALF_UP);
        return Cd15DemandWindowResult.builder()
                .demandQuantity(normalize(demand))
                .effectiveShiftCount(effective.size())
                .shiftDetails(details)
                .build();
    }

    private BigDecimal windowWeight(Cd15DemandShift shift) {
        return shift.getWindowWeight() == null ? BigDecimal.ONE : shift.getWindowWeight();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }
}
