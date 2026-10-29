package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.ContinuationRemainderFinishGroup;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingMachineProfile;
import com.zlt.aps.lh.engine.strategy.support.ContinuationFinishScheduleResult;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineFinishPlan;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.ShiftProductionControlUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 收尾优先规则，完整收尾时跳过对应05，候选和续作均以独立容量视图证明。
 * 原始05来源和06故障保持不变，作用域内只发布独立执行视图，不扣减任何数量账本。
 *
 * @author APS
 * @date 2026/10/23
 */
public final class LhRepairEndingDeferralPolicy {

    /**
     * 禁止创建无状态规则实例。
     */
    private LhRepairEndingDeferralPolicy() {
    }

    /**
     * 为一个机台候选建立独立维修跳过视图，失败、异常和预演退出均恢复外层事实。
     *
     * @param context 当前排程上下文
     * @param sku 当前收尾物料
     * @param machineCode 本次候选运行侧
     * @param notBefore 本次允许生产的班次下限
     * @param ending 当前主链已经确认的收尾身份
     * @return 自动恢复外层维修视图的作用域
     */
    public static Scope openScope(LhScheduleContext context, SkuScheduleDTO sku, String machineCode,
            Date notBefore, boolean ending) {
        List<MdmDevicePlanShut> plans = context.getDevicePlanShutList();
        List<MdmDevicePlanShut> projected = plans;
        if (ending && sku != null && sku.getSurplusQty() > 0) {
            projected = projectCandidate(context, sku, machineCode, notBefore, plans);
        }
        return new Scope(context, projected == plans
                ? context.getRepairPlanRuntimeState().getScopedStopPlans() : projected);
    }

    /**
     * 用已提交的完整收尾结果排除对应05，供后续SKU、容量校正及保存阶段共用。
     *
     * @param context 已提交结果及其来源身份
     * @param plans 原始实际维修列表
     * @return 无跳过时复用原列表，有完整收尾时返回排除对应05的独立列表
     */
    public static List<MdmDevicePlanShut> resolveCommittedPlans(LhScheduleContext context,
            List<MdmDevicePlanShut> plans) {
        if (plans == null || plans.isEmpty() || context.getScheduleResultList().isEmpty()) {
            return plans;
        }
        RepairPlanRuntimeState state = context.getRepairPlanRuntimeState();
        List<Object> key = committedKey(context, plans);
        if (state.getCommittedStopSource() == plans && key.equals(state.getCommittedStopKey())) {
            return state.getCommittedStopView();
        }
        List<MdmDevicePlanShut> projected = new ArrayList<>(plans.size());
        boolean changed = false;
        for (MdmDevicePlanShut plan : plans) {
            if (resolveCommittedEnding(context, plan) == null) {
                projected.add(plan);
            } else {
                changed = true;
            }
        }
        List<MdmDevicePlanShut> view = changed ? projected : plans;
        boolean sourceChanged = state.getCommittedStopSource() != plans || state.getCommittedStopKey() == null
                || !key.get(0).equals(state.getCommittedStopKey().get(0));
        List<MdmDevicePlanShut> previous = state.getCommittedStopView();
        if (!sourceChanged && sameStopTimes(previous, view)) {
            view = previous;
        }
        state.cacheCommittedStopView(plans, key, view);
        if (sourceChanged || previous != view) {
            state.constraintsChanged();
        }
        return view;
    }

    /**
     * 一次轻量扫描提取维修相关整机的数量与时间事实，不为每条维修反复扫描全量结果。
     * 快照只含不可变值，因此同数量行数下的就地改量、换料、目标变化及结果回滚也会失效。
     *
     * @param context 当前已提交结果
     * @param plans 当前实际停机列表
     * @return 来源、整机存在性及相关结果的结构化事实键
     */
    private static List<Object> committedKey(LhScheduleContext context, List<MdmDevicePlanShut> plans) {
        List<Object> sources = new ArrayList<>(plans.size());
        Set<String> machines = new LinkedHashSet<>();
        for (MdmDevicePlanShut plan : plans) {
            sources.add(plan == null ? null : Arrays.asList(plan.getId(), plan.getMachineCode(),
                    plan.getMachineStopType(), millis(plan.getBeginDate()), millis(plan.getEndDate())));
            if (isRepair(plan)) {
                String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getMachineCode());
                machines.add(physical);
                machines.add(physical + "L");
                machines.add(physical + "R");
            }
        }
        List<Object> sides = machines.stream().map(context.getMachineScheduleMap()::containsKey)
                .collect(Collectors.toList());
        List<Object> results = new ArrayList<>();
        Set<String> occupyingSkus = new HashSet<>();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (result == null || !machines.contains(result.getLhMachineCode())) {
                continue;
            }
            int first = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
            Date start = first > 0 ? ShiftFieldUtil.getShiftStartTime(result, first) : null;
            results.add(Arrays.asList(result.getLhMachineCode(), result.getMaterialCode(), result.getProductStatus(),
                    result.getMouldSurplusQty(), ShiftFieldUtil.resolveScheduledQty(result), millis(start),
                    millis(result.getSpecEndTime()), result.getIsEmbryoEnding()));
            if (start != null && result.getSpecEndTime() != null && plans.stream().anyMatch(plan ->
                    isRepair(plan) && samePhysical(plan.getMachineCode(), result.getLhMachineCode())
                            && start.before(plan.getBeginDate()) && result.getSpecEndTime().after(plan.getBeginDate()))) {
                occupyingSkus.add(MonthPlanDateResolver.buildMaterialStatusKey(
                        result.getMaterialCode(), result.getProductStatus()));
            }
        }
        // 整组完成依赖同SKU其他机台的数量；无关SKU不参与指纹，避免每次读维修视图都反射全厂八班。
        List<Object> peers = context.getScheduleResultList().stream().filter(Objects::nonNull)
                .filter(result -> !machines.contains(result.getLhMachineCode()))
                .filter(result -> occupyingSkus.contains(MonthPlanDateResolver.buildMaterialStatusKey(
                        result.getMaterialCode(), result.getProductStatus())))
                .map(result -> Arrays.asList(result.getMaterialCode(), result.getProductStatus(),
                        ShiftFieldUtil.resolveScheduledQty(result))).collect(Collectors.toList());
        List<Object> targets = context.getContinuationRemainderFinishGroups().stream()
                .map(group -> Arrays.asList(group.getSourceSku().getMaterialCode(),
                        group.getSourceSku().getProductStatus(), group.getFinalTargetQty(),
                        group.getOriginalMachineFinishPlan() == null ? null
                                : group.getOriginalMachineFinishPlan().getRemainingQty()))
                .collect(Collectors.toList());
        return Arrays.asList(sources, sides, results, peers, targets,
                new java.util.HashMap<>(context.getSharedEmbryoStockEndingInitialQuotaMap()));
    }

    /**
     * 比较派生维修视图的实际约束，相关结果变化但时刻未变时继续复用容量输入。
     *
     * @param first 上一次视图
     * @param second 本次计算视图
     * @return 来源及实际起止一致时返回true
     */
    private static boolean sameStopTimes(List<MdmDevicePlanShut> first, List<MdmDevicePlanShut> second) {
        if (first == null || first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            MdmDevicePlanShut previous = first.get(index);
            MdmDevicePlanShut current = second.get(index);
            if (previous != current && (previous == null || current == null
                    || !Objects.equals(previous.getId(), current.getId())
                    || !Objects.equals(millis(previous.getBeginDate()), millis(current.getBeginDate()))
                    || !Objects.equals(millis(previous.getEndDate()), millis(current.getEndDate())))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将可变日期转为事实键使用的不可变毫秒值。
     *
     * @param date 可空业务时间
     * @return 时间毫秒或空值
     */
    private static Long millis(Date date) {
        return date == null ? null : date.getTime();
    }

    /**
     * 为续作收尾组打开扣05前的独立试算，其他设备约束保持原位。
     * 正式提交前必须按真实分配恢复未能证明完整收尾的维修来源。
     *
     * @param context 本轮上下文
     * @param group 已冻结目标和原机身份的收尾组
     * @return 自动恢复外层视图的试算作用域
     */
    public static Scope openContinuationScope(LhScheduleContext context, ContinuationRemainderFinishGroup group) {
        List<MdmDevicePlanShut> original = context.getDevicePlanShutList();
        Set<String> machines = group.getParticipatingResults().stream()
                .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                .collect(Collectors.toSet());
        List<MdmDevicePlanShut> projected = original.stream()
                .filter(plan -> !isRepair(plan) || !machines.contains(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getMachineCode())))
                .collect(Collectors.toList());
        return new Scope(context, projected, original);
    }

    /**
     * 将不执行05的原因绑定到直接承接后料的首个有量班次，并记录来源证据。
     *
     * @param context 已完成最终数量及时间校正的上下文
     */
    public static void appendCommittedAnalysis(LhScheduleContext context) {
        List<MdmDevicePlanShut> actualPlans = context.getDevicePlanShutList();
        for (MdmDevicePlanShut source : context.getPlannedRepairSourcePlanMap().values()) {
            MdmDevicePlanShut repair = LhPlannedRepairResolver.buildActualRepairPlan(context, source);
            Date finish = repair == null ? null : resolveCommittedEnding(context, repair);
            if (finish == null || actualPlans.stream().anyMatch(plan -> Objects.equals(plan.getId(), source.getId()))) {
                continue;
            }
            Date nextSwitch = context.getScheduleResultList().stream()
                    .filter(result -> samePhysical(source.getMachineCode(), result.getLhMachineCode()))
                    .map(LhScheduleResult::getMouldChangeStartTime).filter(Objects::nonNull)
                    .filter(start -> !start.before(finish)).min(Date::compareTo).orElse(null);
            context.getScheduleResultList().stream()
                    .filter(result -> samePhysical(source.getMachineCode(), result.getLhMachineCode()))
                    .filter(result -> nextSwitch != null && nextSwitch.equals(result.getMouldChangeStartTime()))
                    .filter(result -> ShiftFieldUtil.resolveFirstPlannedShiftIndex(result) > 0)
                    .forEach(result -> {
                        int first = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
                        ShiftFieldUtil.appendShiftAnalysis(result, first, "收尾+计划性维修");
                        if ("1".equals(result.getIsChangeMould()) && !"1".equals(result.getIsTypeBlock())) {
                            ShiftFieldUtil.appendShiftAnalysis(result, first, "换模");
                        }
                    });
            PriorityTraceLogHelper.appendProcessLog(context, "收尾跳过计划性维修",
                    "工厂=" + context.getFactoryCode() + ", 批次=" + context.getBatchNo()
                            + ", 来源=" + source.getId() + ", 物理机台=" + source.getMachineCode()
                            + ", 原实际开始=" + LhScheduleTimeUtil.formatDateTime(repair.getBeginDate())
                            + ", 实际收尾=" + LhScheduleTimeUtil.formatDateTime(finish)
                            + ", 处理=05不执行且不追加预热，后料按实际切换首检");
        }
    }

    /**
     * 在维修本体发生前已完成准备的收尾候选，按整机全部侧位的真实净容量验证完整目标。
     * 只使用本轮已加载的合法班次；没有完整容量证据时继续保留原维修。
     *
     * @param context 已加载资源与班次
     * @param sku 当前收尾物料
     * @param machineCode 运行侧
     * @param notBefore 生产下限
     * @param plans 当前维修视图
     * @return 候选持有的独立维修执行视图
     */
    private static List<MdmDevicePlanShut> projectCandidate(LhScheduleContext context, SkuScheduleDTO sku,
            String machineCode, Date notBefore, List<MdmDevicePlanShut> plans) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (machine == null || context.getScheduleWindowShifts().isEmpty()) {
            return plans;
        }
        Integer ledgerRemaining = context.getSkuProductionRemainingQtyMap().get(
                MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus()));
        Integer embryoQuota = context.getSharedEmbryoStockEndingInitialQuotaMap().get(
                MonthPlanDateResolver.buildMaterialStatusKey(
                        sku.getMaterialCode(), sku.getProductStatus()));
        int remaining;
        if (embryoQuota != null) {
            remaining = Math.max(0, embryoQuota);
        } else {
            remaining = ledgerRemaining == null ? sku.getSurplusQty() : Math.max(0, ledgerRemaining);
        }
        if (remaining <= 0 || sku.getEmbryoStock() < remaining) {
            return plans;
        }
        List<MdmDevicePlanShut> projected = new ArrayList<>(plans.size());
        boolean changed = false;
        for (MdmDevicePlanShut plan : plans) {
            Date finish = null;
            if (isRepair(plan) && samePhysical(machineCode, plan.getMachineCode())) {
                Date start = resolveProductionStart(context, sku, machine, notBefore);
                // 真正与当前切换重叠的维修继续沿用组合规则，不能借后料收尾身份取消本体禁产。
                if (start != null && start.before(plan.getBeginDate())) {
                    finish = previewEnding(context, sku, machine, start, plan, plans, remaining);
                }
            }
            if (finish != null && finish.after(plan.getBeginDate())) {
                changed = true;
            } else {
                projected.add(plan);
            }
        }
        return changed ? projected : plans;
    }

    /**
     * 以现有生产或最早合法切换完成作为预演起点，生产门禁只允许向后收敛。
     *
     * @param context 当前班次及机台状态
     * @param sku 当前SKU
     * @param machine 候选机台
     * @param notBefore 当前业务日下限
     * @return 保守的可生产起点，无合法准备班次时为空
     */
    private static Date resolveProductionStart(LhScheduleContext context, SkuScheduleDTO sku,
            MachineScheduleDTO machine, Date notBefore) {
        boolean continuing = context.getScheduleResultList().stream().anyMatch(result ->
                StringUtils.equals(machine.getMachineCode(), result.getLhMachineCode())
                        && ShiftFieldUtil.resolveScheduledQty(result) > 0
                        && StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()));
        Date start = notBefore == null ? context.getScheduleWindowShifts().get(0).getShiftStartDateTime() : notBefore;
        if (!continuing) {
            Date available = machine.getEstimatedEndTime();
            if (available != null && available.after(start)) {
                start = available;
            }
            Date ready = start;
            LhShiftConfigVO preparation = context.getScheduleWindowShifts().stream()
                    .filter(shift -> shift.getShiftEndDateTime().after(ready))
                    .filter(shift -> !LhScheduleTimeUtil.isNoMouldChangeTime(context,
                            shift.getShiftStartDateTime())).findFirst().orElse(null);
            if (preparation == null) {
                return null;
            }
            if (start.before(preparation.getShiftStartDateTime())) {
                start = preparation.getShiftStartDateTime();
            }
            start = LhScheduleTimeUtil.addHours(start, LhScheduleTimeUtil.getMouldChangeTotalHours(context));
        }
        Date embryoReady = com.zlt.aps.lh.engine.strategy.support.NewSpecEmbryoAvailableTimeResolver
                .resolveProductionNotBeforeTime(context, sku, context.getScheduleWindowShifts());
        return embryoReady != null && embryoReady.after(start) ? embryoReady : start;
    }

    /**
     * 汇总同一物理机台配对侧的净容量，保留其他停机、清洗、精度、胶囊和生产管控。
     *
     * @param context 当前批次
     * @param sku 当前收尾SKU
     * @param machine 当前运行侧
     * @param start 保守的可生产起点
     * @param repair 本次唯一允许跳过的维修
     * @param plans 当前设备停机视图
     * @param remaining 本轮中心账本或冻结份额的真实目标
     * @return 完整余量可在维修当周完成时的保守完成点，否则为空
     */
    private static Date previewEnding(LhScheduleContext context, SkuScheduleDTO sku, MachineScheduleDTO machine,
            Date start, MdmDevicePlanShut repair, List<MdmDevicePlanShut> plans, int remaining) {
        List<MachineScheduleDTO> sides = new ArrayList<>();
        sides.add(machine);
        String pairCode = LhSingleControlMachineUtil.resolvePairMachineCode(machine.getMachineCode());
        MachineScheduleDTO pair = context.getMachineScheduleMap().get(pairCode);
        if (pair != null) {
            sides.add(pair);
        }
        List<MdmDevicePlanShut> otherStops = plans.stream()
                .filter(plan -> !Objects.equals(plan.getId(), repair.getId())).collect(Collectors.toList());
        int left = remaining;
        Date weekEnd = weekEnd(repair.getBeginDate());
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            Date from = start.after(shift.getShiftStartDateTime()) ? start : shift.getShiftStartDateTime();
            Date until = shift.getShiftEndDateTime().before(weekEnd) ? shift.getShiftEndDateTime() : weekEnd;
            if (!from.before(until)) {
                continue;
            }
            int capacity = 0;
            for (MachineScheduleDTO side : sides) {
                List<MachineMaintenanceWindowDTO> maintenance = new ArrayList<>(side.getMaintenanceWindowList());
                maintenance.addAll(ShiftCapacityResolverUtil.resolveCapsuleReplacementCapacityWindowList(
                        context, side.getMachineCode()));
                maintenance.addAll(LhTemporaryFaultService.resolveCapacityWindows(context, side.getMachineCode()));
                int runtimeCapacity = ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(
                        context, side, sku.getShiftCapacity());
                int available = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(otherStops,
                        CleaningScheduleRuleUtil.shouldSkipCleaningBySkuEnding(context, sku)
                                ? Collections.emptyList() : side.getCleaningWindowList(),
                        maintenance, side.getMachineCode(), from, until, runtimeCapacity,
                        sku.getLhTimeSeconds(), ShiftCapacityResolverUtil.resolveMachineMouldQty(side),
                        ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift), 0, 0, 0, true);
                available = ShiftProductionControlUtil.deductCapacityByControl(
                        ShiftProductionControlUtil.resolveEffectiveControl(context, shift, from), available,
                        ShiftCapacityResolverUtil.resolveMachineMouldQty(side));
                capacity += Math.max(0, available);
            }
            if (capacity >= left && capacity > 0) {
                long seconds = (until.getTime() - from.getTime()) / 1000L;
                return new Date(from.getTime() + (long) Math.ceil((double) left * seconds / capacity) * 1000L);
            }
            left -= capacity;
        }
        return null;
    }

    /**
     * 仅以维修时刻正在生产的当前SKU及整机侧位完成证明跳过05，不借用更早前料。
     *
     * @param context 已提交结果
     * @param plan 原实际维修
     * @return 完整收尾晚于原维修起点且在同周时的实际整机完成点
     */
    private static Date resolveCommittedEnding(LhScheduleContext context, MdmDevicePlanShut plan) {
        if (!isRepair(plan)) {
            return null;
        }
        List<LhScheduleResult> occupying = context.getScheduleResultList().stream()
                .filter(result -> samePhysical(result.getLhMachineCode(), plan.getMachineCode()))
                .filter(result -> result.getSpecEndTime() != null
                        && result.getSpecEndTime().after(plan.getBeginDate())
                        && !result.getSpecEndTime().after(weekEnd(plan.getBeginDate())))
                .filter(result -> startsBefore(result, plan.getBeginDate())).collect(Collectors.toList());
        if (occupying.isEmpty()) {
            return null;
        }
        LhScheduleResult last = occupying.stream().max(Comparator.comparing(
                LhScheduleResult::getSpecEndTime)).orElse(null);
        List<LhScheduleResult> group = occupying.stream()
                .filter(result -> StringUtils.equals(last.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(last.getProductStatus(), result.getProductStatus()))
                .collect(Collectors.toList());
        int target = resolveEndingTarget(context, last, group);
        int completed = context.getScheduleResultList().stream()
                .filter(result -> !LhSingleControlMachineUtil.isVirtualMachineCode(result.getLhMachineCode()))
                .filter(result -> StringUtils.equals(last.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(last.getProductStatus(), result.getProductStatus()))
                .mapToInt(ShiftFieldUtil::resolveScheduledQty).sum();
        // 最终isEnd统一刷新晚于维修时间校正，完整数量证明不能依赖尚未刷新的展示标记。
        if (target <= 0 || completed < target) {
            return null;
        }
        String pair = LhSingleControlMachineUtil.resolvePairMachineCode(last.getLhMachineCode());
        if (StringUtils.isNotEmpty(pair) && context.getMachineScheduleMap().containsKey(pair)
                && context.getScheduleResultList().stream().noneMatch(result ->
                StringUtils.equals(pair, result.getLhMachineCode())
                        && StringUtils.equals(last.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(last.getProductStatus(), result.getProductStatus())
                        && result.getSpecEndTime() != null && !result.getSpecEndTime().after(last.getSpecEndTime())
                        && startsBefore(result, plan.getBeginDate()))) {
            return null;
        }
        return last.getSpecEndTime();
    }

    /**
     * 核验首个真实有量生产段是否早于设备事件，零量占位不得作为在机证据。
     *
     * @param result 待核验的结果
     * @param boundary 原设备事件起点
     * @return 存在正量且开产早于边界时返回true
     */
    private static boolean startsBefore(LhScheduleResult result, Date boundary) {
        int first = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
        Date start = first > 0 ? ShiftFieldUtil.getShiftStartTime(result, first) : null;
        return start != null && start.before(boundary);
    }

    /**
     * 按已确认的收尾身份读取冻结目标，胎胚份额不得回退成原始硫化余量。
     *
     * @param context 本轮已冻结目标
     * @param result 维修时刻的当前物料
     * @param physicalResults 同物料状态的物理机台结果
     * @return 冻结的合法收尾总量，缺少胎胚份额时为零
     */
    private static int resolveEndingTarget(LhScheduleContext context, LhScheduleResult result,
            List<LhScheduleResult> physicalResults) {
        Integer target = context.getContinuationRemainderFinishGroups().stream()
                .filter(group -> StringUtils.equals(result.getMaterialCode(), group.getSourceSku().getMaterialCode())
                        && StringUtils.equals(result.getProductStatus(), group.getSourceSku().getProductStatus()))
                .map(group -> group.getOriginalMachineFinishPlan() == null
                        || group.getOriginalMachineFinishPlan().getRemainingQty() > 0
                        ? Integer.valueOf(0) : group.getFinalTargetQty())
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (target != null) {
            return target;
        }
        if ("1".equals(result.getIsEmbryoEnding())) {
            return context.getSharedEmbryoStockEndingInitialQuotaMap().getOrDefault(
                    MonthPlanDateResolver.buildMaterialStatusKey(
                            result.getMaterialCode(), result.getProductStatus()), 0);
        }
        return physicalResults.stream().map(LhScheduleResult::getMouldSurplusQty)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
    }

    /**
     * 判断有效05来源，不把故障及其他设备事件纳入收尾跳过。
     *
     * @param plan 设备计划
     * @return 是否为有效05维修
     */
    private static boolean isRepair(MdmDevicePlanShut plan) {
        return plan != null && MachineStopTypeEnum.PLANNED_REPAIR.getCode().equals(plan.getMachineStopType())
                && plan.getBeginDate() != null && plan.getEndDate() != null
                && plan.getBeginDate().before(plan.getEndDate());
    }

    /**
     * 比较归一后的物理机台身份。
     *
     * @param first 第一个机台
     * @param second 第二个机台
     * @return 是否同一物理机台
     */
    private static boolean samePhysical(String first, String second) {
        return StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(first),
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(second));
    }

    /**
     * 取得维修业务日所在自然周的下周一零点。
     *
     * @param date 维修业务日期
     * @return 半开区间的周末结束边界
     */
    private static Date weekEnd(Date date) {
        LocalDate nextMonday = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return Date.from(nextMonday.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 单候选的维修投影视图，关闭时恢复外层视图并失效派生容量缓存。
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 当前批次上下文。
         */
        private final LhScheduleContext context;
        /**
         * 外层候选持有的独立时间视图。
         */
        private final List<MdmDevicePlanShut> previous;
        /**
         * 本次试算前的完整有效视图，用于逐条恢复缺少收尾证据的05。
         */
        private final List<MdmDevicePlanShut> original;

        /**
         * 发布本次候选视图。
         *
         * @param context 当前批次
         * @param plans 本次独立时间投影
         */
        private Scope(LhScheduleContext context, List<MdmDevicePlanShut> plans) {
            this(context, plans, Collections.emptyList());
        }

        /**
         * 保存试算前来源并发布本次视图。
         *
         * @param context 本轮上下文
         * @param plans 试算设备视图
         * @param original 本次试算前的完整有效设备视图
         */
        private Scope(LhScheduleContext context, List<MdmDevicePlanShut> plans,
                List<MdmDevicePlanShut> original) {
            this.context = context;
            this.original = original;
            previous = context.getRepairPlanRuntimeState().getScopedStopPlans();
            context.getRepairPlanRuntimeState().setScopedStopPlans(plans);
        }

        /**
         * 按本次实际分配恢复不满足条件的05；每轮只增加约束，至多恢复全部来源。
         *
         * @param profiles 与收尾分配逐机对应的合法容量画像
         * @param calculated 尚未扣账的真实收尾计划
         * @return 有来源恢复时返回true，调用方必须重算后再提交
         */
        public boolean restoreUnprovenRepairs(List<ContinuationEndingMachineProfile> profiles,
                ContinuationFinishScheduleResult calculated) {
            List<MdmDevicePlanShut> current = context.getDevicePlanShutList();
            List<MdmDevicePlanShut> restored = original.stream()
                    .filter(plan -> !current.contains(plan))
                    .filter(plan -> !this.provesEnding(plan, profiles, calculated)).collect(Collectors.toList());
            if (restored.isEmpty()) {
                return false;
            }
            List<MdmDevicePlanShut> next = new ArrayList<>(current);
            next.addAll(restored);
            context.getRepairPlanRuntimeState().setScopedStopPlans(next);
            return true;
        }

        /**
         * 用整组清量及本物理机台的真实生产段证明收尾跨过维修起点，零量释放不取得豁免。
         *
         * @param repair 本次拟跳过的05
         * @param profiles 合法容量画像
         * @param calculated 尚未提交的分配
         * @return 本机全部运行侧均具备完整生产证据时返回true
         */
        private boolean provesEnding(MdmDevicePlanShut repair, List<ContinuationEndingMachineProfile> profiles,
                ContinuationFinishScheduleResult calculated) {
            if (calculated.getRemainingQty() > 0) {
                return false;
            }
            for (int index = 0; index < profiles.size(); index++) {
                ContinuationEndingMachineProfile profile = profiles.get(index);
                if (profile.getOriginals().stream().noneMatch(result -> samePhysical(
                        result.getLhMachineCode(), repair.getMachineCode()))) {
                    continue;
                }
                ContinuationMachineFinishPlan plan = calculated.getMachinePlans().get(index);
                if (plan.getPlanQty() <= 0 || plan.getFinishTime() == null
                        || !plan.getFinishTime().after(repair.getBeginDate())
                        || plan.getFinishTime().after(weekEnd(repair.getBeginDate()))) {
                    return false;
                }
                List<LhScheduleResult> proposed = profile.getOriginals().stream()
                        .map(result -> new LhScheduleResult()).collect(Collectors.toList());
                profile.writePlan(proposed, plan.getPlanQty());
                for (int side = 0; side < proposed.size(); side++) {
                    String code = profile.getOriginals().get(side).getLhMachineCode();
                    String pair = LhSingleControlMachineUtil.resolvePairMachineCode(code);
                    if (StringUtils.isNotEmpty(pair) && context.getMachineScheduleMap().containsKey(pair)
                            && profile.getOriginals().stream().noneMatch(result -> pair.equals(result.getLhMachineCode()))) {
                        return false;
                    }
                    if (!startsBefore(proposed.get(side), repair.getBeginDate())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        /**
         * 恢复外层候选视图，不改变固定来源、结果或数量账本。
         */
        @Override
        public void close() {
            context.getRepairPlanRuntimeState().setScopedStopPlans(previous);
        }
    }
}
