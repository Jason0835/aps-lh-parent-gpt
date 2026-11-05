package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 结构收尾续作的班次边界及统一阶段归属，不修改需求额度和资源。
 *
 * @author Codex
 * @date 2026/10/29
 */
public final class StructureEndingContinuationPolicy {
    /**
     * 中班生产截止小时，沿用需求明确的十九点控制点。
     */
    private static final int AFTERNOON_FINISH_HOUR = 19;

    /**
     * 工具类不允许实例化。
     */
    private StructureEndingContinuationPolicy() {
    }

    /**
     * 解析主窗口内已经接管的唯一需求状态，班次九保持独立规则。
     *
     * @param context 本轮上下文
     * @param sku 当前来源或同账本副本
     * @return 已冻结的状态；未接管或独立班次九返回空
     */
    public static StructureEndingContinuationState state(LhScheduleContext context, SkuScheduleDTO sku) {
        return context == null || sku == null || context.isIsolatedNextShiftPlan()
                || SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getCode().equals(sku.getSourceType()) ? null
                : context.getStructureEndingContinuationStates().get(
                        MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus()));
    }

    /**
     * 判断普通入口是否需要让出已经接管的候选。
     *
     * @param context 当前阶段
     * @param sku 待排需求
     * @return 已接管且当前不在专用步骤时为真
     */
    public static boolean shouldDefer(LhScheduleContext context, SkuScheduleDTO sku) {
        return state(context, sku) != null && !context.isStructureEndingContinuationScheduling();
    }

    /**
     * 为已接管结果提供严格数量上限，保持原收尾身份，不因结构命中改变isEnd语义。
     *
     * @param context 当前主窗口及接管身份
     * @param sku 当前来源
     * @param ending 原规则确定的实际收尾身份
     * @return 禁止补满超量的本次数量策略；未接管时保持原策略
     */
    public static ProductionQuantityPolicy quantityPolicy(LhScheduleContext context, SkuScheduleDTO sku,
            boolean ending) {
        ProductionQuantityPolicy policy = ProductionQuantityPolicy.from(sku, ending);
        if (state(context, sku) != null) {
            policy.setStrictUpperLimit(true);
            policy.setAllowFillStartedShift(false);
            policy.setFullRunForNonTailMachine(false);
        }
        return policy;
    }

    /**
     * 判断正式实机结果是否由本规则统一控量，虚拟及班次九独立规则不受影响。
     *
     * @param context 当前上下文
     * @param result 待后处理的结果
     * @return 属于本规则正式实机结果时为真
     */
    public static boolean ownsResult(LhScheduleContext context, LhScheduleResult result) {
        if (context != null && result != null && context.getScheduleResultSourceSkuMap().containsKey(result)
                && state(context, context.getScheduleResultSourceSkuMap().get(result)) == null) {
            return false;
        }
        return context != null && result != null && !context.isIsolatedNextShiftPlan()
                && !LhSingleControlMachineUtil.isVirtualMachineCode(result.getLhMachineCode())
                && context.getStructureEndingContinuationStates().containsKey(
                        MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result
                                .getProductStatus()));
    }

    /**
     * 按计划身份统一路由历史主入口、扩展和预演，避免迁移后的关系提前提交。
     *
     * @param context 当前阶段
     * @param plan 原始有效历史关系
     * @return 当前阶段允许执行该关系时为真
     */
    public static boolean acceptsHistoricalPlan(LhScheduleContext context, LhMouldChangePlan plan) {
        if (context.isIsolatedNextShiftPlan() || plan == null) {
            return true;
        }
        StructureEndingContinuationState state = historicalState(context, plan);
        boolean owned = state != null && state.getHistoricalPlanIds().contains(plan.getId());
        return context.isStructureEndingContinuationScheduling() ? owned && !state.isCompleted() : !owned;
    }

    /**
     * 查询本规则尚未执行的指定关系，迁移阶段期间保留原指令的有限竞争保护。
     *
     * @param context 有效历史计划及三步进度
     * @param machineCode 当前候选机台
     * @return 未兑现指定计划；无待执行关系时为空
     */
    public static LhMouldChangePlan pendingHistoricalPlan(LhScheduleContext context, String machineCode) {
        if (context == null || context.isIsolatedNextShiftPlan() || machineCode == null) {
            return null;
        }
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return context.getStructureEndingHistoricalMachinePlans().getOrDefault(physical, Collections
                .emptyList()).stream()
                .filter(plan -> {
                    StructureEndingContinuationState state = historicalState(context, plan);
                    return state != null && !state.isCompleted()
                            && !state.getCommittedHistoricalPlanIds().contains(plan.getId());
                }).findFirst().orElse(null);
    }

    /**
     * 按已冻结身份一次建立历史机台索引，新增和结构切换的候选循环仅做键查询。
     *
     * @param context 已冻结资格或刚完成回滚的上下文
     */
    public static void rebuildHistoricalIndex(LhScheduleContext context) {
        context.setStructureEndingHistoricalMachinePlans(context.getEligiblePreviousAlternatePlans().stream()
                .filter(plan -> {
                    StructureEndingContinuationState state = historicalState(context, plan);
                    return state != null && state.getHistoricalPlanIds().contains(plan.getId());
                }).collect(Collectors.groupingBy(plan -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        plan.getLhMachineCode()), LinkedHashMap::new, Collectors.toList())));
    }

    /**
     * 记录正量历史承接，不改变资格或机台需求，候选失败随运行态快照撤销。
     *
     * @param context 当前提交上下文
     * @param plan 已兑现历史关系
     * @param result 对应正量结果
     */
    public static void recordHistoricalCommit(LhScheduleContext context, LhMouldChangePlan plan,
            LhScheduleResult result) {
        StructureEndingContinuationState state = historicalState(context, plan);
        if (state != null && state.getHistoricalPlanIds().contains(plan.getId())
                && ShiftFieldUtil.resolveScheduledQty(result) > 0) {
            state.getCommittedHistoricalPlanIds().add(plan.getId());
        }
    }

    /**
     * 按历史后料的物料状态定位原冻结身份，不在机台热循环扫描全部SKU状态。
     *
     * @param context 当前上下文
     * @param plan 原历史关系
     * @return 后料冻结身份；没有接管时为空
     */
    private static StructureEndingContinuationState historicalState(LhScheduleContext context, LhMouldChangePlan plan) {
        return plan == null ? null : context.getStructureEndingContinuationStates().get(
                MonthPlanDateResolver.buildMaterialStatusKey(plan.getAfterMaterialCode(), plan.getProductStatus()));
    }

    /**
     * 新增原机隔离和物理去重门禁，历史与普通资格不能互相扩散。
     *
     * @param context 当前步骤及历史作用域
     * @param sku 当前需求
     * @param machineCode 候选运行侧或物理机台
     * @return 当前组合具备本次新增机会时为真
     */
    public static boolean allowsNewMachine(LhScheduleContext context, SkuScheduleDTO sku, String machineCode) {
        StructureEndingContinuationState state = state(context, sku);
        if (state == null) {
            return !context.isStructureEndingContinuationScheduling();
        }
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        boolean completingHistoricalSide = context.getActivePreviousAlternateEvent() != null
                && !state.getAddedRuntimeMachines().contains(machineCode)
                && PreviousAlternateTrialSideAdmissionPolicy.canCompleteSpecifiedSide(context, sku);
        if (!context.isStructureEndingContinuationScheduling() || state.isCompleted()
                || state.getOriginalPhysicalMachines().contains(physical)
                || (state.getAddedPhysicalMachines().contains(physical) && !completingHistoricalSide)) {
            return false;
        }
        LhMouldChangePlan historicalPlan = PreviousAlternateStructureLimitExemptionPolicy.resolvePlan(context,
                sku, machineCode);
        if (historicalPlan != null) {
            return state.getHistoricalPlanIds().contains(historicalPlan.getId());
        }
        if (state.getFirstExpansionDate() == null || context.getCurrentScheduleDate() == null) {
            return false;
        }
        LocalDate productionDate = context.getCurrentScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (productionDate.isBefore(state.getFirstExpansionDate())) {
            return false;
        }
        return state.isDeferredExpansion()
                ? SkuEndingDeferredExpansionPolicy.isFirstDayAllowed(context, sku, productionDate)
                    && SkuEndingDeferredExpansionPolicy.evaluate(context, sku, productionDate).needsExpansion()
                : state.getAddedPhysicalMachines().size() < state.getOrdinaryAdditionalByDate().getOrDefault(
                        productionDate, 0);
    }

    /**
     * 校验正式结果全部正量班次受生产截止约束，超界由外层完整快照回滚。
     *
     * @param context 单组合提交后的结果与原资格
     * @return 所有接管结果均未越过截止时为真
     */
    public static boolean withinDeadlines(LhScheduleContext context) {
        if (context.isIsolatedNextShiftPlan() || context.getStructureEndingContinuationStates().isEmpty()) {
            return true;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!ownsResult(context, result)) {
                continue;
            }
            StructureEndingContinuationState state = context.getStructureEndingContinuationStates().get(
                    MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus()));
            if (state == null) {
                continue;
            }
            FirstInspectionAllocationPlan inspection = context.getFirstInspectionResultPlanMap().get(result);
            if (inspection != null && inspection.getInspectionEndTime() != null
                    && inspection.getInspectionEndTime().after(state.getFinalDeadline())) {
                return false;
            }
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (quantity == null || quantity <= 0) {
                    continue;
                }
                Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                Date end = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
                if (start == null || end == null || !start.before(state.getFinalDeadline())
                        || end.after(state.getFinalDeadline())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 仅在原子提交成功后登记新增物理机台，历史与普通来源共用同一去重集合。
     *
     * @param context 已通过数量和资源校验的上下文
     * @param firstResult 本次提交前的结果条数
     */
    public static void recordCommittedMachines(LhScheduleContext context, int firstResult) {
        if (!context.isStructureEndingContinuationScheduling()) {
            return;
        }
        for (int index = firstResult; index < context.getScheduleResultList().size(); index++) {
            LhScheduleResult result = context.getScheduleResultList().get(index);
            StructureEndingContinuationState state = context.getStructureEndingContinuationStates().get(
                    MonthPlanDateResolver.buildMaterialStatusKey(result.getMaterialCode(), result.getProductStatus()));
            if (state == null || ShiftFieldUtil.resolveScheduledQty(result) <= 0) {
                continue;
            }
            String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
            if (!state.getOriginalPhysicalMachines().contains(physical)) {
                state.getAddedPhysicalMachines().add(physical);
                state.getAddedRuntimeMachines().add(result.getLhMachineCode());
            }
        }
    }

    /**
     * 优先排除转产，再按物料后结构反查全部前结构并取最晚收尾时间。
     *
     * @param context 有效成型批次的前后结构关系及收尾快照
     * @param structureName 物料结构，与NEXT_STRUCTURE_NAME精确匹配
     * @return 全部匹配前结构的最晚ENDING_TIME；转产、无关系或无有效时间时返回空
     */
    public static Date resolveEndingTime(LhScheduleContext context, String structureName) {
        if (context == null || StringUtils.isEmpty(structureName)
                || StructureTransferExemptionPolicy.isTransferStructure(context, structureName)) {
            return null;
        }
        // 先取全部前结构的最晚时间，再判断窗口，不能用窗口内较早记录替代真实最晚收尾。
        return context.getStructureContinuationPreviousStructureMap()
                .getOrDefault(structureName, Collections.emptySet()).stream()
                .map(previous -> context.getStructureContinuationEndingTimeMap().get(previous))
                .filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
    }

    /**
     * 在正式T至T+2排程窗口内定位收尾班次，沿用半开区间及夜班业务日归属。
     *
     * @param context 正式窗口和班次配置
     * @param endingTime 前结构最晚收尾时刻
     * @return 命中的正式班次；缺失或窗口外返回空
     */
    public static LhShiftConfigVO resolveEndingShift(LhScheduleContext context, Date endingTime) {
        if (context == null || endingTime == null || context.getScheduleDate() == null) {
            return null;
        }
        return LhScheduleTimeUtil.resolveShiftByTime(
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate()), endingTime);
    }

    /**
     * 解析班次生产截止，中班取班末与当日十九点中的较早者。
     *
     * @param shift 已定位的结构收尾班次
     * @return 第一步生产截止
     */
    public static Date firstDeadline(LhShiftConfigVO shift) {
        if (!shift.isAfternoonShift()) {
            return shift.getShiftEndDateTime();
        }
        Date afternoon = LhScheduleTimeUtil.buildTime(shift.getWorkDate(), AFTERNOON_FINISH_HOUR, 0, 0);
        return afternoon.before(shift.getShiftEndDateTime()) ? afternoon : shift.getShiftEndDateTime();
    }

    /**
     * 打开单组合生产截止，首检、真实产能和提交读取同一有效边界。
     *
     * @param context 本轮上下文
     * @param sku 当前唯一生产需求
     * @return 必须在退出时关闭的嵌套作用域
     */
    public static DeadlineScope openDeadline(LhScheduleContext context, SkuScheduleDTO sku) {
        return new DeadlineScope(context, state(context, sku));
    }

    /**
     * 单组合截止作用域保留迁移前的结构切换身份，不修改正式班次配置，退出时恢复外层状态。
     */
    public static final class DeadlineScope implements AutoCloseable {
        /**
         * 当前排程上下文。
         */
        private final LhScheduleContext context;
        /**
         * 外层组合的原截止。
         */
        private final Date previous;
        /**
         * 外层结构切换身份，迁移的候选仍执行原切换额度和首检规则。
         */
        private final boolean previousStructureSwitch;

        /**
         * 安装当前需求的截止，非接管候选清除外层组合边界。
         *
         * @param context 当前上下文
         * @param state 当前需求状态
         */
        private DeadlineScope(LhScheduleContext context, StructureEndingContinuationState state) {
            this.context = context;
            this.previous = context.getActiveStructureEndingProductionDeadline();
            this.previousStructureSwitch = context.isStructureSwitchScheduling();
            if (state != null && StructureSwitchCandidateScope.owns(context, state.getSourceSku())) {
                context.setStructureSwitchScheduling(true);
            }
            context.setActiveStructureEndingProductionDeadline(state == null ? null : state.getFinalDeadline());
        }

        /**
         * 恢复进入作用域前的截止，避免污染其他需求。
         */
        @Override
        public void close() {
            context.setActiveStructureEndingProductionDeadline(previous);
            context.setStructureSwitchScheduling(previousStructureSwitch);
        }
    }
}
