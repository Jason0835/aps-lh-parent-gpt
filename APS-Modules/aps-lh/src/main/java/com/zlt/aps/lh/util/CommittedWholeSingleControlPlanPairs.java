package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从实际整机提交关系识别最终计划中的唯一双侧换模，不以展示前料反推独立动作。
 *
 * @author APS
 * @date 2026/10/18
 */
public final class CommittedWholeSingleControlPlanPairs {

    /**
     * 工具类禁止实例化。
     */
    private CommittedWholeSingleControlPlanPairs() {
    }

    /**
     * 将已证实同次提交的唯一配对计划关联到主侧，仅供最终物理事件计数读取。
     * 未提交、零量、同侧重复、时间/后料/状态/类型或计划属性不一致时保持原计数口径。
     *
     * @param context 已稳定的本批结果及实际提交关系
     * @param plans 最终有效及待核对交替计划
     * @return 配对侧计划到主侧计划的只读用途身份映射，不修改结果、计划或资源
     */
    public static Map<LhMouldChangePlan, LhMouldChangePlan> resolve(LhScheduleContext context,
            List<LhMouldChangePlan> plans) {
        Map<LhMouldChangePlan, LhMouldChangePlan> pairs = new IdentityHashMap<>();
        Set<LhScheduleResult> committed = Collections.newSetFromMap(new IdentityHashMap<>());
        committed.addAll(context.getScheduleResultList());
        context.getCommittedWholeSingleControlPairMap().forEach((pairedResult, primaryResult) -> {
            if (!committed.contains(primaryResult) || !committed.contains(pairedResult)
                    || !isSameAction(primaryResult, pairedResult)) {
                return;
            }
            List<LhMouldChangePlan> primaryPlans = plans.stream()
                    .filter(plan -> matchesResult(plan, primaryResult)).collect(Collectors.toList());
            List<LhMouldChangePlan> pairedPlans = plans.stream()
                    .filter(plan -> matchesResult(plan, pairedResult)).collect(Collectors.toList());
            // 一侧有多个最终计划时缺少唯一动作证据，禁止按机台简单去重。
            if (primaryPlans.size() != 1 || pairedPlans.size() != 1) {
                return;
            }
            LhMouldChangePlan primary = primaryPlans.get(0);
            LhMouldChangePlan paired = pairedPlans.get(0);
            if (StringUtils.equals(primary.getFactoryCode(), paired.getFactoryCode())
                    && StringUtils.equals(primary.getLhResultBatchNo(), paired.getLhResultBatchNo())
                    && Objects.equals(primary.getScheduleDate(), paired.getScheduleDate())
                    && Objects.equals(primary.getPlanDate(), paired.getPlanDate())
                    && StringUtils.equals(primary.getClassIndex(), paired.getClassIndex())
                    && StringUtils.equals(primary.getEndType(), paired.getEndType())
                    && StringUtils.equals(StringUtils.trimToEmpty(primary.getCleanType()),
                            StringUtils.trimToEmpty(paired.getCleanType()))) {
                pairs.put(paired, primary);
            }
        });
        return pairs;
    }

    /**
     * 校验主副结果确为同一物理机的互补双侧、同次正量交替。
     *
     * @param primary 主侧结果
     * @param paired 实际配对提交的副侧结果
     * @return 结果、后料、状态、开始时间及动作完全对应时为 true
     */
    private static boolean isSameAction(LhScheduleResult primary, LhScheduleResult paired) {
        return Objects.nonNull(primary) && Objects.nonNull(paired)
                && LhSingleControlMachineUtil.isSingleMouldMachine(primary.getLhMachineCode())
                && StringUtils.equals(LhSingleControlMachineUtil.resolvePairMachineCode(primary.getLhMachineCode()),
                paired.getLhMachineCode())
                && ShiftFieldUtil.resolveScheduledQty(primary) > 0 && ShiftFieldUtil.resolveScheduledQty(paired) > 0
                && Objects.nonNull(primary.getMouldChangeStartTime())
                && primary.getMouldChangeStartTime().equals(paired.getMouldChangeStartTime())
                && StringUtils.equals(primary.getMaterialCode(), paired.getMaterialCode())
                && StringUtils.equals(primary.getProductStatus(), paired.getProductStatus())
                && Objects.nonNull(resolveAction(primary))
                && StringUtils.equals(resolveAction(primary), resolveAction(paired));
    }

    /**
     * 将最终计划精确匹配回本次已提交结果，保留重复计划检测。
     *
     * @param plan 待匹配交替计划
     * @param result 已提交结果
     * @return 有效计划的运行侧、后料、状态、实际开始与动作均一致时为 true
     */
    private static boolean matchesResult(LhMouldChangePlan plan, LhScheduleResult result) {
        return Objects.nonNull(plan) && Objects.equals(plan.getIsDelete(), 0)
                && Objects.nonNull(plan.getPlanDate())
                && StringUtils.equals(plan.getLhMachineCode(), result.getLhMachineCode())
                && StringUtils.equals(plan.getAfterMaterialCode(), result.getMaterialCode())
                && StringUtils.equals(plan.getProductStatus(), result.getProductStatus())
                && Objects.equals(plan.getChangeTime(), result.getMouldChangeStartTime())
                && StringUtils.equals(plan.getChangeMouldType(), resolveAction(result));
    }

    /**
     * 按结果实际标识识别正规换模或换活字块，未换模续排不提供配对证据。
     *
     * @param result 已提交结果
     * @return 实际01/02动作；无动作时为空
     */
    private static String resolveAction(LhScheduleResult result) {
        if ("1".equals(result.getIsTypeBlock())) {
            return MouldChangeTypeEnum.TYPE_BLOCK.getCode();
        }
        return "1".equals(result.getIsChangeMould()) ? MouldChangeTypeEnum.REGULAR.getCode() : null;
    }
}
