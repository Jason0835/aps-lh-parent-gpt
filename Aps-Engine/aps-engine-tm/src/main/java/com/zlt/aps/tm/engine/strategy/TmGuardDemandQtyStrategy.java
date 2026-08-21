package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.ScheduleSupplyDurationCalculator;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.enums.TmDemandAlgorithmEnum;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmDemandQtyInput;
import com.zlt.aps.tm.engine.domain.TmDemandQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 胎面库存保证需求量策略。
 *
 * <p>将不重叠的当前班需求和库存最低保证范围需求合并后计算库存缺口。
 * 本策略只使用 当前班初滚动库存，不接入来源未确认的已计划入库量、已占用量、不良量和调整量。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TmStrategyRegistry} 按算法编码 "1" 收集。</p>
 */
@Component
public class TmGuardDemandQtyStrategy implements ITmDemandQtyStrategy {

    /**
     * 获取算法编码。
     *
     * @return 算法编码
     */
    @Override
    public String getAlgorithmCode() {
        return TmDemandAlgorithmEnum.GUARD.getCode();
    }

    /**
     * 计算胎面基础应排需求。
     *
     * @param input   需求量计算输入
     * @param context 胎面排程上下文（当前算法暂不使用，预留扩展）
     * @return 需求量计算结果
     * @throws ServiceException 入参为空时抛出
     */
    @Override
    public TmDemandQtyResult calculate(TmDemandQtyInput input, TmScheduleContext context) {
        if (input == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_INVENTORY_PREDICT_INVALID.getDefaultMessage());
        }
        BigDecimal currentDemand = nvl(input.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(input.getGuardDemandQty());
        BigDecimal rollingStock = nvl(input.getRollingStockQty());
        int guardShiftCount = input.getGuardShiftCount() == null || input.getGuardShiftCount() <= 0
                ? TmScheduleConstants.DEFAULT_GUARD_SHIFT_COUNT : input.getGuardShiftCount();
        BigDecimal currentShiftStockGap = currentDemand.subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal stockGap = currentDemand.add(guardDemand).subtract(rollingStock).max(BigDecimal.ZERO);
        BigDecimal demandQty = stockGap;

        TmDemandQtyResult result = new TmDemandQtyResult();
        result.setCurrentShiftDemandQty(currentDemand);
        result.setGuardDemandQty(guardDemand);
        result.setRollingStockQty(rollingStock);
        result.setCurrentShiftStockGapQty(currentShiftStockGap);
        result.setStockGapQty(stockGap);
        result.setDemandQty(demandQty);
        result.setGuardShiftCount(guardShiftCount);
        result.setSupplyHours(ScheduleSupplyDurationCalculator.calculate(rollingStock,
                input.getFormingGuardWindowQtyMap(), input.getFormingGuardWindowHoursMap()).getSupplyHours());
        result.setCalcDesc("按当前班初滚动库存计算，需求=当班成型需求与保证范围需求之和扣减库存后的缺口");
        return result;
    }

    /**
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return BigDecimalUtils.valueOf(value);
    }
}
