package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15StockGuaranteeResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 逐班库存保证能力计算器。
 */
@Component
public class Cd15StockGuaranteeCalculator {

    /**
     * 按有效需求班次顺序计算库存保证班数和供应时长。
     *
     * <p>模拟"库存逐个班次消耗"过程：库存足够满足当前班需求时，扣除后继续；
     * 库存不足以满足完整班需求时，按比例切割最后一个班，返回部分保证。</p>
     *
     * @param expectedAvailableStock 预计可用库存（扣除窗口前消耗后），必须 ≥ 0
     * @param demandShifts 有效需求班次明细，按自然班次时间升序传入
     * @return 库存保证结果，包含保证班数、供应时长和剩余库存
     */
    public Cd15StockGuaranteeResult calculate(BigDecimal expectedAvailableStock,
                                              List<Cd15DemandShift> demandShifts) {
        if (expectedAvailableStock == null || expectedAvailableStock.signum() < 0) {
            throw new IllegalArgumentException("预计可用库存不能小于0");
        }
        // 从可用库存开始，逐班扣减需求，直到库存耗尽或所有班次处理完毕。
        BigDecimal remaining = expectedAvailableStock;
        BigDecimal guaranteedShifts = BigDecimal.ZERO;
        BigDecimal supplyHours = BigDecimal.ZERO;

        for (Cd15DemandShift shift : demandShifts == null ? Collections.<Cd15DemandShift>emptyList() : demandShifts) {
            // 跳过不参与计算的班次（如停产班或已跳过班）。
            if (!shift.isIncluded()) {
                continue;
            }
            BigDecimal demand = value(shift.getSteelStripDemandQuantity());
            BigDecimal hours = value(shift.getShiftHours());
            BigDecimal weight = shift.getWindowWeight() == null
                    ? BigDecimal.ONE : shift.getWindowWeight();
            // 需求为0的班次不消耗库存，跳过计数。
            if (demand.signum() <= 0) {
                continue;
            }
            if (remaining.compareTo(demand) >= 0) {
                // 库存足够覆盖当前窗口份额：按窗口权重累计保证班数和供应时长。
                remaining = remaining.subtract(demand);
                guaranteedShifts = guaranteedShifts.add(weight);
                supplyHours = supplyHours.add(hours.multiply(weight));
                continue;
            }
            // 库存不足以覆盖整个班需求：按剩余库存占该班需求的比例，计算部分保证。
            // 例如剩余100米，该班需求400米，则算0.25班、2小时（8h×0.25）。
            BigDecimal ratio = remaining.divide(demand, 10, RoundingMode.HALF_UP);
            guaranteedShifts = guaranteedShifts.add(weight.multiply(ratio));
            supplyHours = supplyHours.add(hours.multiply(weight).multiply(ratio));
            remaining = BigDecimal.ZERO;
            break;
        }
        return Cd15StockGuaranteeResult.builder()
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
