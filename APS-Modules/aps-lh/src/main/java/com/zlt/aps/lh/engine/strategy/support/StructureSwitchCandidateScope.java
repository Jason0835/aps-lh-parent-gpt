package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.service.impl.PreviousAlternatePlanEligibilityService;
import org.apache.commons.lang3.StringUtils;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 结构切换步骤的候选归属。只冻结身份与来源，不复制或初始化数量、资源账本。
 * 普通入口按业务键分流，派生补偿对象不能绕过；独立班次9继续沿用原窗口隔离规则。
 * @author APS
 * @since 2026/10/09
 */
public final class StructureSwitchCandidateScope {
    /**
     * 工具类禁止实例化。
     */
    private StructureSwitchCandidateScope() {
    }

    /**
     * 生成与现有数量账本一致的物料和产品状态业务键。
     *
     * @param sku 物料及状态
     * @return 与现有账本一致的业务键
     */
    public static String skuKey(SkuScheduleDTO sku) {
        return MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
    }

    /**
     * 续作数量和设备复评稳定后冻结，历史承接及换活字块只分流这些真实切换候选。
     *
     * @param context 稳定后的续作上下文
     */
    public static void freeze(LhScheduleContext context) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context) || context.isIsolatedNextShiftPlan()) {
            return;
        }
        List<LocalDate> dates = context.getScheduleWindowShifts().stream()
                .filter(shift -> Objects.nonNull(shift.getWorkDate()))
                .map(shift -> shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .distinct().sorted().collect(Collectors.toList());
        List<SkuScheduleDTO> candidates = Stream.concat(context.getNewSpecSkuList().stream(),
                context.getPreviousAlternateNewSpecCandidates().stream()).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        candidates.forEach(sku -> {
            String key = skuKey(sku);
            if (context.getStructureSwitchCandidateSourceMap().containsKey(key)) {
                return;
            }
            CxEmbryoLhTime source = dates.stream()
                    .map(date -> StructureSwitchSchedulingPolicy.resolveSource(context, sku, date))
                    .filter(Objects::nonNull).findFirst().orElse(null);
            if (Objects.nonNull(source)) {
                context.getStructureSwitchCandidateSkuMap().put(key, sku);
                context.getStructureSwitchCandidateSourceMap().put(key, source);
            }
        });
        // 原策略在结构S0提交后会把同结构其他SKU归入同一来源；在独立步骤提前冻结该组，
        // 防止这些SKU留在普通新增中再次触发结构切换。各SKU实际准入仍由公共物料资格判断。
        Map<String, CxEmbryoLhTime> structures = context.getStructureSwitchCandidateSourceMap().values().stream()
                .collect(Collectors.toMap(CxEmbryoLhTime::getNextStructureName, source -> source, (first,
                    second) -> first));
        candidates.stream().filter(sku -> structures.containsKey(sku.getStructureName())).forEach(sku -> {
            String key = skuKey(sku);
            context.getStructureSwitchCandidateSkuMap().putIfAbsent(key, sku);
            context.getStructureSwitchCandidateSourceMap().putIfAbsent(key, structures.get(sku.getStructureName()));
        });
    }

    /**
     * 判断候选是否归属主窗口的独立结构切换步骤。
     *
     * @param context 上下文
     * @param sku 当前候选
     * @return 是否归属主窗口结构切换步骤
     */
    public static boolean owns(LhScheduleContext context, SkuScheduleDTO sku) {
        return StructureSwitchSchedulingPolicy.isEnabled(context) && !context.isIsolatedNextShiftPlan()
                && Objects.nonNull(sku) && context.getStructureSwitchCandidateSourceMap().containsKey(skuKey(sku));
    }

    /**
     * 判断普通排产入口是否应将候选交给结构切换步骤，精确历史编排作用域优先承接。
     *
     * @param context 上下文
     * @param sku 当前候选
     * @return 普通入口是否应交给独立步骤
     */
    public static boolean shouldDefer(LhScheduleContext context, SkuScheduleDTO sku) {
        return owns(context, sku) && !context.isStructureSwitchScheduling()
                && !context.isPreviousAlternateScheduling();
    }

    /**
     * 查询仍可重试的结构切换指定关系，确定本窗口不可承接时不再排他保护原机。
     *
     * @param context 本批有效历史关系与真实剩余量
     * @param machineCode 候选运行侧或物理机台
     * @return 待评估或暂时等待的指定关系；已承接、无需求或本窗口不可承接时为空
     */
    public static LhMouldChangePlan resolvePendingHistoricalPlan(LhScheduleContext context, String machineCode) {
        if (!isEnabledForHistoricalProtection(context) || StringUtils.isEmpty(machineCode)) {
            return null;
        }
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return context.getEligiblePreviousAlternatePlans().stream()
                .filter(plan -> StringUtils.equals(physical,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode())))
                .filter(plan -> StringUtils.equals(plan.getLhMachineCode(), physical)
                        || StringUtils.equals(plan.getLhMachineCode(), machineCode))
                .filter(plan -> {
                    HistoricalStructureHandoffState state = HistoricalStructureHandoffPolicy.resolveState(context, plan);
                    // 未评估和暂时等待仍优先承接；有确定等待超限证据时解除原机排他保留。
                    return state == HistoricalStructureHandoffState.PENDING
                            || state == HistoricalStructureHandoffState.WAITING;
                })
                .sorted(PreviousAlternatePlanEligibilityService.planOrder())
                .findFirst().orElse(null);
    }

    /**
     * 判断候选是否会抢占尚未落实的指定后料机台，原在机前料及指定后料可以继续执行。
     *
     * @param context 已冻结历史关系的排程上下文
     * @param sku 当前拟排候选
     * @param machineCode 候选机台
     * @return 指定关系未完成且当前物料或状态不属于该关系时为 true
     */
    public static boolean isHistoricalMachineReservedForOtherSku(
            LhScheduleContext context, SkuScheduleDTO sku, String machineCode) {
        LhMouldChangePlan plan = resolvePendingHistoricalPlan(context, machineCode);
        if (Objects.isNull(plan) || Objects.isNull(sku)) {
            return false;
        }
        return !StringUtils.equals(plan.getBeforeMaterialCode(), sku.getMaterialCode())
                && !context.matchesPreviousAlternateAfterMaterial(
                        plan, sku.getMaterialCode(), sku.getProductStatus());
    }

    /**
     * 限定保护仅适用于主窗口内已冻结的结构切换来源。
     *
     * @param context 当前上下文
     * @return 主窗口启用结构切换时为 true
     */
    private static boolean isEnabledForHistoricalProtection(LhScheduleContext context) {
        return Objects.nonNull(context) && StructureSwitchSchedulingPolicy.isEnabled(context)
                && !context.isIsolatedNextShiftPlan();
    }
}
