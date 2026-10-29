package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateActionPlan;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.service.impl.LhCleaningRepairOverlapPolicy;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 按来源在机物料和已提交结果统一解析清洗处置。
 * 候选查询无副作用；历史计划只阻止提前执行离机喷砂，不作为已完成换模撤销清洗。
 *
 * @author APS
 * @since 2026/10/15
 */
public final class CleaningDispositionResolver {

    /**
     * 创建工具类，禁止外部实例化。
     */
    private CleaningDispositionResolver() {
    }

    /**
     * 来源清洗事件的最终业务处置。
     */
    public enum Disposition {
        /**
         * 来源物料已收尾，取消清洗。
         */
        ENDING_CANCELLED,
        /**
         * 正规换模覆盖清洗，取消清洗。
         */
        MOULD_CHANGE_CANCELLED,
        /**
         * 独立执行清洗，包含合法的同SKU模具置换。
         */
        INDEPENDENT
    }

    /**
     * 同一清洗事件的处置和可追溯收尾结果。
     */
    public static final class Decision {
        /**
         * 清洗处置。
         */
        private final Disposition disposition;
        /**
         * 取消清洗对应的来源物料收尾结果。
         */
        private final LhScheduleResult endingResult;
        /**
         * 取消清洗的实际换模起点，原模具仍须保留该真实释放边界。
         */
        private final Date mouldChangeStartTime;

        /**
         * 固定本次查询使用的处置及来源结果。
         *
         * @param disposition 清洗处置
         * @param endingResult 收尾来源；非收尾处置为空
         * @param mouldChangeStartTime 实际换模起点；非换模取消时为空
         */
        private Decision(Disposition disposition, LhScheduleResult endingResult, Date mouldChangeStartTime) {
            this.disposition = disposition;
            this.endingResult = endingResult;
            this.mouldChangeStartTime = mouldChangeStartTime;
        }

        /**
         * 取得清洗处置。
         *
         * @return 本次来源事件的清洗处置
         */
        public Disposition getDisposition() {
            return this.disposition;
        }

        /**
         * 取得收尾取消清洗的证据。
         *
         * @return 来源SKU收尾结果，其他处置为空
         */
        public LhScheduleResult getEndingResult() {
            return this.endingResult;
        }

        /**
         * 取得取消清洗的实际换模起点。
         *
         * @return 原模具仍须遵守的换模释放边界；非换模取消时为空
         */
        public Date getMouldChangeStartTime() {
            return this.mouldChangeStartTime;
        }
    }

    /**
     * 从已提交事实判断清洗是否仍须执行；已兑现的干冰维修切换组合先于来源收尾取消。
     * 无替换模具的离机喷砂核实来源真实收尾后独立保留，其他来源继续使用收尾取消优先于正规换模取消的原规则。
     *
     * @param context 当前批次上下文
     * @param machineCode 清洗运行侧机台
     * @param window 来源清洗窗口
     * @return 本次清洗处置及收尾证据
     */
    public static Decision resolve(LhScheduleContext context, String machineCode,
                                   MachineCleaningWindowDTO window) {
        if (Objects.isNull(context) || Objects.isNull(window) || StringUtils.isEmpty(machineCode)) {
            return new Decision(Disposition.INDEPENDENT, null, null);
        }
        LhScheduleResult combinedSwitch = LhCleaningRepairOverlapPolicy.resolveCommittedSwitch(
                context, machineCode, window);
        if (combinedSwitch != null) {
            return "1".equals(combinedSwitch.getIsTypeBlock())
                    ? new Decision(Disposition.INDEPENDENT, null, null)
                    : new Decision(Disposition.MOULD_CHANGE_CANCELLED, null, combinedSwitch.getMouldChangeStartTime());
        }
        LhScheduleResult endingResult = resolveEndingResult(context, machineCode, window);
        if (Objects.nonNull(endingResult)) {
            return new Decision(Disposition.ENDING_CANCELLED, endingResult, null);
        }
        // 无替换模具的原模具已经离机，后料换模只占用机台，不能撤销其清洗等待、迁移身份及两端交替事实。
        if (window.isOffMachineCleaning() && MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                && StringUtils.isEmpty(window.getReplacementMouldCode())) {
            return new Decision(Disposition.INDEPENDENT, null, null);
        }
        Date changeStart = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> coversMachineSide(result.getLhMachineCode(), machineCode))
                .filter(result -> "1".equals(result.getIsChangeMould()) && !"1".equals(result.getIsTypeBlock()))
                .filter(result -> Objects.nonNull(result.getDailyPlanQty()) && result.getDailyPlanQty() > 0)
                .map(LhScheduleResult::getMouldChangeStartTime)
                .filter(start -> isRegularChangeOverlap(context, window, start))
                .min(Date::compareTo).orElse(null);
        return new Decision(Objects.nonNull(changeStart) ? Disposition.MOULD_CHANGE_CANCELLED : Disposition.INDEPENDENT,
                null, changeStart);
    }

    /**
     * 取得尚须执行的清洗窗口，供各排产策略共用，不删除候选或修改资源。
     *
     * @param context 当前批次上下文
     * @param machineCode 当前运行侧机台
     * @param windows 原始清洗窗口
     * @return 未被已提交收尾或正规换模取消的窗口
     */
    public static List<MachineCleaningWindowDTO> resolveEffectiveWindows(LhScheduleContext context,
            String machineCode, List<MachineCleaningWindowDTO> windows) {
        if (Objects.isNull(windows)) {
            return Collections.emptyList();
        }
        return windows.stream().filter(Objects::nonNull)
                .filter(window -> resolve(context, machineCode, window).getDisposition() == Disposition.INDEPENDENT)
                .collect(Collectors.toList());
    }

    /**
     * 解析本次喷砂接管的物理交接视图；无候选或已确认作用域时原样返回原窗口。
     * 换模起点不得晚于喷砂起点，保证冻结画像中节点以前的生产量与时间仍然真实。
     *
     * @param context 当前批次及联合尝试的精确节点
     * @param machineCode 当前运行侧机台
     * @param windows 原始或冻结清洗窗口
     * @param changeStart 本次候选正规换模节点；为空时仅认可本次已兑现的接管节点
     * @return 仍然阻挡物理交接的清洗窗口，不修改来源或取消清洗
     */
    public static List<MachineCleaningWindowDTO> resolvePhysicalReleaseWindows(LhScheduleContext context,
            String machineCode, List<MachineCleaningWindowDTO> windows, Date changeStart) {
        if (Objects.isNull(windows)) {
            return Collections.emptyList();
        }
        boolean candidate = Objects.nonNull(changeStart)
                && changeStart.equals(context.getTimedMachineOffCleaningCandidateMap().get(machineCode));
        Date confirmedStart = context.getTimedMachineOffCleaningConfirmedMap().get(machineCode);
        if (!candidate && Objects.isNull(confirmedStart)) {
            // 普通时间下机与余量收尾完全沿用原窗口，不在共享画像中扩大清洗取消口径。
            return windows;
        }
        return windows.stream().filter(window -> {
            if (candidate && isPendingSandBlastCovered(context, window, changeStart)) {
                return false;
            }
            // 正式阶段仅移除本次已兑现节点覆盖的喷砂，其他清洗仍按原画像参与交接。
            if (Objects.nonNull(confirmedStart) && isPendingSandBlastCovered(context, window, confirmedStart)) {
                Decision decision = resolve(context, machineCode, window);
                return decision.getDisposition() != Disposition.MOULD_CHANGE_CANCELLED
                        || !confirmedStart.equals(decision.getMouldChangeStartTime());
            }
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * 判断历史正规换模节点能否在喷砂开始前直接接管机台。
     *
     * @param context 本次换模时长配置
     * @param window 实际喷砂窗口
     * @param changeStart 精确历史换模节点
     * @return 节点不晚于喷砂开始且两窗口严格相交时为 true
     */
    public static boolean isPendingSandBlastCovered(LhScheduleContext context,
            MachineCleaningWindowDTO window, Date changeStart) {
        return Objects.nonNull(changeStart) && Objects.nonNull(window)
                && MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                && Objects.nonNull(window.getCleanStartTime())
                && !changeStart.after(window.getCleanStartTime())
                && isRegularChangeOverlap(context, window, changeStart);
    }

    /**
     * 判断正规换模的真实八小时窗口是否覆盖清洗，端点相接不算重叠。
     *
     * @param context 当前批次上下文
     * @param window 清洗窗口
     * @param changeStart 实际换模开始
     * @return 是否命中换模取消清洗
     */
    public static boolean isRegularChangeOverlap(LhScheduleContext context,
            MachineCleaningWindowDTO window, Date changeStart) {
        return Objects.nonNull(changeStart) && MachineCleaningOverlapUtil.isOverlap(window, changeStart,
                LhScheduleTimeUtil.addHours(changeStart, LhScheduleTimeUtil.getMouldChangeTotalHours(context)));
    }

    /**
     * 历史正规换模待落实时保留普通清洗候选，禁止抢先执行仅喷砂模具置换或离机。
     *
     * @param context 当前批次上下文
     * @param machineCode 清洗运行侧
     * @param window 实际清洗窗口
     * @return 是否存在覆盖清洗的待落实正规换模
     */
    public static boolean hasPendingRegularChange(LhScheduleContext context, String machineCode,
                                                  MachineCleaningWindowDTO window) {
        return context.getEligiblePreviousAlternatePlans().stream().filter(Objects::nonNull)
                .filter(plan -> coversMachineSide(plan.getLhMachineCode(), machineCode))
                .filter(plan -> MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode()))
                .anyMatch(plan -> isRegularChangeOverlap(context, window, resolveHistoricalStart(context, plan)));
    }

    /**
     * 判断已通过历史资格校验的实际正规换模是否与本机喷砂窗口重叠。
     * 该判断只保留释放及承接资格，不取消清洗，也不占用后料资源。
     *
     * @param context 本批实际机台和冻结动作
     * @param event 已冻结的有效历史交替事件
     * @return 实际正规换模与本侧喷砂严格相交时为 true
     */
    public static boolean isHistoricalRegularChangeOverlappingSandBlast(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(event.getMachineCode());
        if (Objects.isNull(machine) || Objects.isNull(event.getOfflineTime())
                || !MouldChangeTypeEnum.REGULAR.getCode().equals(
                        PreviousAlternateActionPlan.resolve(context, event).getChangeMouldType())) {
            return false;
        }
        return machine.getCleaningWindowList().stream()
                .filter(MachineCleaningOverlapUtil::isSandBlastCleaning)
                .anyMatch(window -> isRegularChangeOverlap(context, window, event.getOfflineTime()));
    }

    /**
     * 将历史计划日期与早中班配置合成实际候选起点，避免日期零点漏判早班重叠。
     *
     * @param context 当前班次配置
     * @param plan 已通过资格检查的历史交替
     * @return 历史计划班次起点；缺日期时为空
     */
    private static Date resolveHistoricalStart(LhScheduleContext context, LhMouldChangePlan plan) {
        if (Objects.isNull(plan.getPlanDate())) {
            return null;
        }
        if (ShiftEnum.MORNING_SHIFT.getCode().equals(plan.getClassIndex())) {
            return LhScheduleTimeUtil.buildTime(plan.getPlanDate(), LhScheduleTimeUtil.getMorningStartHour(context), 0, 0);
        }
        if (ShiftEnum.AFTERNOON_SHIFT.getCode().equals(plan.getClassIndex())) {
            return LhScheduleTimeUtil.buildTime(plan.getPlanDate(), LhScheduleTimeUtil.getAfternoonStartHour(context), 0, 0);
        }
        return plan.getPlanDate();
    }

    /**
     * 判断动作覆盖范围，整机动作覆盖两侧，单侧动作不得取消对侧清洗。
     *
     * @param actionMachine 动作运行侧或整机编码
     * @param cleaningMachine 清洗运行侧或整机编码
     * @return 动作是否覆盖该清洗侧
     */
    public static boolean coversMachineSide(String actionMachine, String cleaningMachine) {
        return StringUtils.isNotEmpty(actionMachine) && StringUtils.isNotEmpty(cleaningMachine)
                && (StringUtils.equals(actionMachine, cleaningMachine)
                || (!LhSingleControlMachineUtil.isSingleMouldMachine(actionMachine)
                && StringUtils.equals(actionMachine,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(cleaningMachine))));
    }

    /**
     * 用来源在机SKU的最后一段结果核对收尾，清洗前已经结束的结果也必须参加判断。
     *
     * @param context 已提交结果及初始在机快照
     * @param machineCode 清洗运行侧
     * @param window 来源清洗事件
     * @return 可撤销清洗的收尾结果；证据不足时为空
     */
    private static LhScheduleResult resolveEndingResult(LhScheduleContext context, String machineCode,
                                                       MachineCleaningWindowDTO window) {
        MachineScheduleDTO initial = context.getInitialMachineScheduleMap().get(machineCode);
        LhMachineOnlineInfo online = context.getMachineOnlineInfoMap().get(machineCode);
        String materialCode = window.getSourceMaterialCode();
        String productStatus = window.getSourceProductStatus();
        if (window.isSourceFollowsMachineOccupation()) {
            LhScheduleResult occupying = context.getScheduleResultList().stream().filter(Objects::nonNull)
                    .filter(result -> coversMachineSide(result.getLhMachineCode(), machineCode))
                    .filter(result -> result.getSpecEndTime() != null && window.getCleanStartTime() != null
                            && result.getSpecEndTime().after(window.getCleanStartTime()))
                    .filter(result -> {
                        int first = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
                        Date start = first > 0 ? ShiftFieldUtil.getShiftStartTime(result, first) : null;
                        return start != null && !start.after(window.getCleanStartTime());
                    }).max(Comparator.comparing(result -> ShiftFieldUtil.getShiftStartTime(result,
                            ShiftFieldUtil.resolveFirstPlannedShiftIndex(result)))).orElse(null);
            return occupying != null && CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(occupying)
                    ? occupying : null;
        }
        if (StringUtils.isEmpty(materialCode)) {
            materialCode = window.getContinuationMaterialCode();
            productStatus = window.getContinuationProductStatus();
        }
        if (StringUtils.isEmpty(materialCode) && Objects.nonNull(online)) {
            materialCode = online.getMaterialCode();
            productStatus = online.getProductStatus();
        }
        if (StringUtils.isEmpty(materialCode) && Objects.nonNull(initial)) {
            materialCode = initial.getCurrentMaterialCode();
        }
        if (StringUtils.isEmpty(materialCode)) {
            return null;
        }
        String sourceKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, normalizeStatus(productStatus));
        List<LhScheduleResult> sourceResults = context.getScheduleResultList().stream().filter(Objects::nonNull)
                .filter(result -> coversMachineSide(result.getLhMachineCode(), machineCode))
                .filter(result -> sourceKey.equals(MonthPlanDateResolver.buildMaterialStatusKey(
                        result.getMaterialCode(), normalizeStatus(result.getProductStatus()))))
                .filter(result -> Objects.nonNull(result.getDailyPlanQty()) && result.getDailyPlanQty() > 0)
                .filter(result -> Objects.nonNull(result.getSpecEndTime()))
                .collect(Collectors.toList());
        LhScheduleResult latest = sourceResults.stream()
                .max(Comparator.comparing(LhScheduleResult::getSpecEndTime)).orElse(null);
        if (Objects.isNull(latest)) {
            return null;
        }
        if (!window.isOffMachineCleaning()) {
            return CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(latest) ? latest : null;
        }
        // 离机喷砂强制截断不等于SKU收尾；只有真实余量已排完且未到触发时刻才撤销。
        int originalSurplus = sourceResults.stream().map(LhScheduleResult::getMouldSurplusQty)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        int scheduledQty = sourceResults.stream().mapToInt(LhScheduleResult::getDailyPlanQty).sum();
        return originalSurplus > 0 && scheduledQty >= originalSurplus
                && Objects.nonNull(window.getCleanStartTime())
                && !latest.getSpecEndTime().after(window.getCleanStartTime()) ? latest : null;
    }

    /**
     * 将来源空产品状态规范为正规状态，与现有清洗规则保持一致。
     *
     * @param status 来源产品状态
     * @return 用于精确匹配SKU的产品状态
     */
    private static String normalizeStatus(String status) {
        return StringUtils.defaultIfBlank(StringUtils.trimToEmpty(status), "S");
    }
}
