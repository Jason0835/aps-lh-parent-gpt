package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.enums.TmDemandAlgorithmEnum;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmDemandQtyInput;
import com.zlt.aps.tm.engine.domain.TmDemandQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面下班成型需求量策略。
 *
 * <p>数据加载层在算法 2 下已经把当前胎面班次需求切换为“下个成型班次”的需求。
 * 本策略负责把该需求接入引擎注册表，并继续使用库存保证缺口参与基础应排量计算。</p>
 */
@Component
public class TmNextShiftDemandQtyStrategy implements ITmDemandQtyStrategy {

    /**
     * 获取算法编码。
     *
     * @return 算法编码 2
     */
    @Override
    public String getAlgorithmCode() {
        return TmDemandAlgorithmEnum.NEXT_SHIFT.getCode();
    }

    /**
     * 计算下班成型需求口径的胎面基础应排需求。
     *
     * @param input   需求量计算输入
     * @param context 胎面排程上下文
     * @return 需求量计算结果
     * @throws ServiceException 入参为空时抛出
     */
    @Override
    public TmDemandQtyResult calculate(TmDemandQtyInput input, TmScheduleContext context) {
        if (input == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_INVENTORY_PREDICT_INVALID.getDefaultMessage());
        }
        BigDecimal nextShiftDemand = nvl(input.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(input.getGuardDemandQty());
        BigDecimal rollingStock = nvl(input.getRollingStockQty());
        int guardShiftCount = input.getGuardShiftCount() == null || input.getGuardShiftCount() <= 0
                ? TmScheduleConstants.DEFAULT_GUARD_SHIFT_COUNT : input.getGuardShiftCount();
        BigDecimal currentShiftStockGap = nextShiftDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal stockGap = guardDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal demandQty = currentShiftStockGap.max(stockGap);

        TmDemandQtyResult result = new TmDemandQtyResult();
        result.setCurrentShiftDemandQty(nextShiftDemand);
        result.setGuardDemandQty(guardDemand);
        result.setRollingStockQty(rollingStock);
        result.setCurrentShiftStockGapQty(currentShiftStockGap);
        result.setStockGapQty(stockGap);
        result.setDemandQty(demandQty);
        result.setGuardShiftCount(guardShiftCount);
        result.setSupplyHours(this.calculateSupplyHours(rollingStock, guardDemand, input.getGuardRangeHours()));
        result.setCalcDesc("算法2按下个成型班次需求计算，需求=max(下班成型库存缺口, 保证范围库存缺口)");
        return result;
    }

    /**
     * 计算库存供应时长。
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
                TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
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
