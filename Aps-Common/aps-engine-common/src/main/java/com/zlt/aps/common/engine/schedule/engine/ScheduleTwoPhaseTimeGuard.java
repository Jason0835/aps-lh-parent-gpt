package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 已确认D3时间保护纯计算，不把班长作为第二份米数产能。
 */
public final class ScheduleTwoPhaseTimeGuard {

    /**
     * 一小时对应的毫秒数。
     */
    private static final BigDecimal MILLIS_PER_HOUR = new BigDecimal("3600000");

    /**
     * 用后置基础任务的时间余量限制本行追加；已超期任务不得进一步恶化。
     * 缺速度/时点采用用户确认的仅预警降级，仍校验其他已知后置任务。
     *
     * @param expandedNode    原地扩增节点
     * @param orderedNodes    当前机台本班任务链
     * @param resourceAllowed 资源已允许的追加量
     * @return 时间保护允许量，未向整卷取整（由数量计算器统一取整）
     */
    public BigDecimal limitByDownstreamBase(ScheduleTaskNode<? extends ScheduleTaskDraftModel> expandedNode,
                                            List<? extends ScheduleTaskNode<? extends ScheduleTaskDraftModel>> orderedNodes,
                                            BigDecimal resourceAllowed) {
        ScheduleTaskDraftModel expandedTask = expandedNode.getTask();
        ScheduleTwoPhaseQuantityCalculator quantities = new ScheduleTwoPhaseQuantityCalculator();
        BigDecimal speed = BigDecimalUtils.valueOf(expandedTask.getMachineSpeed());
        BigDecimal allowed = BigDecimalUtils.valueOf(resourceAllowed).max(BigDecimal.ZERO);
        if (speed.signum() <= 0) {
            quantities.addReason(expandedTask, "TIME_GUARD_DEGRADED_DUE_TO_MISSING_SPEED");
            return allowed;
        }
        // 原行基础部分的起点及生产速度不变；追加只能推迟后置行，不能用本行旧基础期限误拦备库。
        boolean afterExpanded = false;
        for (ScheduleTaskNode<? extends ScheduleTaskDraftModel> node : orderedNodes) {
            if (node == expandedNode) {
                afterExpanded = true;
                continue;
            }
            if (!afterExpanded || node.getTask() == null
                    || BigDecimalUtils.valueOf(node.getTask().getBaseAssignQty()).signum() <= 0) {
                continue;
            }
            Date latestStart = node.getTask().getLatestStartTime();
            Date estimatedStart = node.getStartTime();
            if (BigDecimalUtils.valueOf(node.getTask().getMachineSpeed()).signum() <= 0
                    || latestStart == null || estimatedStart == null) {
                quantities.addReason(expandedTask, "TIME_GUARD_DEGRADED_DUE_TO_MISSING_SPEED");
                continue;
            }
            BigDecimal slackMillis = BigDecimalUtils.valueOf(latestStart.getTime())
                    .subtract(BigDecimalUtils.valueOf(estimatedStart.getTime())).max(BigDecimal.ZERO);
            BigDecimal safeQty = slackMillis.multiply(speed).divide(MILLIS_PER_HOUR, 12, RoundingMode.FLOOR);
            allowed = allowed.min(safeQty);
        }
        if (allowed.compareTo(BigDecimalUtils.valueOf(resourceAllowed)) < 0) {
            quantities.addReason(expandedTask, "DOWNSTREAM_BASE_DELAY_BLOCKED");
        }
        return allowed;
    }
}
