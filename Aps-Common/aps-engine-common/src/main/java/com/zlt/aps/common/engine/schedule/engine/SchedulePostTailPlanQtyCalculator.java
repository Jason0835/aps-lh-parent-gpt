package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/**
 * 收尾间隔内禁排计划量结果构造器。
 *
 * <p>只保留待承接数量，不产生实际生产量、库存入账或工装消耗；领域服务负责写入
 * 对应 TM/TC 未排原因和解释证据。</p>
 */
public final class SchedulePostTailPlanQtyCalculator {

    private SchedulePostTailPlanQtyCalculator() {
    }

    /**
     * 按“当班需求＋保证需求－班初可用库存”的口径构造禁排结果。
     *
     * @param task 当前任务
     * @return 公共计划量结果
     */
    public static SchedulePlanQtyResultModel calculate(ScheduleTaskDraftModel task) {
        if (task == null) {
            return null;
        }
        return calculate(task, new SchedulePlanQtyResultModel());
    }

    /**
     * 使用调用方创建的结果对象构造收尾间隔内禁排结果，保留 TM/TC 的具体结果类型。
     *
     * @param <R>    计划量结果类型
     * @param task   当前任务
     * @param result 调用方创建的计划量结果对象
     * @return 填充后的原结果对象
     */
    public static <R extends SchedulePlanQtyResultModel> R calculate(
            ScheduleTaskDraftModel task, R result) {
        if (task == null || result == null) {
            return result;
        }
        BigDecimal currentDemandQty = nvl(task.getCurrentShiftDemandQty());
        BigDecimal guardDemandQty = nvl(task.getGuardDemandQty());
        BigDecimal stockQty = nvl(task.getRollingStockQty());
        BigDecimal grossDemandQty = currentDemandQty.add(guardDemandQty);
        BigDecimal blockedQty = grossDemandQty.subtract(stockQty).max(BigDecimal.ZERO);
        BigDecimal stockDeductQty = stockQty.min(grossDemandQty).max(BigDecimal.ZERO);
        task.setPostTailBlockedQty(blockedQty);
        task.setStockDeductQty(stockDeductQty);
        // 禁排不代表生产，班末库存仍须扣除本班实际消耗，避免把班初库存冻结到下一班。
        task.setPlanStockQty(stockQty.subtract(currentDemandQty).max(BigDecimal.ZERO));
        task.setBaseDemandQty(blockedQty);
        task.setLossAddQty(BigDecimal.ZERO);
        task.setToolLimitAdjustQty(BigDecimal.ZERO);
        task.setToolOverflowQty(BigDecimal.ZERO);
        task.setMinStartAdjustQty(BigDecimal.ZERO);
        task.setTailRoundAdjustQty(BigDecimal.ZERO);
        task.setCapacityAdjustQty(BigDecimal.ZERO);
        task.setPreLossPlanQty(blockedQty);
        task.setPlanQtyBeforeToolLimit(blockedQty);
        task.setPlanQty(blockedQty);
        task.setCalcFormulaDesc("收尾后起排班次限制");

        result.setBaseDemandQty(blockedQty);
        result.setLossAddQty(BigDecimal.ZERO);
        result.setToolLimitAdjustQty(BigDecimal.ZERO);
        result.setToolOverflowQty(BigDecimal.ZERO);
        result.setMinStartAdjustQty(BigDecimal.ZERO);
        result.setTailRoundAdjustQty(BigDecimal.ZERO);
        result.setCapacityAdjustQty(BigDecimal.ZERO);
        result.setPreLossPlanQty(blockedQty);
        result.setPlanQtyBeforeToolLimit(blockedQty);
        result.setFinalPlanQty(blockedQty);
        result.setCalcFormulaDesc(task.getCalcFormulaDesc());
        return result;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
