package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90StockGuaranteeResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 逐班库存保证能力计算器。
 */
@Component
public class Cd90StockGuaranteeCalculator {

    /**
     * 按有效需求班次顺序计算库存保证班数和供应时长。
     *
     * @param expectedAvailableStock 预计可用库存
     * @param demandShifts 有效需求班次明细
     * @return 库存保证结果
     */
    public Cd90StockGuaranteeResult calculate(BigDecimal expectedAvailableStock,
                                              List<Cd90DemandShift> demandShifts) {
        if (expectedAvailableStock == null || expectedAvailableStock.signum() < 0) {
            throw new IllegalArgumentException("预计可用库存不能小于0");
        }
        BigDecimal remaining = expectedAvailableStock;
        BigDecimal guaranteedShifts = BigDecimal.ZERO;
        BigDecimal supplyHours = BigDecimal.ZERO;

        for (Cd90DemandShift shift : demandShifts == null ? Collections.<Cd90DemandShift>emptyList() : demandShifts) {
            if (!shift.isIncluded()) {
                continue;
            }
            BigDecimal demand = value(shift.getClothDemandQuantity());
            BigDecimal hours = value(shift.getShiftHours());
            if (demand.signum() <= 0) {
                continue;
            }
            if (remaining.compareTo(demand) >= 0) {
                remaining = remaining.subtract(demand);
                guaranteedShifts = guaranteedShifts.add(BigDecimal.ONE);
                supplyHours = supplyHours.add(hours);
                continue;
            }
            BigDecimal ratio = remaining.divide(demand, 10, RoundingMode.HALF_UP);
            guaranteedShifts = guaranteedShifts.add(ratio);
            supplyHours = supplyHours.add(hours.multiply(ratio));
            remaining = BigDecimal.ZERO;
            break;
        }
        return Cd90StockGuaranteeResult.builder()
                .guaranteedShifts(normalize(guaranteedShifts))
                .supplyHours(normalize(supplyHours))
                .remainingStock(remaining)
                .build();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }
}
