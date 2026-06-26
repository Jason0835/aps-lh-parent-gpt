package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90DemandWindowResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成型需求窗口帘布需求量计算器。
 */
@Component
public class Cd90DemandWindowCalculator {

    private static final BigDecimal MILLIMETERS_PER_METER = new BigDecimal("1000");

    /**
     * 按AVERAGE或SUM口径计算帘布需求量。
     *
     * @param shifts 自然需求班次及额外班次
     * @param unitConsumeMillimeter 单耗，单位毫米/条
     * @param mode 计算方式：AVERAGE或SUM
     * @return 需求窗口结果
     */
    public Cd90DemandWindowResult calculate(List<Cd90DemandShift> shifts,
                                            BigDecimal unitConsumeMillimeter,
                                            String mode) {
        if (unitConsumeMillimeter == null || unitConsumeMillimeter.signum() < 0) {
            throw new IllegalArgumentException("帘布单耗不能小于0");
        }
        List<Cd90DemandShift> details = shifts == null
                ? Collections.emptyList() : new ArrayList<>(shifts);
        List<Cd90DemandShift> effective = details.stream()
                .filter(Cd90DemandShift::isIncluded)
                .filter(item -> value(item.getFormingQuantity()).signum() > 0)
                .collect(Collectors.toList());
        if (effective.isEmpty()) {
            return Cd90DemandWindowResult.builder()
                    .demandQuantity(BigDecimal.ZERO)
                    .effectiveShiftCount(0)
                    .shiftDetails(details)
                    .build();
        }

        BigDecimal formingQuantity = effective.stream()
                .map(item -> value(item.getFormingQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if ("AVERAGE".equals(mode)) {
            formingQuantity = formingQuantity.divide(
                    BigDecimal.valueOf(effective.size()), 10, RoundingMode.HALF_UP);
        } else if (!"SUM".equals(mode)) {
            throw new IllegalArgumentException("需求计算方式只能取AVERAGE或SUM");
        }
        BigDecimal demand = formingQuantity.multiply(unitConsumeMillimeter)
                .divide(MILLIMETERS_PER_METER, 10, RoundingMode.HALF_UP);
        return Cd90DemandWindowResult.builder()
                .demandQuantity(normalize(demand))
                .effectiveShiftCount(effective.size())
                .shiftDetails(details)
                .build();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }
}
