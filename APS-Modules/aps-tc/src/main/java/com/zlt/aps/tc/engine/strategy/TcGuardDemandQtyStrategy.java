package com.zlt.aps.tc.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcDemandAlgorithmEnum;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.domain.TcDemandQtyInput;
import com.zlt.aps.tc.engine.domain.TcDemandQtyResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎侧库存保证需求量策略。
 *
 * <p>按“当前班库存缺口”和“库存最低保证班数缺口”取大值生成基础应排需求。
 * 本策略只使用 当前班初滚动库存，不接入来源未确认的已计划入库量、已占用量、不良量和调整量。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TcStrategyRegistry} 按算法编码 "1" 收集。</p>
 */
@Component
public class TcGuardDemandQtyStrategy implements ITcDemandQtyStrategy {

    /**
     * 获取算法编码。
     *
     * @return 算法编码
     */
    @Override
    public String getAlgorithmCode() {
        return TcDemandAlgorithmEnum.GUARD.getCode();
    }

    /**
     * 计算胎侧基础应排需求。
     *
     * @param input   需求量计算输入
     * @param context 胎侧排程上下文（当前算法暂不使用，预留扩展）
     * @return 需求量计算结果
     * @throws ServiceException 入参为空时抛出
     */
    @Override
    public TcDemandQtyResult calculate(TcDemandQtyInput input, TcScheduleContext context) {
        if (input == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_INVENTORY_PREDICT_INVALID.getDefaultMessage());
        }
        BigDecimal currentDemand = nvl(input.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(input.getGuardDemandQty());
        BigDecimal rollingStock = nvl(input.getRollingStockQty());
        int guardShiftCount = input.getGuardShiftCount() == null || input.getGuardShiftCount() <= 0
                ? TcScheduleConstants.DEFAULT_MIN_STOCK_CLASS_VALUE : input.getGuardShiftCount();
        BigDecimal currentShiftStockGap = currentDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal stockGap = guardDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal demandQty = currentShiftStockGap.max(stockGap);

        TcDemandQtyResult result = new TcDemandQtyResult();
        result.setCurrentShiftDemandQty(currentDemand);
        result.setGuardDemandQty(guardDemand);
        result.setRollingStockQty(rollingStock);
        result.setCurrentShiftStockGapQty(currentShiftStockGap);
        result.setStockGapQty(stockGap);
        result.setDemandQty(demandQty);
        result.setGuardShiftCount(guardShiftCount);
        result.setSupplyHours(calculateSupplyHours(rollingStock, guardDemand, input.getGuardRangeHours()));
        result.setCalcDesc("按当前班初滚动库存计算，需求=max(当前班库存缺口, 保证范围库存缺口)");
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
        BigDecimal futureDemandPerHour = demand.divide(hours,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
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
