package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmPlanQtyResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面默认计划量策略。
 *
 * <p>默认策略只负责计算与机台无关的基础计划量：库存抵扣、最小起排、卷数取整、收尾基础量。
 * 损耗率解析、工装限制和产能压缩统一在机台分配阶段处理，避免机台维度规则提前固化。</p>
 */
@Component
public class TmDefaultPlanQtyStrategy implements ITmPlanQtyStrategy {

    /** 是 */
    private static final String YES = "1";

    @Override
    public String getStrategyCode() {
        return "DEFAULT";
    }

    @Override
    public TmPlanQtyResult calculate(TmTaskDraft taskDraft, TmScheduleContext context) {
        if (taskDraft == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
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

        TmPlanQtyResult result = new TmPlanQtyResult();
        result.setBaseDemandQty(planQty);
        result.setLossAddQty(BigDecimal.ZERO);
        result.setMinStartAdjustQty(BigDecimal.ZERO);
        result.setTailRoundAdjustQty(BigDecimal.ZERO);
        result.setToolLimitAdjustQty(BigDecimal.ZERO);
        result.setToolOverflowQty(BigDecimal.ZERO);
        result.setCapacityAdjustQty(BigDecimal.ZERO);

        boolean tailTask = isTailTask(taskDraft, planQty);
        if (tailTask) {
            BigDecimal beforeTail = planQty;
            planQty = nvl(taskDraft.getTailBalanceQty()).multiply(nvl(taskDraft.getTreadShoulderLength()));
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

    private boolean isTailTask(TmTaskDraft taskDraft, BigDecimal baseDemandQty) {
        BigDecimal tailBaseQty = nvl(taskDraft.getTailBalanceQty()).multiply(nvl(taskDraft.getTreadShoulderLength()));
        return YES.equals(taskDraft.getTailFlag())
                && nvl(taskDraft.getTailBalanceQty()).compareTo(BigDecimal.ZERO) > 0
                && nvl(taskDraft.getTreadShoulderLength()).compareTo(BigDecimal.ZERO) > 0
                && tailBaseQty.compareTo(nvl(baseDemandQty)) <= 0;
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