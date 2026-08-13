package com.zlt.aps.tc.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleSupplyDurationCalculator;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcDemandAlgorithmEnum;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.domain.TcDemandQtyInput;
import com.zlt.aps.tc.engine.domain.TcDemandQtyResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 胎侧下班成型需求量策略。
 *
 * <p>数据加载层在算法 2 下已经把当前胎侧班次需求切换为“下个成型班次”的需求。
 * 本策略负责把该需求接入引擎注册表，并继续使用库存保证缺口参与基础应排量计算。</p>
 */
@Component
public class TcNextShiftDemandQtyStrategy implements ITcDemandQtyStrategy {

    /**
     * 获取算法编码。
     *
     * @return 算法编码 2
     */
    @Override
    public String getAlgorithmCode() {
        return TcDemandAlgorithmEnum.NEXT_SHIFT.getCode();
    }

    /**
     * 计算下班成型需求口径的胎侧基础应排需求。
     *
     * @param input   需求量计算输入
     * @param context 胎侧排程上下文
     * @return 需求量计算结果
     * @throws ServiceException 入参为空时抛出
     */
    @Override
    public TcDemandQtyResult calculate(TcDemandQtyInput input, TcScheduleContext context) {
        if (input == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_INVENTORY_PREDICT_INVALID.getDefaultMessage());
        }
        BigDecimal nextShiftDemand = nvl(input.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(input.getGuardDemandQty());
        BigDecimal rollingStock = nvl(input.getRollingStockQty());
        int guardShiftCount = input.getGuardShiftCount() == null || input.getGuardShiftCount() <= 0
                ? TcScheduleConstants.DEFAULT_MIN_STOCK_CLASS_VALUE : input.getGuardShiftCount();
        BigDecimal currentShiftStockGap = nextShiftDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal stockGap = guardDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal demandQty = currentShiftStockGap.max(stockGap);

        TcDemandQtyResult result = new TcDemandQtyResult();
        result.setCurrentShiftDemandQty(nextShiftDemand);
        result.setGuardDemandQty(guardDemand);
        result.setRollingStockQty(rollingStock);
        result.setCurrentShiftStockGapQty(currentShiftStockGap);
        result.setStockGapQty(stockGap);
        result.setDemandQty(demandQty);
        result.setGuardShiftCount(guardShiftCount);
        result.setSupplyHours(ScheduleSupplyDurationCalculator.calculate(rollingStock,
                input.getFormingGuardWindowQtyMap(), input.getFormingGuardWindowHoursMap()).getSupplyHours());
        result.setCalcDesc("算法2按下个成型班次需求计算，需求=max(下班成型库存缺口, 保证范围库存缺口)");
        return result;
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
