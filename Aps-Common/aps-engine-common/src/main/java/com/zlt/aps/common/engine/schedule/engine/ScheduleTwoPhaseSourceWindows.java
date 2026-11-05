package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 两阶段来源窗口并集。相同成型来源、物料和原消费班只保留一次，不同来源正常累加。
 * 统一请求、解释、消费及工装释放的来源归属，不修改来源任务或原需求班。
 */
public final class ScheduleTwoPhaseSourceWindows {

    /**
     * 从原始来源快照构建工装释放需求，与库存消费采用相同的来源和原需求班去重口径。
     * 来源解释标志不排除真实来源；lead、carry及保留事实仍按消费规则排除。
     *
     * @param tasks              聚合前来源快照，无快照时为未聚合正式任务
     * @param maxShiftOrder      有效原需求班上限
     * @param curlLengthResolver 领域有效卷长解析器
     * @param <T>                领域任务类型
     * @return 按原需求班及物料汇总的不可变工装需求，保留卷长冲突证据
     */
    public <T extends ScheduleTaskDraftModel> Map<Integer, List<ScheduleShiftToolDemand>> toolDemands(
            List<T> tasks, int maxShiftOrder, Function<T, BigDecimal> curlLengthResolver) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        ScheduleTwoPhaseRequestPreparer preparer = new ScheduleTwoPhaseRequestPreparer();
        Map<Integer, Map<String, Map<String, ScheduleShiftToolDemand>>> events = new LinkedHashMap<>();
        tasks.stream().filter(Objects::nonNull).forEach(task -> {
            BigDecimal consumption = preparer.consumptionQty(task);
            if (consumption.signum() <= 0) {
                return;
            }
            Integer shift = preparer.resolveDemandShift(task);
            BigDecimal curlLength = BigDecimalUtils.valueOf(curlLengthResolver.apply(task));
            if (shift == null || shift < 1 || shift > maxShiftOrder || curlLength.signum() <= 0) {
                return;
            }
            ScheduleShiftToolDemand demand = new ScheduleShiftToolDemand(shift, task.getProcessCode(),
                    task.getEmbryoCode(), consumption, curlLength);
            events.computeIfAbsent(shift, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(task.getProcessCode(), ignored -> new LinkedHashMap<>())
                    .merge(this.sourceKey(task), demand,
                            (first, second) -> this.mergeToolDemands(first, second, true));
        });
        Map<Integer, List<ScheduleShiftToolDemand>> result = new LinkedHashMap<>();
        events.forEach((shift, products) -> {
            List<ScheduleShiftToolDemand> demands = products.values().stream()
                    .map(sources -> sources.values().stream()
                            .reduce((first, second) -> this.mergeToolDemands(first, second, false)).orElse(null))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            result.put(shift, Collections.unmodifiableList(demands));
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 同事件副本取最大消费，不同来源累加；两种合并都不得丢失卷长冲突。
     *
     * @param first      已有需求
     * @param second     新需求
     * @param sameSource 是否为同来源同消费事件
     * @return 合并后不可变需求
     */
    private ScheduleShiftToolDemand mergeToolDemands(ScheduleShiftToolDemand first,
                                                     ScheduleShiftToolDemand second, boolean sameSource) {
        BigDecimal quantity = sameSource ? first.getFormingDemandQty().max(second.getFormingDemandQty())
                : first.getFormingDemandQty().add(second.getFormingDemandQty());
        List<BigDecimal> details = new ArrayList<>();
        if (sameSource) {
            details.add(quantity);
        } else {
            details.addAll(first.getFormingDemandQtyDetails());
            details.addAll(second.getFormingDemandQtyDetails());
        }
        String formingCodes = Arrays.asList(first.getFormingCode(), second.getFormingCode()).stream()
                .filter(Objects::nonNull).flatMap(codes -> Arrays.stream(codes.split(",")))
                .map(String::trim).filter(code -> !code.isEmpty()).distinct().collect(Collectors.joining(","));
        return new ScheduleShiftToolDemand(first.getShiftOrder(), first.getProductCode(),
                formingCodes.isEmpty() ? null : formingCodes, quantity, first.getCurlRollLength(), details,
                first.isCurlRollLengthConflict() || second.isCurlRollLengthConflict()
                        || first.getCurlRollLength().compareTo(second.getCurlRollLength()) != 0);
    }

    /**
     * 从原始来源快照构建关账消费；生产聚合、实验前移及解释副本不能改变原需求班。
     *
     * @param tasks         原始来源快照，无快照时使用尚未聚合的正式任务
     * @param maxShiftOrder 有效生产班次上限
     * @return 原需求班、物料对应的去重实际需求
     */
    public Map<Integer, Map<String, BigDecimal>> consumption(List<? extends ScheduleTaskDraftModel> tasks,
                                                             int maxShiftOrder) {
        ScheduleTwoPhaseRequestPreparer preparer = new ScheduleTwoPhaseRequestPreparer();
        Map<Integer, Map<String, Map<String, BigDecimal>>> events = new LinkedHashMap<>();
        tasks.stream().filter(Objects::nonNull).forEach(task -> {
            Integer shift = preparer.resolveDemandShift(task);
            if (shift != null && shift >= 1 && shift <= maxShiftOrder) {
                events.computeIfAbsent(shift, ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(task.getProcessCode(), ignored -> new LinkedHashMap<>())
                        .merge(this.sourceKey(task), preparer.consumptionQty(task), BigDecimal::max);
            }
        });
        Map<Integer, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        events.forEach((shift, products) -> {
            Map<String, BigDecimal> quantities = new LinkedHashMap<>();
            products.forEach((product, sources) -> quantities.put(product,
                    sources.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
            result.put(shift, quantities);
        });
        return result;
    }

    /**
     * 合并来源的完整供应窗口，保留各派生任务独有的需求班。
     *
     * @param tasks 同生产计划组的来源任务
     * @return 按原成型逻辑班汇总的去重窗口
     */
    public Map<Integer, BigDecimal> fullWindow(List<? extends ScheduleTaskDraftModel> tasks) {
        Map<String, Map<Integer, BigDecimal>> sources = this.windows(tasks, false, false);
        Map<Integer, BigDecimal> result = new LinkedHashMap<>();
        sources.values().forEach(window -> window.forEach((shift, qty) ->
                result.merge(shift, qty, BigDecimal::add)));
        // 供应时长和窗口证明按消费班顺序读取，不能依赖正需求、零需求候选的追加次序。
        return result.entrySet().stream().filter(entry -> entry.getKey() != null)
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        BigDecimal::max, LinkedHashMap::new));
    }

    /**
     * 计算去重后的当前需求或基础两班需求。
     *
     * @param tasks       来源任务
     * @param includeNext 是否包含下一班需求
     * @return 去重后的需求量
     */
    public BigDecimal baseDemand(List<? extends ScheduleTaskDraftModel> tasks, boolean includeNext) {
        return this.windows(tasks, true, includeNext).values().stream()
                .flatMap(window -> window.values().stream()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 为完整窗口每个来源消费分配唯一解释归属，避免同一窗口被派生任务重复分摊。
     *
     * @param tasks 来源任务
     * @return 来源业务键到唯一需求权重
     */
    public Map<String, BigDecimal> sourceWeights(List<? extends ScheduleTaskDraftModel> tasks) {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        tasks.forEach(task -> weights.put(task.getBusinessKey(), BigDecimal.ZERO));
        this.windows(tasks, false, false).forEach((source, window) -> window.forEach((shift, qty) -> {
            ScheduleTaskDraftModel owner = tasks.stream().filter(task -> source.equals(this.sourceKey(task)))
                    .filter(task -> Objects.equals(task.getFormingLogicalShiftOrder(), shift))
                    .findFirst().orElse(null);
            // 分开解析回退来源，避免两次通配符流产生不同捕获类型而无法传给orElseGet。
            if (owner == null) {
                owner = tasks.stream().filter(task -> source.equals(this.sourceKey(task)))
                        .filter(task -> task.getFormingGuardWindowQtyMap() != null
                                && task.getFormingGuardWindowQtyMap().containsKey(shift))
                        .findFirst().orElse(null);
            }
            if (owner != null) {
                weights.merge(owner.getBusinessKey(), qty, BigDecimal::add);
            }
        }));
        return weights;
    }

    /**
     * 按成型来源及物料构建消费班并集；同一事件取最大已批准数量，不能叠加派生副本。
     *
     * @param tasks       来源任务
     * @param base        是否仅构建基础窗口
     * @param includeNext 基础窗口是否包含下一班
     * @return 来源到消费班数量映射
     */
    private Map<String, Map<Integer, BigDecimal>> windows(List<? extends ScheduleTaskDraftModel> tasks,
                                                          boolean base, boolean includeNext) {
        Map<String, Map<Integer, BigDecimal>> result = new LinkedHashMap<>();
        tasks.forEach(task -> {
            Map<Integer, BigDecimal> window = result.computeIfAbsent(this.sourceKey(task),
                    ignored -> new LinkedHashMap<>());
            Integer shift = task.getFormingLogicalShiftOrder() == null
                    ? task.getSourceShiftOrder() : task.getFormingLogicalShiftOrder();
            if (!base && task.getFormingGuardWindowQtyMap() != null
                    && !task.getFormingGuardWindowQtyMap().isEmpty()) {
                task.getFormingGuardWindowQtyMap().forEach((logical, qty) ->
                        window.merge(logical, BigDecimalUtils.valueOf(qty), BigDecimal::max));
            } else {
                window.merge(shift, BigDecimalUtils.valueOf(task.getCurrentShiftDemandQty()), BigDecimal::max);
                if (base && includeNext && shift != null) {
                    window.merge(shift + 1, BigDecimalUtils.valueOf(task.getNextShiftDemandQty()), BigDecimal::max);
                } else if (!base && shift != null) {
                    window.merge(shift + 1, BigDecimalUtils.valueOf(task.getGuardDemandQty()), BigDecimal::max);
                }
            }
        });
        return result;
    }

    /**
     * 生成去重来源键；无成型来源的兼容任务只能按自身业务键区分。
     *
     * @param task 来源任务
     * @return 含来源日期及物料的键
     */
    private String sourceKey(ScheduleTaskDraftModel task) {
        String source = task.getFormingSourceKey();
        if (source == null || source.trim().isEmpty()) {
            source = task.getBusinessKey();
        }
        return Objects.toString(task.getFormingSourceScheduleDate(), "") + "|" + source
                + "|" + Objects.toString(task.getProcessCode(), "");
    }
}
