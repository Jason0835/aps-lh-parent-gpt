package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 历史指定额外增机的无副作用资格判断，正式承接和结构收尾前置冻结共用。
 *
 * @author Codex
 * @date 2026/10/29
 */
public final class PreviousAlternateAdditionalMachinePolicy {
    /**
     * 工具类禁止实例化。
     */
    private PreviousAlternateAdditionalMachinePolicy() {
    }

    /**
     * 基于尚未被本规则截短的原机和精确历史关系预判独立承接资格。
     *
     * @param context 原需求和原资源上下文
     * @param day 本次生产业务日
     * @param event 有效指定关系
     * @param source 后物料需求
     * @param candidate 原日目标及有效在机数
     * @return 只读资格；不满足原规则时为空
     */
    public static PreviousAlternateAdditionalMachineDemand preview(LhScheduleContext context,
            DayScheduleContext day, PreviousAlternatePlanReleaseEvent event,
            SkuScheduleDTO source, DailyNewSpecCandidate candidate) {
        return preview(context, day, event, source, candidate,
                source.isStrictTargetQty() && isOrdinarySurplusEnding(context, source));
    }

    /**
     * 共用正式收尾分组和结构收尾前置冻结的历史资格，数量上限不充当台数限制。
     *
     * @param context 原需求和资源上下文
     * @param day 本次生产业务日
     * @param event 有效指定关系
     * @param source 后物料需求
     * @param candidate 原日目标及有效在机数
     * @param ordinarySurplusEnding 已由续作内核确认的普通硫化余量收尾身份
     * @return 需要保留原续作的指定资格，不满足原规则时为空
     */
    public static PreviousAlternateAdditionalMachineDemand preview(LhScheduleContext context,
            DayScheduleContext day, PreviousAlternatePlanReleaseEvent event,
            SkuScheduleDTO source, DailyNewSpecCandidate candidate, boolean ordinarySurplusEnding) {
        // 普通余量归整也会建立严格数量上限；只豁免其台数资格，不解除严格控量或其他来源限制。
        if (!StringUtils.equals("S", source.getProductStatus())
                || (source.isStrictTargetQty() && !ordinarySurplusEnding)
                || source.isStrictNewSpecShortageOnly() || context.isFutureOnlyEarlyProductionCandidate(source)
                || StringUtils.equals(SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getCode(), source.getSourceType())
                || StringUtils.equals(event.getMaterialCode(), source.getMaterialCode())
                || !MouldChangeTypeEnum.REGULAR.getCode().equals(
                        PreviousAlternateActionPlan.resolve(context, event).getChangeMouldType())
                || day.getScheduleDate().isBefore(event.getPlan().getPlanDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate())) {
            return null;
        }
        Set<String> retainedMachines = context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType()))
                .filter(result -> StringUtils.equals(source.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(source.getProductStatus(), result.getProductStatus()))
                .filter(result -> Objects.nonNull(result.getSpecEndTime()) && result.getSpecEndTime()
                        .after(day.getDayStartTime()))
                .filter(result -> !context.getPreviousAlternateReleaseEventMap().containsKey(result.getLhMachineCode())
                        && !context.getPreviousAlternateReleasedMachineTimeMap().containsKey(result.getLhMachineCode())
                        && !context.getTimedMachineOffDecisionMap().containsKey(result)
                        && !context.isContinuousStopHoldMachine(result.getLhMachineCode()))
                .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                .filter(code -> !StringUtils.equals(code,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(event.getMachineCode())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (retainedMachines.isEmpty() || source.getSurplusQty() <= 0) {
            return null;
        }
        return new PreviousAlternateAdditionalMachineDemand(event.getPlan(),
                MonthPlanDateResolver.buildMaterialStatusKey(source.getMaterialCode(), source.getProductStatus()),
                day.getScheduleDate(), candidate.getScheduledMachineCount() + 1, retainedMachines);
    }

    /**
     * 复用已确认的普通余量收尾身份，避免把胎胚硬目标或独立调整误作数量归整。
     * 仅在精确历史资格路径查询现有分组，同源副本必须共享日计划账本。
     *
     * @param context 本批原机收尾分组及冻结状态
     * @param source 当前原需求或同源副本
     * @return 严格数量来自普通硫化余量收尾时为真
     */
    private static boolean isOrdinarySurplusEnding(LhScheduleContext context, SkuScheduleDTO source) {
        StructureEndingContinuationState state = StructureEndingContinuationPolicy.state(context, source);
        if (state != null && state.isOriginalRemainderEnding()) {
            return true;
        }
        return context.getContinuationRemainderFinishGroups().stream()
                .filter(group -> group.getEndingType() == ContinuationRemainderFinishGroup.EndingType.SURPLUS)
                .map(ContinuationRemainderFinishGroup::getSourceSku)
                .filter(Objects::nonNull)
                .anyMatch(original -> original == source
                        || (source.getDailyPlanQuotaMap() != null
                        && original.getDailyPlanQuotaMap() == source.getDailyPlanQuotaMap()
                        && StringUtils.equals(original.getMaterialCode(), source.getMaterialCode())
                        && StringUtils.equals(original.getProductStatus(), source.getProductStatus())));
    }
}
