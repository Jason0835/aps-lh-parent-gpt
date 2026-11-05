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
     * 基于尚未被本规则截短的原机、日目标和精确历史关系预判额外增机资格。
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
        if (!StringUtils.equals("S", source.getProductStatus()) || source.isStrictTargetQty()
                || source.isStrictNewSpecShortageOnly() || context.isFutureOnlyEarlyProductionCandidate(source)
                || StringUtils.equals(SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getCode(), source.getSourceType())
                || StringUtils.equals(event.getMaterialCode(), source.getMaterialCode())
                || !MouldChangeTypeEnum.REGULAR.getCode().equals(
                        PreviousAlternateActionPlan.resolve(context, event).getChangeMouldType())
                || candidate.getTargetMachineCount() <= 0
                || candidate.getScheduledMachineCount() < candidate.getTargetMachineCount()
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
}
