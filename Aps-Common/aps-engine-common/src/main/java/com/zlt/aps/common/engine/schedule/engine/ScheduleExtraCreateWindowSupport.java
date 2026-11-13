package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 从真实来源、已偏移窗口和成型日历构建公共窗口证明。
 *
 * <p>停放上下限、生产起止时点与期限净请求裁量已按业务确认下线：本支撑类只负责
 * 需求归集（参数化覆盖窗口、真实保证需求兼容路径）、需求侧窗口证明与来源权重；
 * 备库数量上限由目标、产能、工装与成型余量额度在数量计算器中约束。</p>
 */
public final class ScheduleExtraCreateWindowSupport {
    /**
     * 纯备库新行支持的最远成型逻辑班，不作为普通基础排程的覆盖参数上限。
     */
    public static final int MAX_FORMING_LOGICAL_SHIFT_ORDER = 15;

    private ScheduleExtraCreateWindowSupport() {
    }

    /**
     * 解析纯备库预加载所需的日历边界，保留CLASS1至CLASS8快照且最多准备到CLASS15。
     * 此处只限制额外日历的资源范围，不校验普通任务的覆盖窗口，也不截断业务需求。
     *
     * @param tasks 本批任务及其冻结的参数化覆盖窗口
     * @return CLASS8至CLASS15范围内的最远成型逻辑班
     */
    public static int resolveMaxFormingLogicalShiftOrder(List<? extends ScheduleTaskDraftModel> tasks) {
        long endShift = tasks.stream().filter(Objects::nonNull)
                .mapToLong(ScheduleExtraCreateWindowSupport::approvedWindowEndShift).max().orElse(8L);
        return (int) Math.max(8L, Math.min(MAX_FORMING_LOGICAL_SHIFT_ORDER, endShift));
    }

    /**
     * 判断实际纯备库候选能否在支持范围内证明整份批准窗口；不修改普通任务或窗口。
     *
     * @param task 已确认零基础且有正备库请求的候选
     * @return 批准窗口是否在CLASS15以内；无覆盖窗口的特殊规格沿用原兼容路径
     */
    public static boolean isApprovedWindowSupported(ScheduleTaskDraftModel task) {
        return approvedWindowEndShift(task) <= MAX_FORMING_LOGICAL_SHIFT_ORDER;
    }

    /**
     * 以long解析单任务批准窗口终点，不对普通覆盖参数施加纯备库范围限制。
     *
     * @param task 冻结覆盖窗口的任务
     * @return 有效窗口的终点；窗口缺失或基础字段无效返回零
     */
    private static long approvedWindowEndShift(ScheduleTaskDraftModel task) {
        ScheduleStockCoverageWindowModel window = task.getStockCoverageWindow();
        Integer startShift = task.getFormingLogicalShiftOrder();
        if (window == null || window.getShiftCount() == null || window.getShiftCount() <= 0
                || startShift == null || startShift <= 0) {
            return 0;
        }
        return (long) startShift + window.getShiftCount() - 1L;
    }

    /**
     * 独立新行按已冻结的参数化库存覆盖窗口重建损耗前净请求，不沿用普通库存覆盖清零后的全量请求。
     * 当前及下一班的实际净缺口必须仍为0，不能把未满足的基础保供包装成备库。
     *
     * @param context 工作区 @param task 零基础普通候选 @param <T> 任务类型
     * @return 参数化覆盖窗口需求减班初库存后的正缺口；基础未满足或资料不足返回0
     */
    public static <T extends ScheduleTaskDraftModel> BigDecimal resolveRequest(
            ScheduleContextModel<T, ?, ?, ?, ?> context, T task) {
        BigDecimal stock = BigDecimalUtils.valueOf(task.getRollingStockQty()).max(BigDecimal.ZERO);
        BigDecimal baseDemand = BigDecimalUtils.valueOf(task.getCurrentShiftDemandQty()).max(BigDecimal.ZERO)
                .add(BigDecimalUtils.valueOf(task.getNextShiftDemandQty()).max(BigDecimal.ZERO));
        if (baseDemand.compareTo(stock) > 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal demand = task.getStockCoverageWindow() == null
                ? actualGuardDemandByShift(context, task).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                : approvedDemandTotal(task);
        return demand.subtract(stock).max(BigDecimal.ZERO);
    }

    /**
     * 按显式偏移与压缩外推段计算整窗需求，避免尚未筛选的超长窗口按班展开。
     * 显式零值或空值也占用偏移位置，不再由外推均值补回；范围外偏移不参与。
     *
     * @param task 冻结批准窗口的任务
     * @return 整份窗口的非负需求合计，不受纯备库CLASS15支持范围截断
     */
    private static BigDecimal approvedDemandTotal(ScheduleTaskDraftModel task) {
        if (approvedWindowEndShift(task) == 0) {
            return BigDecimal.ZERO;
        }
        ScheduleStockCoverageWindowModel window = task.getStockCoverageWindow();
        Map<Integer, BigDecimal> quantities = window.getDemandQtyByOffset() == null
                ? Collections.emptyMap() : window.getDemandQtyByOffset();
        BigDecimal explicitDemand = quantities.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey() >= 0
                        && entry.getKey() < window.getShiftCount())
                .map(entry -> BigDecimalUtils.valueOf(entry.getValue()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (window.getExtrapolatedStartOffset() == null || window.getExtrapolatedShiftCount() == null
                || window.getExtrapolatedShiftCount() <= 0) {
            return explicitDemand;
        }
        long startOffset = Math.max(0L, window.getExtrapolatedStartOffset());
        long endOffset = Math.min((long) window.getShiftCount(),
                (long) window.getExtrapolatedStartOffset() + window.getExtrapolatedShiftCount());
        if (endOffset <= startOffset) {
            return explicitDemand;
        }
        long overriddenOffsets = quantities.keySet().stream().filter(Objects::nonNull)
                .filter(offset -> offset >= startOffset && offset < endOffset).count();
        return explicitDemand.add(BigDecimalUtils.valueOf(window.getExtrapolatedDemandQtyPerShift())
                .max(BigDecimal.ZERO).multiply(BigDecimalUtils.valueOf(endOffset - startOffset - overriddenOffsets)));
    }

    /**
     * 按当前批次参数化覆盖窗口展开已批准需求；窗口缺失时使用特殊规格真实保证需求兼容路径。
     *
     * @param context 工作区
     * @param task 已匹配来源工艺的候选
     * @param <T> 任务类型
     * @return 按成型逻辑班升序的候选需求
     */
    private static <T extends ScheduleTaskDraftModel> Map<Integer, BigDecimal> candidateDemandByShift(
            ScheduleContextModel<T, ?, ?, ?, ?> context, T task) {
        if (task.getStockCoverageWindow() != null) {
            return approvedDemandByShift(task);
        }
        return actualGuardDemandByShift(context, task);
    }

    /**
     * 展开本批已冻结的库存覆盖窗口。覆盖窗口之外的保证需求不得混入纯备库候选。
     *
     * @param task 已冻结库存覆盖窗口的候选
     * @param <T>  任务类型
     * @return 按成型逻辑班升序的批准需求
     */
    private static <T extends ScheduleTaskDraftModel> Map<Integer, BigDecimal> approvedDemandByShift(T task) {
        Map<Integer, BigDecimal> demand = new TreeMap<>();
        long endShift = approvedWindowEndShift(task);
        if (endShift == 0 || endShift > MAX_FORMING_LOGICAL_SHIFT_ORDER) {
            return demand;
        }
        int startShift = task.getFormingLogicalShiftOrder();
        ScheduleStockCoverageWindowModel window = task.getStockCoverageWindow();
        for (int logicalShift = startShift; logicalShift <= endShift; logicalShift++) {
            BigDecimal approvedQuantity = approvedDemandQtyByOffset(window, logicalShift - startShift);
            if (approvedQuantity.signum() > 0) {
                demand.put(logicalShift, approvedQuantity);
            }
        }
        return demand;
    }

    /**
     * 按逻辑班直接读取来源覆盖窗口的单班需求，避免来源权重逐班重复展开整份窗口。
     * 显式偏移（包括零值和空值）优先于外推段；窗口外班次不参与。
     *
     * @param task         来源任务及其冻结覆盖窗口
     * @param logicalShift 待读取的成型逻辑班
     * @return 该班非负需求；窗口缺失或班次不在窗口内返回零
     */
    private static BigDecimal approvedDemandQtyAtShift(ScheduleTaskDraftModel task, int logicalShift) {
        long endShift = approvedWindowEndShift(task);
        if (endShift == 0 || logicalShift > MAX_FORMING_LOGICAL_SHIFT_ORDER
                || logicalShift < task.getFormingLogicalShiftOrder() || logicalShift > endShift) {
            return BigDecimal.ZERO;
        }
        return approvedDemandQtyByOffset(task.getStockCoverageWindow(), logicalShift - task.getFormingLogicalShiftOrder());
    }

    /**
     * 读取已校验窗口中的单个偏移；供整窗展开和来源单班权重共同复用。
     *
     * @param window 已通过范围校验的批准窗口
     * @param offset 已确认位于窗口内的相对班次偏移
     * @return 显式需求优先、外推需求补充后的非负米数
     */
    private static BigDecimal approvedDemandQtyByOffset(ScheduleStockCoverageWindowModel window, int offset) {
        Map<Integer, BigDecimal> demandQtyByOffset = window.getDemandQtyByOffset();
        if (demandQtyByOffset != null && demandQtyByOffset.containsKey(offset)) {
            return BigDecimalUtils.valueOf(demandQtyByOffset.get(offset)).max(BigDecimal.ZERO);
        }
        Integer extrapolatedStartOffset = window.getExtrapolatedStartOffset();
        Integer extrapolatedShiftCount = window.getExtrapolatedShiftCount();
        if (extrapolatedStartOffset != null && extrapolatedShiftCount != null && extrapolatedShiftCount > 0
                && offset >= extrapolatedStartOffset
                && offset < (long) extrapolatedStartOffset + extrapolatedShiftCount) {
            return BigDecimalUtils.valueOf(window.getExtrapolatedDemandQtyPerShift()).max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 特殊规格没有参数化库存覆盖快照时，沿用CLASS1至CLASS8的真实保证需求口径。
     *
     * @param context 工作区
     * @param task    已匹配来源工艺的候选
     * @param <T>     任务类型
     * @return 按成型逻辑班升序的真实保证需求
     */
    private static <T extends ScheduleTaskDraftModel> Map<Integer, BigDecimal> actualGuardDemandByShift(
            ScheduleContextModel<T, ?, ?, ?, ?> context, T task) {
        Map<Integer, BigDecimal> demand = new TreeMap<>();
        SchedulePlanTaskGroup<T> group = context.getPlanTaskGroupMap().get(task.getPlanGroupKey());
        if (group == null || task.getFormingGuardWindowQtyMap() == null
                || task.getFormingLogicalShiftOrder() == null || task.getGuardShiftCount() == null
                || task.getGuardShiftCount() <= 0) {
            return demand;
        }
        Map<String, List<T>> sources = group.getSourceTaskList().stream().filter(Objects::nonNull)
                .filter(source -> Objects.equals(source.getFormingSourceScheduleDate(), context.getScheduleDate()))
                .collect(Collectors.groupingBy(source -> source.getFormingSourceKey() == null
                        ? source.getBusinessKey() : source.getFormingSourceKey()));
        task.getFormingGuardWindowQtyMap().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey() >= 1 && entry.getKey() <= 8)
                .filter(entry -> entry.getKey() >= task.getFormingLogicalShiftOrder()
                        && (long) entry.getKey() <= (long) task.getFormingLogicalShiftOrder() + task.getGuardShiftCount())
                .forEach(entry -> {
                    int logical = entry.getKey();
                    BigDecimal raw = sources.values().stream().map(sourceList -> sourceList.stream()
                            .filter(source -> Objects.equals(source.getFormingClassProductCodeMap().get(logical),
                                    task.getProcessCode()))
                            .map(source -> BigDecimalUtils.valueOf(source.getFormingClassQtyMap().get(logical))
                                    .multiply(BigDecimalUtils.valueOf(source.getFormingClassLengthMap().get(logical))))
                            .reduce(BigDecimal.ZERO, BigDecimal::max)).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal quantity = raw.min(BigDecimalUtils.valueOf(entry.getValue()));
                    if (quantity.signum() > 0) {
                        demand.put(logical, quantity);
                    }
                });
        return demand;
    }

    /**
     * 将新行来源权重限定为本次合格的真实消费班；同一来源的派生快照只向稳定锚点归属一次。
     *
     * @param group 当前计划组 @param task 已确认工艺 @param proof 窗口证明
     * @param <T>   来源任务类型
     * @return 含所有来源键的独立权重表；不符合窗口的来源权重为0
     */
    public static <T extends ScheduleTaskDraftModel> Map<String, BigDecimal> sourceWeights(
            SchedulePlanTaskGroup<T> group, T task, ScheduleExtraCreateWindowProof proof) {
        if (task.getStockCoverageWindow() != null) {
            return approvedSourceWeights(group, task, proof.getEligibleFormingShiftOrders());
        }
        return sourceWeights(group, task, proof.getEligibleFormingShiftOrders());
    }

    /**
     * 普通覆盖分配权重可能全为0，独立新行必须按批准窗口内真实消费重建来源权重。
     *
     * @param context 工作区 @param task 当前候选 @param <T> 任务类型
     * @return 本次独立备库请求的来源权重，不修改原始需求量
     */
    public static <T extends ScheduleTaskDraftModel> Map<String, BigDecimal> requestSourceWeights(
            ScheduleContextModel<T, ?, ?, ?, ?> context, T task) {
        SchedulePlanTaskGroup<T> group = context.getPlanTaskGroupMap().get(task.getPlanGroupKey());
        if (task.getStockCoverageWindow() != null) {
            return approvedSourceWeights(group, task, candidateDemandByShift(context, task).keySet());
        }
        return sourceWeights(group, task, candidateDemandByShift(context, task).keySet());
    }

    /**
     * 根据来源各自冻结的覆盖窗口重建解释权重，同源同班取最大值并归属稳定锚点。
     *
     * @param group  当前计划组
     * @param task   已匹配来源工艺的候选
     * @param shifts 本次参与计算的成型逻辑班
     * @param <T>    来源任务类型
     * @return 含全部来源业务键的独立权重表
     */
    private static <T extends ScheduleTaskDraftModel> Map<String, BigDecimal> approvedSourceWeights(
            SchedulePlanTaskGroup<T> group, T task, Collection<Integer> shifts) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        if (group == null || group.getSourceTaskList() == null) {
            return weights;
        }
        group.getSourceTaskList().stream().filter(Objects::nonNull)
                .forEach(source -> weights.put(source.getBusinessKey(), BigDecimal.ZERO));
        group.getSourceTaskList().stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(source -> {
                    String formingSourceKey = source.getFormingSourceKey();
                    return formingSourceKey == null || formingSourceKey.trim().isEmpty()
                            ? source.getBusinessKey() : formingSourceKey;
                }))
                .values().forEach(sources -> {
                    T anchor = sources.stream().min(java.util.Comparator.comparing(
                            ScheduleTaskDraftModel::getBusinessKey)).orElse(null);
                    if (anchor == null) {
                        return;
                    }
                    BigDecimal weight = shifts.stream().distinct().map(logical -> sources.stream()
                                    .filter(source -> Objects.equals(source.getProcessCode(), task.getProcessCode()))
                                    .map(source -> approvedDemandQtyAtShift(source, logical))
                                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    weights.put(anchor.getBusinessKey(), weight);
                });
        return weights;
    }

    /**
     * @param group 来源组 @param task 工艺候选 @param shifts 合法消费班 @param <T> 任务类型 @return 来源权重
     */
    private static <T extends ScheduleTaskDraftModel> Map<String, BigDecimal> sourceWeights(
            SchedulePlanTaskGroup<T> group, T task, Collection<Integer> shifts) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        group.getSourceTaskList().forEach(source -> weights.put(source.getBusinessKey(), BigDecimal.ZERO));
        group.getSourceTaskList().stream().collect(Collectors.groupingBy(source -> source.getFormingSourceKey() == null
                ? source.getBusinessKey() : source.getFormingSourceKey())).values().forEach(sources -> {
            T anchor = sources.stream().min(java.util.Comparator.comparing(
                    ScheduleTaskDraftModel::getBusinessKey)).orElse(null);
            if (anchor != null) {
                BigDecimal weight = shifts.stream().distinct().map(logical ->
                                sources.stream().filter(source -> Objects.equals(
                                                source.getFormingClassProductCodeMap().get(logical), task.getProcessCode()))
                                        .map(source -> BigDecimalUtils.valueOf(source.getFormingClassQtyMap().get(logical))
                                                .multiply(BigDecimalUtils.valueOf(source.getFormingClassLengthMap().get(logical))))
                                        .reduce(BigDecimal.ZERO, BigDecimal::max))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                weights.put(anchor.getBusinessKey(), weight);
            }
        });
        return weights;
    }

    /**
     * 从来源及已批准窗口构建需求侧窗口证明（窗口范围、成型日历与口型白名单）。
     *
     * @param context 工作区 @param task 已试排候选 @param machine 目标机台 @param <T> 任务类型
     * @return 需求侧窗口证明，不修改上下文
     */
    public static <T extends ScheduleTaskDraftModel> ScheduleExtraCreateWindowProof evaluate(
            ScheduleContextModel<T, ?, ?, ?, ?> context, T task, ScheduleMachineCandidateModel machine) {
        return new ScheduleExtraCreateWindowGuard().evaluate(windowRequest(context, task, machine));
    }

    /**
     * 从来源及已批准窗口组装窗口输入，供需求侧证明使用。
     *
     * @param context 工作区 @param task 候选 @param machine 目标机台 @param <T> 任务类型
     * @return 独立的窗口输入，不修改上下文
     */
    private static <T extends ScheduleTaskDraftModel> ScheduleExtraCreateWindowRequest windowRequest(
            ScheduleContextModel<T, ?, ?, ?, ?> context, T task, ScheduleMachineCandidateModel machine) {
        ScheduleExtraCreateWindowRequest request = new ScheduleExtraCreateWindowRequest();
        request.setCurrentFormingLogicalShiftOrder(task.getFormingLogicalShiftOrder());
        ScheduleStockCoverageWindowModel stockCoverageWindow = task.getStockCoverageWindow();
        int guardShiftCount = stockCoverageWindow == null
                ? BigDecimalUtils.valueOf(task.getGuardShiftCount()).intValue()
                : BigDecimalUtils.valueOf(stockCoverageWindow.getShiftCount()).intValue() - 1;
        request.setGuardShiftCount(guardShiftCount);
        request.setMouthPlateCode(task.getMouthPlateCode());
        request.setAllowedMouthPlateCodes(machine.getMouthPlateCodes());
        // 压出CLASS1为中班，对应成型CLASS2；这里按同时段检查，不能再次应用供料偏移。
        request.setProductionWindowFormingCalendarOpen(
                context.getExtraCreateFormingCalendarOpenMap().get(task.getShiftOrder() + 1));
        List<ScheduleExtraCreateDemandSlice> slices = new ArrayList<>();
        candidateDemandByShift(context, task).entrySet().forEach(entry -> {
            int logical = entry.getKey();
            ScheduleExtraCreateDemandSlice slice = new ScheduleExtraCreateDemandSlice();
            slice.setFormingLogicalShiftOrder(logical);
            slice.setDemandQty(entry.getValue());
            slice.setRealDemand(logical <= 8);
            slice.setApprovedDemand(stockCoverageWindow != null);
            slice.setExtrapolatedDemand(logical > 8);
            slice.setCalendarOpen(context.getExtraCreateFormingCalendarOpenMap().get(logical));
            slices.add(slice);
        });
        request.setDemandSlices(slices);
        return request;
    }
}
