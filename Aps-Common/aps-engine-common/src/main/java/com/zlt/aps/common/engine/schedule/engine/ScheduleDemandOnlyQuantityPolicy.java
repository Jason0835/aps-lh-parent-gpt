package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintCalculator;
import com.zlt.aps.common.engine.schedule.constraint.SchedulePlanQtyAdjustmentResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Objects;

/**
 * 按需求生产的显式数量策略。仅领域证明来源完整后启用，不修改真实卷长。
 */
public final class ScheduleDemandOnlyQuantityPolicy {

    private ScheduleDemandOnlyQuantityPolicy() {
    }

    /**
     * 判断任务是否由领域启用免整卷策略。
     *
     * @param task 运行态任务
     * @return 已证明且最低量为正时返回true
     */
    public static boolean enabled(ScheduleTaskDraftModel task) {
        return task != null && BigDecimalUtils.valueOf(task.getDemandOnlyMinimumQty()).signum() > 0;
    }

    /**
     * 计算正净需求的损耗、最低量与整数米，零缺口不新增生产。
     *
     * @param task     已证明免整卷的任务
     * @param request  损耗前净需求
     * @param lossRate 损耗百分比
     * @param scale    损耗精度
     * @return 各阶段证据；整卷补量恒为零
     */
    public static SchedulePlanQtyAdjustmentResult settle(ScheduleTaskDraftModel task, BigDecimal request,
                                                         BigDecimal lossRate, int scale) {
        SchedulePlanQtyAdjustmentResult result = new ScheduleConstraintCalculator().calculatePlanQtyAfterLoss(
                request, lossRate, task.getDemandOnlyMinimumQty(), null, false, scale);
        BigDecimal before = result.getFinalPlanQty();
        BigDecimal after = before.setScale(0, RoundingMode.CEILING);
        result.setFinalPlanQty(after);
        return result;
    }

    /**
     * 在硬数量上限内选取整数米，不足最低量时不创建残行。产能超用须由调用方单独授权。
     *
     * @param task   已证明免整卷的任务
     * @param target 整行目标
     * @param limits 工装、时间等硬上限
     * @return 最大合法整数米；不足最低量或无缺口返回0
     */
    public static BigDecimal allowed(ScheduleTaskDraftModel task, BigDecimal target, BigDecimal... limits) {
        BigDecimal quantity = BigDecimalUtils.valueOf(target).max(BigDecimal.ZERO);
        if (limits != null) {
            quantity = Arrays.stream(limits).filter(Objects::nonNull)
                    .map(value -> value.max(BigDecimal.ZERO)).reduce(quantity, BigDecimal::min);
        }
        quantity = quantity.setScale(0, RoundingMode.FLOOR);
        return quantity.compareTo(task.getDemandOnlyMinimumQty()) < 0 ? BigDecimal.ZERO : quantity;
    }

    /**
     * 对正剩余产能应用最低量例外；不授权工装或时间门禁超用。
     *
     * @param task            已证明免整卷的任务
     * @param requested       已结算目标
     * @param capacityLimited 原始产能可承接量
     * @return 正产能不足最低量时返回最低量，其余按整数米承接
     */
    public static BigDecimal capacity(ScheduleTaskDraftModel task, BigDecimal requested, BigDecimal capacityLimited) {
        BigDecimal raw = BigDecimalUtils.valueOf(requested).min(BigDecimalUtils.valueOf(capacityLimited));
        if (raw.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return raw.setScale(0, RoundingMode.FLOOR).max(task.getDemandOnlyMinimumQty());
    }
}
