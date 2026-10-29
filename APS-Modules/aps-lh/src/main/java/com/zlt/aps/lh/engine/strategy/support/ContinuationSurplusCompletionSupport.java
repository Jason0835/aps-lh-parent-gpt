package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 普通余量组的最终完成依据，防止历史截量后的单机旧快照继续证明自然收尾。
 * 胎胚份额及按时间下机等独立退出事实不由本工具重新解释。
 *
 * @author Codex
 * @date 2026/10/27
 */
public final class ContinuationSurplusCompletionSupport {

    /**
     * 工具类不允许实例化。
     */
    private ContinuationSurplusCompletionSupport() {
    }

    /**
     * 按原冻结总目标核对同物料状态的最终实机排量，固定生产段也计入完成量。
     *
     * @param context 当前有效排程结果
     * @param group 普通余量收尾组
     * @return 尚未消化的合法余量；胎胚组或未冻结目标返回零
     */
    public static int remainingQty(LhScheduleContext context, ContinuationRemainderFinishGroup group) {
        if (group.getEndingType() != ContinuationRemainderFinishGroup.EndingType.SURPLUS
                || Objects.isNull(group.getFinalTargetQty())) {
            return 0;
        }
        SkuScheduleDTO source = group.getSourceSku();
        long scheduled = context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(source.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(source.getProductStatus(), result.getProductStatus()))
                .filter(result -> !LhSingleControlMachineUtil.isVirtualMachineCode(result.getLhMachineCode()))
                .mapToLong(ShiftFieldUtil::resolveScheduledQty).sum();
        return (int) Math.max(0L, group.getFinalTargetQty().longValue() - scheduled);
    }

    /**
     * 判断本行是否仍属于未完成普通余量组的在机续作，排除已有独立硬退出的结果。
     *
     * @param context 当前完成量、退出决定及快照
     * @param result 原续作结果
     * @return 为真时不得使用本行旧节点证明自然收尾
     */
    public static boolean isUnfinishedContinuation(LhScheduleContext context, LhScheduleResult result) {
        ContinuationEndingAllocationSnapshot snapshot = context.getContinuationSurplusEndingSnapshotMap().get(result);
        if (Objects.isNull(snapshot)
                || snapshot.getFinishState() == ContinuationMachineFinishPlan.FinishState.FORCED_RELEASE
                || context.hasTimedMachineOffRequest(result)
                || context.getTimedMachineOffDecisionMap().containsKey(result)) {
            return false;
        }
        return hasIncompleteGroup(context, result);
    }

    /**
     * 判断结果所属普通余量组是否尚未完成，独立退出机台仍属于该组的数量核对范围。
     *
     * @param context 当前有效结果与冻结目标
     * @param result 原组结果
     * @return 是否属于存在实机排量缺口的普通余量组
     */
    public static boolean hasIncompleteGroup(LhScheduleContext context, LhScheduleResult result) {
        return context.getContinuationRemainderFinishGroups().stream()
                .filter(group -> Objects.nonNull(group.getOriginalResults())
                        && group.getOriginalResults().contains(result))
                .anyMatch(group -> remainingQty(context, group) > 0);
    }

    /**
     * 核对当前物理机台仍在承接的普通余量，历史释放清空在机身份时也检查其原续作结果。
     *
     * @param context 当前机台及有效结果
     * @param machine 待判断的物理机台或运行侧
     * @return 是否存在未完成且没有独立退出事实的在机续作
     */
    public static boolean hasUnfinishedContinuation(LhScheduleContext context, MachineScheduleDTO machine) {
        if (Objects.isNull(machine)) {
            return false;
        }
        return context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(machine.getMachineCode(), result.getLhMachineCode()))
                .filter(result -> StringUtils.isBlank(machine.getCurrentMaterialCode())
                        || StringUtils.equals(machine.getCurrentMaterialCode(), result.getMaterialCode()))
                .anyMatch(result -> isUnfinishedContinuation(context, result));
    }
}
