package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.api.enums.TmScheduleStrategyEnum;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.engine.domain.TmPlanQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面默认计划量策略。
 *
 * <p>默认策略负责计算与机台无关的基础计划量和派机前估算量。非收尾任务按最小起排、卷数取整形成估算量，
 * 供现有机台评分保持兼容；损耗率解析及“损耗、最小起排、卷数取整”的最终重算统一在机台分配阶段处理。</p>
 */
@Component
public class TmDefaultPlanQtyStrategy implements ITmPlanQtyStrategy {

    @Override
    public String getStrategyCode() {
        return TmScheduleStrategyEnum.DEFAULT.getCode();
    }

    @Override
    public TmPlanQtyResult calculate(TmTaskDraft taskDraft, TmScheduleContext context) {
        if (taskDraft == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        BigDecimal currentDemand = nvl(taskDraft.getCurrentShiftDemandQty());
        BigDecimal guardDemand = nvl(taskDraft.getGuardDemandQty());
        BigDecimal grossDemand = currentDemand.add(guardDemand);
        BigDecimal stock = nvl(taskDraft.getRollingStockQty());
        BigDecimal stockDeductQty = stock.min(grossDemand);
        BigDecimal planQty = grossDemand.subtract(stock).max(BigDecimal.ZERO);
        taskDraft.setStockDeductQty(stockDeductQty);

        TmPlanQtyResult result = new TmPlanQtyResult();
        result.setBaseDemandQty(planQty);
        result.setLossAddQty(BigDecimal.ZERO);
        result.setMinStartAdjustQty(BigDecimal.ZERO);
        result.setTailRoundAdjustQty(BigDecimal.ZERO);
        result.setToolLimitAdjustQty(BigDecimal.ZERO);
        result.setToolOverflowQty(BigDecimal.ZERO);
        result.setCapacityAdjustQty(BigDecimal.ZERO);

        BigDecimal preLossPlanQty = planQty;
        boolean tailTask = isTailTask(taskDraft, planQty);
        if (tailTask) {
            // 详设 §14.3 Step11：收尾规格实际排产 = min(需排产量, 月计划余量)，不执行最小起排和卷曲取整。
            // planQty 当前为 baseDemand(需排产量)，收尾分支取 min(baseDemand, tailBalanceQty×标准长度)。
            BigDecimal beforeTail = planQty;
            BigDecimal tailBaseQty = nvl(taskDraft.getTailBalanceQty()).multiply(nvl(taskDraft.getTreadShoulderLength()));
            planQty = planQty.min(tailBaseQty);
            preLossPlanQty = planQty;
            result.setTailRoundAdjustQty(planQty.subtract(beforeTail));
        } else {
            BigDecimal beforeMinStart = planQty;
            planQty = applyMinStartQty(taskDraft, planQty);
            result.setMinStartAdjustQty(planQty.subtract(beforeMinStart));

            BigDecimal beforeRound = planQty;
            planQty = roundToCurlLength(taskDraft, planQty);
            result.setTailRoundAdjustQty(planQty.subtract(beforeRound));
        }

        // preLossPlanQty 必须保留损耗计算前的真实基础量，不能使用已补最小起排或已取整的派机估算量。
        result.setPreLossPlanQty(preLossPlanQty);
        result.setPlanQtyBeforeToolLimit(planQty);
        result.setFinalPlanQty(planQty);
        taskDraft.setPlanStockQty(calculateHandoverStock(stock, currentDemand, planQty));
        result.setCalcFormulaDesc(tailTask ? "收尾余量" : "基础需求->库存抵扣->派机前最小起排与卷数取整估算");
        taskDraft.setPlanQty(planQty);
        return result;
    }

    private boolean isTailTask(TmTaskDraft taskDraft, BigDecimal baseDemandQty) {
        // 详设 §14.3 Step11：收尾判定只看收尾标识与余量/长度有效性，不再要求 月计划余量<=需排产量。
        // 月计划余量>需排产量时仍按收尾口径取 min(需排产量, 月计划余量)=需排产量，不执行 minStart/卷曲取整。
        return TmYesNoEnum.YES.getCode().equals(taskDraft.getTailFlag())
                && nvl(taskDraft.getTailBalanceQty()).compareTo(BigDecimal.ZERO) > 0
                && nvl(taskDraft.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal applyMinStartQty(TmTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal minStartQty = nvl(taskDraft.getMinStartQty());
        if (planQty.compareTo(BigDecimal.ZERO) > 0 && minStartQty.compareTo(BigDecimal.ZERO) > 0
                && planQty.compareTo(minStartQty) < 0) {
            return minStartQty;
        }
        return planQty;
    }

    private BigDecimal roundToCurlLength(TmTaskDraft taskDraft, BigDecimal planQty) {
        BigDecimal curlLength = resolveCurlLength(taskDraft);
        if (planQty.compareTo(BigDecimal.ZERO) <= 0 || curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            return planQty;
        }
        return planQty.divide(curlLength, 0, RoundingMode.CEILING).multiply(curlLength);
    }

    private BigDecimal resolveCurlLength(TmTaskDraft taskDraft) {
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
