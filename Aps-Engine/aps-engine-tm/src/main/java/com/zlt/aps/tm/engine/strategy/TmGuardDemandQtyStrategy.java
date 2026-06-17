package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmDemandQtyInput;
import com.zlt.aps.tm.engine.domain.TmDemandQtyResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面库存保证需求量策略。
 *
 * <p>按“当前班需求”和“库存最低保证班数缺口”取大值生成基础应排需求。
 * 本策略只使用 6 点库存滚动余额，不接入来源未确认的已计划入库量、已占用量、不良量和调整量。</p>
 */
public class TmGuardDemandQtyStrategy {

    /** 库存最低保证班数缺省值 */
    public static final int DEFAULT_GUARD_SHIFT_COUNT = 2;

    /**
     * 计算胎面基础应排需求。
     *
     * @param input 需求量计算输入
     * @return 需求量计算结果
     * @throws IllegalArgumentException 入参为空时抛出
     */
    public TmDemandQtyResult calculate(TmDemandQtyInput input) {
        if (input == null) {
            throw new IllegalArgumentException("需求量计算输入不能为空");
        }
        BigDecimal currentDemand = nvl(input.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(input.getGuardDemandQty());
        BigDecimal rollingStock = nvl(input.getRollingStockQty());
        int guardShiftCount = input.getGuardShiftCount() == null || input.getGuardShiftCount() <= 0
                ? DEFAULT_GUARD_SHIFT_COUNT : input.getGuardShiftCount();
        BigDecimal stockGap = guardDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal demandQty = currentDemand.max(stockGap);

        TmDemandQtyResult result = new TmDemandQtyResult();
        result.setCurrentShiftDemandQty(currentDemand);
        result.setGuardDemandQty(guardDemand);
        result.setRollingStockQty(rollingStock);
        result.setStockGapQty(stockGap);
        result.setDemandQty(demandQty);
        result.setGuardShiftCount(guardShiftCount);
        result.setSupplyHours(calculateSupplyHours(rollingStock, guardDemand, input.getGuardRangeHours()));
        result.setCalcDesc("按6点库存滚动余额计算，基础需求=max(当前班需求, 保证范围库存缺口)");
        return result;
    }

    /**
     * 计算供应时长。
     *
     * @param rollingStock    当前滚动库存
     * @param futureDemandQty 未来保证范围需求量
     * @param rangeHours      未来保证范围总小时数
     * @return 供应时长；需求或小时数为 0 时返回 null
     */
    public BigDecimal calculateSupplyHours(BigDecimal rollingStock, BigDecimal futureDemandQty, BigDecimal rangeHours) {
        BigDecimal demand = nvl(futureDemandQty);
        BigDecimal hours = nvl(rangeHours);
        if (demand.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal futureDemandPerHour = demand.divide(hours, 6, RoundingMode.HALF_UP);
        if (futureDemandPerHour.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return nvl(rollingStock).divide(futureDemandPerHour, 2, RoundingMode.HALF_UP);
    }

    /**
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
