package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
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
 * 胎面下班成型需求量策略。
 *
 * <p>数据加载层在算法 2 下已将当前胎面班次与后续库存保证范围需求拆分为不重叠区间。
 * 本策略负责汇总两段需求后计算库存缺口。</p>
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
        result.setCalcDesc("算法2按不重叠的当班成型需求与保证范围需求计算，需求为两者合计扣减库存后的缺口");
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
