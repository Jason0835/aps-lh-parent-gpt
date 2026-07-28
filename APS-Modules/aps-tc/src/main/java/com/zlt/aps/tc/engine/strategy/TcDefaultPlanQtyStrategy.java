package com.zlt.aps.tc.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.engine.domain.TcPlanQtyResult;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎侧默认计划量策略。
 *
 * <p>默认策略只负责计算与机台无关的基础计划量：库存抵扣、最小起排、卷数取整、收尾基础量。
 * 损耗率解析、工装限制和产能压缩统一在机台分配阶段处理，避免机台维度规则提前固化。</p>
 */
@Component
public class TcDefaultPlanQtyStrategy implements ITcPlanQtyStrategy {

    @Override
    public String getStrategyCode() {
        return TcScheduleStrategyEnum.DEFAULT.getCode();
    }

    @Override
    public TcPlanQtyResult calculate(TcTaskDraft taskDraft, TcScheduleContext context) {
        if (taskDraft == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage());
        }
        BigDecimal currentDemand = nvl(taskDraft.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(taskDraft.getGuardDemandQty());
        BigDecimal grossDemand = currentDemand.max(guardDemand);
        BigDecimal stock = nvl(taskDraft.getRollingStockQty());
        BigDecimal stockDeductQty = stock.min(grossDemand);
        BigDecimal planQty = currentDemand.subtract(stock)
                .max(guardDemand.subtract(stock))
                .max(BigDecimal.ZERO);
        taskDraft.setStockDeductQty(stockDeductQty);

        TcPlanQtyResult result = new TcPlanQtyResult();
        result.setBaseDemandQty(planQty);
        result.setLossAddQty(BigDecimal.ZERO);
        result.setMinStartAdjustQty(BigDecimal.ZERO);
        result.setTailRoundAdjustQty(BigDecimal.ZERO);
        result.setToolLimitAdjustQty(BigDecimal.ZERO);
        result.setToolOverflowQty(BigDecimal.ZERO);
        result.setCapacityAdjustQty(BigDecimal.ZERO);

        boolean tailTask = isTailTask(taskDraft, planQty);
        if (tailTask) {
            // 详设 §14.3 Step11：收尾规格实际排产 = min(需排产量, 月计划余量)，不执行最小起排和卷曲取整。
            // planQty 当前为 baseDemand(需排产量)，收尾分支取 min(baseDemand, tailBalanceQty×标准长度)。
            BigDecimal beforeTail = planQty;
            BigDecimal tailBaseQty = nvl(taskDraft.getTailBalanceQty()).multiply(nvl(taskDraft.getSidewallLength()));
            planQty = planQty.min(tailBaseQty);
            result.setTailRoundAdjustQty(planQty.subtract(beforeTail));
        } else {
            BigDecimal beforeMinStart = planQty;
            planQty = applyMinStartQty(taskDraft, planQty);
            result.setMinStartAdjustQty(planQty.subtract(beforeMinStart));

            BigDecimal beforeRound = planQty;
            planQty = roundToCurlLength(taskDraft, planQty);
            result.setTailRoundAdjustQty(planQty.subtract(beforeRound));
        }

        result.setPreLossPlanQty(planQty);
        result.setPlanQtyBeforeToolLimit(planQty);
        result.setFinalPlanQty(planQty);
        taskDraft.setPlanStockQty(calculateHandoverStock(stock, currentDemand, planQty));
        result.setCalcFormulaDesc(tailTask ? "收尾余量" : "基础需求->库存抵扣->最小起排->卷数取整");
        taskDraft.setPlanQty(planQty);
        return result;
    }

    private boolean isTailTask(TcTaskDraft taskDraft, BigDecimal baseDemandQty) {
        // 详设 §14.3 Step11：收尾判定只看收尾标识与余量/长度有效性，不再要求 月计划余量<=需排产量。
        // 月计划余量>需排产量时仍按收尾口径取 min(需排产量, 月计划余量)=需排产量，不执行 minStart/卷曲取整。
        return TcYesNoEnum.YES.getCode().equals(taskDraft.getTailFlag())
                && nvl(taskDraft.getTailBalanceQty()).compareTo(BigDecimal.ZERO) > 0
                && nvl(taskDraft.getSidewallLength()).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal applyMinStartQty(TcTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal minStartQty = nvl(taskDraft.getMinStartQty());
        if (planQty.compareTo(BigDecimal.ZERO) > 0 && minStartQty.compareTo(BigDecimal.ZERO) > 0
                && planQty.compareTo(minStartQty) < 0) {
            return minStartQty;
        }
        return planQty;
    }

    private BigDecimal roundToCurlLength(TcTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal curlLength = resolveCurlLength(taskDraft);
        if (planQty.compareTo(BigDecimal.ZERO) <= 0 || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return planQty;
        }
        return planQty.divide(curlLength, 0, RoundingMode.CEILING).multiply(curlLength);
    }

    private BigDecimal resolveCurlLength(TcTaskDraft taskDraft) {
        if (taskDraft.getCurlRollLength() != null && taskDraft.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return taskDraft.getCurlRollLength();
        }
        return nvl(taskDraft.getDefaultCurlRollLength());
    }

    private BigDecimal calculateHandoverStock(BigDecimal shiftStartStock, BigDecimal currentDemand, BigDecimal finalPlanQty) {
        return nvl(shiftStartStock).add(nvl(finalPlanQty)).subtract(nvl(currentDemand)).max(BigDecimal.ZERO);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
