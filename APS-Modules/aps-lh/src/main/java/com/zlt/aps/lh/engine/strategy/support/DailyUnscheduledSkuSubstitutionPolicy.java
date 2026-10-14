package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 当日未上机 SKU 置换的候选资格与物理机台保护规则。
 *
 * <p>只消费正常日候选和已提交事实，不重新授予日计划准入、扩机或提前生产资格。
 * 当日正量按物料和产品状态核对，机台保护按物理机台核对，避免单控另一侧绕过保护。</p>
 *
 * @author APS
 * @date 2026/10/10
 */
public final class DailyUnscheduledSkuSubstitutionPolicy {

    /**
     * 工具类禁止实例化。
     */
    private DailyUnscheduledSkuSubstitutionPolicy() {
    }

    /**
     * 冻结正常竞争后当日所有班次均无正计划量的候选，按业务键去重并保留原顺序。
     *
     * @param context 当前排程结果及业务状态
     * @param dayContext 当前业务日班次切片
     * @param normalCandidates 已通过当前日正常候选准入的待排集合
     * @return 当日未上机候选快照
     */
    public static List<DailyNewSpecCandidate> collectCandidates(
            LhScheduleContext context, DayScheduleContext dayContext,
            List<DailyNewSpecCandidate> normalCandidates) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(normalCandidates) || normalCandidates.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> scheduledSkuKeys = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> hasPositiveDayQty(dayContext, result))
                .map(result -> MonthPlanDateResolver.buildMaterialStatusKey(
                        result.getMaterialCode(), result.getProductStatus()))
                .collect(Collectors.toSet());
        Map<String, DailyNewSpecCandidate> uniqueCandidates = normalCandidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> Objects.nonNull(candidate.getSku()))
                .filter(candidate -> !scheduledSkuKeys.contains(skuKey(candidate.getSku())))
                .collect(Collectors.toMap(candidate -> skuKey(candidate.getSku()),
                        candidate -> candidate, (first, duplicate) -> first, LinkedHashMap::new));
        return new ArrayList<>(uniqueCandidates.values());
    }

    /**
     * 检查一条结果在当前业务日是否有任意班次正计划量。
     *
     * @param dayContext 当前业务日班次切片
     * @param result 待核对结果
     * @return 当前日任一班次有正量时返回 true
     */
    public static boolean hasPositiveDayQty(DayScheduleContext dayContext, LhScheduleResult result) {
        return Objects.nonNull(dayContext) && Objects.nonNull(result)
                && dayContext.getDayShifts().stream()
                .filter(Objects::nonNull)
                .filter(shift -> Objects.nonNull(shift.getShiftIndex()))
                .map(shift -> ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()))
                .anyMatch(quantity -> Objects.nonNull(quantity) && quantity > 0);
    }

    /**
     * 解析当前绑定所在整台物理机台不得置换的原因。
     *
     * <p>前日指定关系即使尚未成功承接也保护；结构切换和历史承接以活跃绑定及当前结果关联
     * 共同判定，不依赖通用新增类型或主侧标识。收集、选机及正式撤销前均使用同一判断。</p>
     *
     * @param context 历史资格与已提交结果
     * @param state 当前业务日绑定状态
     * @param binding 拟撤销的当日新增绑定
     * @return 保护原因；空字符串表示未命中本层保护
     */
    public static String resolveProtectionReason(
            LhScheduleContext context, DayDrivenScheduleState state, ActiveMachineBinding binding) {
        if (Objects.isNull(context) || Objects.isNull(state) || Objects.isNull(binding)) {
            return "置换绑定上下文不完整";
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(binding.getMachineCode());
        if (StringUtils.isEmpty(physicalMachineCode)) {
            return "置换绑定缺少物理机台编码";
        }
        boolean previousAlternateSpecified = context.getEligiblePreviousAlternatePlans().stream()
                .filter(Objects::nonNull)
                .anyMatch(plan -> samePhysicalMachine(physicalMachineCode, plan.getLhMachineCode()))
                || context.getPreviousAlternateReleaseEventMap().keySet().stream()
                .anyMatch(machineCode -> samePhysicalMachine(physicalMachineCode, machineCode));
        if (previousAlternateSpecified) {
            return "前日交替计划指定机台";
        }
        boolean structureSwitchBinding = Stream.concat(state.getActiveBindings().stream(),
                        context.getPreScheduledMachineBindingList().stream())
                .filter(Objects::nonNull)
                .filter(ActiveMachineBinding::isStructureSwitchBinding)
                .anyMatch(activeBinding -> samePhysicalMachine(physicalMachineCode, activeBinding.getMachineCode())
                        || samePhysicalMachine(physicalMachineCode, activeBinding.getPairMachineCode()));
        if (binding.isStructureSwitchBinding() || structureSwitchBinding
                || hasProtectedResult(context, context.getStructureSwitchResultPlanMap().keySet(),
                physicalMachineCode)) {
            return "结构切换结果机台";
        }
        if (hasProtectedResult(context, context.getPreviousAlternateResultPlanMap().keySet(),
                physicalMachineCode)) {
            return "前日交替已承接结果机台";
        }
        return "";
    }

    /**
     * 判断受保护来源中是否存在当前仍有效的同物理机台结果。
     *
     * @param context 当前排程结果
     * @param protectedResults 特殊来源结果集合
     * @param physicalMachineCode 候选物理机台
     * @return 存在有效受保护结果时返回 true
     */
    private static boolean hasProtectedResult(LhScheduleContext context,
                                               Set<LhScheduleResult> protectedResults,
                                               String physicalMachineCode) {
        return protectedResults.stream().filter(Objects::nonNull)
                .filter(context.getScheduleResultList()::contains)
                .anyMatch(result -> samePhysicalMachine(physicalMachineCode, result.getLhMachineCode()));
    }

    /**
     * 比较运行态机台是否归属指定物理机台。
     *
     * @param physicalMachineCode 候选物理机台
     * @param machineCode 待比较运行态机台
     * @return 同一物理机台时返回 true
     */
    private static boolean samePhysicalMachine(String physicalMachineCode, String machineCode) {
        return StringUtils.isNotEmpty(machineCode)
                && StringUtils.equals(physicalMachineCode,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
    }

    /**
     * 构造跨对象副本一致的 SKU 业务键。
     *
     * @param sku 当前候选 SKU
     * @return 物料与产品状态复合键
     */
    private static String skuKey(SkuScheduleDTO sku) {
        return MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
    }
}
