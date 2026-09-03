package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationItem;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动排程计划量公共主流程引擎。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 * @param <F> 库存预测类型
 * @param <P> 计划量策略类型
 * @param <D> 需求量策略类型
 */
public final class PlanCalculationEngine<C extends PlanCalculationContext<T, F, G>,
        T extends ScheduleTaskDraftModel, F extends ScheduleInventoryForecast,
        G extends SchedulePlanTaskGroup<T>, P, D> {

    /**
     * 执行计划量计算主流程。
     *
     * @param context 排程上下文
     * @param policy 领域策略
     */
    public void calculate(C context, PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        this.prepare(context, policy);
        if (context.getTaskDraftList() == null || context.getTaskDraftList().isEmpty()) {
            return;
        }
        Map<String, F> stockForecastMap = context.getStockForecastMap();
        String planStrategyCode = policy.resolvePlanStrategyCode(context);
        P planStrategy = policy.resolvePlanStrategy(planStrategyCode);
        String demandAlgorithmCode = policy.resolveDemandAlgorithmCode(context);
        D demandStrategy = policy.resolveDemandStrategy(demandAlgorithmCode);
        Map<String, BigDecimal> remainingStockMap = context.getRemainingStockMap();
        BigDecimal remainingToolQty = context.getCurrentAvailableToolQty();
        Map<Integer, List<T>> taskGroupByShift = new LinkedHashMap<>();
        for (T task : context.getTaskDraftList()) {
            taskGroupByShift.computeIfAbsent(task == null ? null : task.getShiftOrder(), key -> new ArrayList<>())
                    .add(task);
        }
        List<T> orderedTaskList = new ArrayList<>();
        int orderIndex = 0;
        for (List<T> shiftTaskList : taskGroupByShift.values()) {
            policy.prepareShiftDemandAndSupply(context, shiftTaskList, stockForecastMap, remainingStockMap,
                    demandStrategy, demandAlgorithmCode);
            policy.sortPlanCalcShiftTasks(context, shiftTaskList);
            for (T task : shiftTaskList) {
                task.setPlanCalcOrderIndex(++orderIndex);
                orderedTaskList.add(task);
                remainingToolQty = policy.calculatePlanQtyForTask(context, task, stockForecastMap,
                        remainingStockMap, remainingToolQty, planStrategy, planStrategyCode, demandAlgorithmCode);
            }
        }
        context.setTaskDraftList(orderedTaskList);
    }

    /**
     * 准备自动排程任务和库存初始状态，不计算依赖跨班实际承接量的库存供应时长和计划量。
     *
     * @param context 排程上下文
     * @param policy  领域计划计算策略
     */
    public void prepare(C context, PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        policy.validateContext(context);
        if (context.getTaskDraftList() == null || context.getTaskDraftList().isEmpty()) {
            return;
        }
        this.aggregateTaskDrafts(context, policy);
        Map<String, F> stockForecastMap = context.getStockForecastMap();

        Map<String, BigDecimal> remainingStockMap = new HashMap<>();
        if (stockForecastMap != null) {
            stockForecastMap.forEach((code, forecast) -> remainingStockMap.put(code,
                    forecast.getRollingStockQty() == null ? BigDecimal.ZERO : forecast.getRollingStockQty()));
        }
        context.setInitialStockMap(new HashMap<>(remainingStockMap));
        context.setProductShiftShortageMap(new LinkedHashMap<>());
        context.setRemainingStockMap(remainingStockMap);
        context.getTaskDraftList().sort(Comparator
                .comparing(ScheduleTaskDraftModel::getShiftOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ScheduleTaskDraftModel::getProcessCode,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> this.defaultString(task.getBusinessKey())));

        BigDecimal remainingToolQty = policy.initializeGlobalAvailableToolQty(context, stockForecastMap);
        context.setInitialAvailableToolQty(remainingToolQty);
        context.setCurrentAvailableToolQty(remainingToolQty);
    }

    /**
     * 使用上一班实际关账库存计算当前班次的需求、供应时长、排序和计划量。
     * 当前方法只允许使用局部计划库存推进同班任务，禁止把理论交接库存写入跨班运行库存。
     *
     * @param context         排程上下文
     * @param shiftTaskList   当前班次任务
     * @param runtimeStockMap 上一班实际关账形成的运行库存
     * @param policy          领域计划计算策略
     */
    public void calculateShiftWithActualStock(C context, List<T> shiftTaskList,
                                               Map<String, BigDecimal> runtimeStockMap,
                                               PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        if (shiftTaskList == null || shiftTaskList.isEmpty()) {
            return;
        }
        Map<String, F> stockForecastMap = context.getStockForecastMap();
        String planStrategyCode = policy.resolvePlanStrategyCode(context);
        P planStrategy = policy.resolvePlanStrategy(planStrategyCode);
        String demandAlgorithmCode = policy.resolveDemandAlgorithmCode(context);
        D demandStrategy = policy.resolveDemandStrategy(demandAlgorithmCode);
        Map<String, BigDecimal> planningStockMap = new HashMap<>(runtimeStockMap);
        Set<String> recalculatedProductCodeSet = new HashSet<>();
        context.setRemainingStockMap(planningStockMap);
        try {
            policy.prepareShiftDemandAndSupply(context, shiftTaskList, stockForecastMap, planningStockMap,
                    demandStrategy, demandAlgorithmCode);
            policy.sortPlanCalcShiftTasks(context, shiftTaskList);
            int orderIndex = this.resolveNextPlanCalcOrderIndex(context);
            BigDecimal remainingToolQty = context.getCurrentAvailableToolQty();
            for (T task : shiftTaskList) {
                task.setPlanCalcOrderIndex(++orderIndex);
                task.setBaseSortIndex(orderIndex);
                if (!this.isPresetPlanTask(task)) {
                    task.setPlanQty(null);
                }
                String productCode = task.getProcessCode();
                if (productCode == null || productCode.trim().isEmpty()) {
                    continue;
                }
                // 同一产品同班只计算首个非预置任务，保留原有计划组去重语义，避免局部库存被重复扣减。
                if (!this.isPresetPlanTask(task) && !recalculatedProductCodeSet.add(productCode)) {
                    task.setPreLossPlanQty(BigDecimal.ZERO);
                    task.setLossAddQty(BigDecimal.ZERO);
                    task.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
                    task.setPlanQty(BigDecimal.ZERO);
                    task.setPlanStockQty(planningStockMap.getOrDefault(productCode,
                            this.nvl(task.getRollingStockQty())));
                    continue;
                }
                remainingToolQty = policy.calculatePlanQtyForTask(context, task, stockForecastMap,
                        planningStockMap, remainingToolQty, planStrategy, planStrategyCode, demandAlgorithmCode);
                if (productCode != null && !productCode.trim().isEmpty()) {
                    planningStockMap.put(productCode, this.nvl(task.getPlanStockQty()));
                }
            }
            context.setCurrentAvailableToolQty(remainingToolQty);
        } finally {
            context.setRemainingStockMap(new HashMap<>(runtimeStockMap));
        }
    }

    /**
     * 获取当前上下文中下一个计划量计算顺序。
     *
     * @param context 排程上下文
     * @return 下一个顺序的前一个值
     */
    private int resolveNextPlanCalcOrderIndex(C context) {
        return context.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .map(ScheduleTaskDraftModel::getPlanCalcOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * 空值转零。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 按产品编码和班次汇总原始成型来源任务。
     *
     * @param context 排程上下文
     * @param policy  领域差异策略
     * @throws RuntimeException 同组生产属性不一致时抛出领域异常
     */
    private void aggregateTaskDrafts(C context, PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        if (context.getPlanTaskGroupMap() != null && !context.getPlanTaskGroupMap().isEmpty()
                && context.getSourceTaskDraftList() != null && !context.getSourceTaskDraftList().isEmpty()) {
            return;
        }
        List<T> originalTaskList = new ArrayList<>(context.getTaskDraftList());
        Map<String, List<T>> groupedTaskMap = originalTaskList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(task -> this.buildPlanGroupKey(context, task),
                        LinkedHashMap::new, Collectors.toList()));
        List<String> conflictMessageList = groupedTaskMap.entrySet().stream()
                .map(entry -> policy.validatePlanGroup(context, entry.getKey(), entry.getValue()))
                .filter(message -> message != null && !message.trim().isEmpty())
                .collect(Collectors.toList());
        if (!conflictMessageList.isEmpty()) {
            throw policy.planGroupConflictException(conflictMessageList);
        }

        List<T> aggregateTaskList = new ArrayList<>();
        List<T> sourceTaskList = new ArrayList<>();
        Map<String, G> planTaskGroupMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> entry : groupedTaskMap.entrySet()) {
            String planGroupKey = entry.getKey();
            List<T> groupSourceList = entry.getValue();
            T aggregateTask = groupSourceList.size() == 1
                    ? groupSourceList.get(0) : policy.copyDerivedTask(groupSourceList.get(0));
            policy.applyTailDecision(aggregateTask, groupSourceList);
            boolean formingShutdownCloseOut = groupSourceList.stream()
                    .allMatch(task -> Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag()));
            aggregateTask.setFormingShutdownCloseOutFlag(formingShutdownCloseOut);
            aggregateTask.setFormingShutdownCloseOutDemandQty(formingShutdownCloseOut
                    ? groupSourceList.stream().map(ScheduleTaskDraftModel::getFormingShutdownCloseOutDemandQty)
                    .map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO);
            policy.enrichAggregateTask(aggregateTask, groupSourceList);

            List<T> sourceSnapshotList = groupSourceList.stream()
                    .map(sourceTask -> this.copySourceTask(policy, sourceTask, planGroupKey))
                    .collect(Collectors.toList());
            Map<Integer, BigDecimal> guardWindowQtyMap = this.resolveGroupGuardWindowQtyMap(groupSourceList);
            BigDecimal rawCurrentShiftDemandQty = this.sum(groupSourceList,
                    ScheduleTaskDraftModel::getCurrentShiftDemandQty);
            BigDecimal currentShiftDemandQty = this.resolveGroupCurrentShiftDemandQty(groupSourceList,
                    rawCurrentShiftDemandQty, guardWindowQtyMap);
            BigDecimal originalCurrentShiftDemandQty = groupSourceList.stream()
                    .map(this::resolveOriginalCurrentShiftDemandQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal nextShiftDemandQty = this.sum(groupSourceList,
                    ScheduleTaskDraftModel::getNextShiftDemandQty);
            BigDecimal guardDemandQty = this.resolveGroupGuardDemandQty(groupSourceList,
                    rawCurrentShiftDemandQty, guardWindowQtyMap);
            Integer formingLogicalShiftOrder = this.resolveGroupCurrentLogicalShiftOrder(groupSourceList,
                    guardWindowQtyMap);

            aggregateTask.setPlanGroupKey(planGroupKey);
            aggregateTask.setSourceTaskBusinessKeyList(sourceSnapshotList.stream()
                    .map(ScheduleTaskDraftModel::getBusinessKey).collect(Collectors.toList()));
            aggregateTask.setSourceExplainTask(Boolean.FALSE);
            if (groupSourceList.size() > 1) {
                aggregateTask.setBusinessKeySuffix("PLAN_GROUP_" + Integer.toHexString(planGroupKey.hashCode()));
            }
            aggregateTask.setSourceOrderNos(groupSourceList.stream()
                    .map(ScheduleTaskDraftModel::getSourceOrderNos)
                    .filter(value -> value != null && !value.trim().isEmpty())
                    .flatMap(value -> Arrays.stream(value.split(",")))
                    .map(String::trim).filter(value -> !value.isEmpty()).distinct()
                    .collect(Collectors.joining(",")));
            aggregateTask.setEmbryoCode(this.mergeDistinctText(groupSourceList,
                    ScheduleTaskDraftModel::getEmbryoCode));
            aggregateTask.setCxMachineCode(this.mergeDistinctText(groupSourceList,
                    ScheduleTaskDraftModel::getCxMachineCode));
            aggregateTask.setCurrentShiftDemandQty(currentShiftDemandQty);
            aggregateTask.setOriginalCurrentShiftDemandQty(originalCurrentShiftDemandQty);
            aggregateTask.setNextShiftDemandQty(nextShiftDemandQty);
            aggregateTask.setGuardDemandQty(guardDemandQty);
            aggregateTask.setFormingGuardWindowQtyMap(guardWindowQtyMap);
            if (formingLogicalShiftOrder != null) {
                aggregateTask.setFormingLogicalShiftOrder(formingLogicalShiftOrder);
            }
            aggregateTask.setFormingGuardWindowHoursMap(
                    this.resolveGroupGuardWindowHoursMap(context, guardWindowQtyMap, 6));
            aggregateTask.setDemandQty(null);
            if (groupSourceList.size() > 1) {
                aggregateTask.setPlanQty(null);
            }

            G taskGroup = policy.createPlanTaskGroup();
            taskGroup.setPlanGroupKey(planGroupKey);
            taskGroup.setAggregateTask(aggregateTask);
            taskGroup.setSourceTaskList(sourceSnapshotList);
            taskGroup.setGroupCurrentShiftDemandQty(currentShiftDemandQty);
            taskGroup.setGroupNextShiftDemandQty(nextShiftDemandQty);
            taskGroup.setGroupGuardDemandQty(guardDemandQty);
            planTaskGroupMap.put(planGroupKey, taskGroup);
            aggregateTaskList.add(aggregateTask);
            sourceTaskList.addAll(sourceSnapshotList);
        }
        context.setPlanTaskGroupMap(planTaskGroupMap);
        context.setSourceTaskDraftList(sourceTaskList);
        context.setTaskDraftList(aggregateTaskList);
    }

    private T copySourceTask(PlanCalculationPolicy<C, T, F, G, P, D> policy,
                             T sourceTask, String planGroupKey) {
        T sourceSnapshot = policy.copyDerivedTask(sourceTask);
        sourceSnapshot.setPlanGroupKey(planGroupKey);
        sourceSnapshot.setSourceExplainTask(Boolean.TRUE);
        sourceSnapshot.setSourceTaskBusinessKeyList(null);
        return sourceSnapshot;
    }

    private String buildPlanGroupKey(C context, T task) {
        String dateText = context.getScheduleDate() == null ? null
                : new SimpleDateFormat("yyyy-MM-dd").format(context.getScheduleDate());
        String groupKey = this.defaultString(context.getFactoryCode())
                + "|" + dateText
                + "|" + this.defaultString(task.getProcessCode())
                + "|" + String.valueOf(task.getShiftOrder());
        if (task.getPlanQty() != null) {
            return groupKey + "|PRESET|" + task.getBusinessKey();
        }
        return Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                ? groupKey + "|FORMING_SHUTDOWN_CLOSE_OUT" : groupKey;
    }

    private BigDecimal sum(List<T> taskList, Function<ScheduleTaskDraftModel, BigDecimal> valueGetter) {
        return taskList.stream().map(valueGetter).map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 将汇总计划量按来源需求权重分摊回解释任务。
     *
     * @param context          排程上下文
     * @param aggregateTask    汇总任务
     * @param calculationScale 分摊精度
     * @param formulaDesc      领域公式说明
     * @param tracePort        领域轨迹端口
     */
    public void applyPlanGroupResult(C context, T aggregateTask, int calculationScale,
                                     String formulaDesc, PlanGroupResultTracePort<C, T, G> tracePort) {
        G taskGroup = context.getPlanTaskGroupMap().get(aggregateTask.getPlanGroupKey());
        if (taskGroup == null || taskGroup.getSourceTaskList() == null
                || taskGroup.getSourceTaskList().isEmpty()) {
            return;
        }
        boolean twoShiftStockCovered = Boolean.TRUE.equals(aggregateTask.getTwoShiftStockCovered());
        Map<String, BigDecimal> sourceWeightMap = this.resolveSourceWeightMap(taskGroup.getSourceTaskList(),
                aggregateTask, twoShiftStockCovered);
        taskGroup.setSourceWeightMap(sourceWeightMap);
        taskGroup.setGroupBaseDemandQty(aggregateTask.getBaseDemandQty());
        taskGroup.setGroupMinStartAdjustQty(aggregateTask.getMinStartAdjustQty());
        taskGroup.setGroupRoundAdjustQty(aggregateTask.getTailRoundAdjustQty());
        taskGroup.setGroupFinalPlanQty(aggregateTask.getPlanQty());
        this.fillGroupFields(aggregateTask, taskGroup);

        Map<String, BigDecimal> stockDeductAllocationMap = this.allocateByWeight(
                aggregateTask.getStockDeductQty(), sourceWeightMap, calculationScale);
        Map<String, BigDecimal> baseDemandAllocationMap = this.allocateByWeight(
                aggregateTask.getBaseDemandQty(), sourceWeightMap, calculationScale);
        Map<String, BigDecimal> minStartAllocationMap = this.allocateByWeight(
                aggregateTask.getMinStartAdjustQty(), sourceWeightMap, calculationScale);
        Map<String, BigDecimal> roundAllocationMap = this.allocateByWeight(
                aggregateTask.getTailRoundAdjustQty(), sourceWeightMap, calculationScale);
        Map<String, BigDecimal> finalPlanAllocationMap = this.allocateByWeight(
                aggregateTask.getPlanQty(), sourceWeightMap, calculationScale);
        Map<String, BigDecimal> planStockAllocationMap = this.allocateByWeight(
                aggregateTask.getPlanStockQty(), sourceWeightMap, calculationScale);
        for (T sourceTask : taskGroup.getSourceTaskList()) {
            String sourceBusinessKey = sourceTask.getBusinessKey();
            sourceTask.setSourceRequiredQty(sourceWeightMap.get(sourceBusinessKey));
            sourceTask.setStockDeductQty(stockDeductAllocationMap.get(sourceBusinessKey));
            sourceTask.setBaseDemandQty(baseDemandAllocationMap.get(sourceBusinessKey));
            sourceTask.setMinStartAdjustQty(minStartAllocationMap.get(sourceBusinessKey));
            sourceTask.setTailRoundAdjustQty(roundAllocationMap.get(sourceBusinessKey));
            sourceTask.setPlanQty(finalPlanAllocationMap.get(sourceBusinessKey));
            sourceTask.setPlanStockQty(planStockAllocationMap.get(sourceBusinessKey));
            sourceTask.setTwoShiftDemandQty(aggregateTask.getTwoShiftDemandQty());
            sourceTask.setTwoShiftStockGapQty(aggregateTask.getTwoShiftStockGapQty());
            sourceTask.setTwoShiftStockCovered(aggregateTask.getTwoShiftStockCovered());
            if (twoShiftStockCovered) {
                sourceTask.setLossAddQty(BigDecimal.ZERO);
                sourceTask.setToolLimitAdjustQty(BigDecimal.ZERO);
                sourceTask.setToolOverflowQty(BigDecimal.ZERO);
                sourceTask.setCapacityAdjustQty(BigDecimal.ZERO);
                sourceTask.setPreLossPlanQty(BigDecimal.ZERO);
                sourceTask.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
            }
            sourceTask.setCalcFormulaDesc(formulaDesc);
            this.fillGroupFields(sourceTask, taskGroup);
            Map<String, Object> sourceEvidence = this.buildPlanGroupEvidence(taskGroup);
            sourceEvidence.put("sourceBusinessKey", sourceBusinessKey);
            sourceEvidence.put("sourceWeight", sourceWeightMap.get(sourceBusinessKey));
            sourceEvidence.put("allocatedPlanQty", sourceTask.getPlanQty());
            tracePort.traceSourceAllocation(context, sourceTask, aggregateTask, taskGroup, sourceEvidence);
        }
        tracePort.traceAggregateResult(context, aggregateTask, taskGroup,
                this.buildPlanGroupEvidence(taskGroup));
    }

    private void fillGroupFields(T task, G taskGroup) {
        task.setPlanGroupKey(taskGroup.getPlanGroupKey());
        task.setGroupSourceCount(taskGroup.getSourceTaskList().size());
        BigDecimal groupRequiredQty = Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                ? this.nvl(task.getFormingShutdownCloseOutDemandQty())
                : Boolean.TRUE.equals(task.getTwoShiftStockCovered())
                ? this.nvl(taskGroup.getGroupCurrentShiftDemandQty())
                .add(this.nvl(taskGroup.getGroupNextShiftDemandQty()))
                : this.nvl(taskGroup.getGroupCurrentShiftDemandQty())
                .add(this.nvl(taskGroup.getGroupGuardDemandQty()));
        task.setGroupRequiredQty(groupRequiredQty);
        task.setGroupBaseDemandQty(taskGroup.getGroupBaseDemandQty());
        task.setGroupMinStartAdjustQty(taskGroup.getGroupMinStartAdjustQty());
        task.setGroupRoundAdjustQty(taskGroup.getGroupRoundAdjustQty());
        task.setGroupFinalPlanQty(taskGroup.getGroupFinalPlanQty());
    }

    private Map<String, Object> buildPlanGroupEvidence(G taskGroup) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("planGroupKey", taskGroup.getPlanGroupKey());
        evidence.put("sourceCount", taskGroup.getSourceTaskList().size());
        evidence.put("groupCurrentShiftDemandQty", taskGroup.getGroupCurrentShiftDemandQty());
        evidence.put("groupNextShiftDemandQty", taskGroup.getGroupNextShiftDemandQty());
        evidence.put("groupGuardDemandQty", taskGroup.getGroupGuardDemandQty());
        evidence.put("groupBaseDemandQty", taskGroup.getGroupBaseDemandQty());
        evidence.put("groupMinStartAdjustQty", taskGroup.getGroupMinStartAdjustQty());
        evidence.put("groupRoundAdjustQty", taskGroup.getGroupRoundAdjustQty());
        evidence.put("groupFinalPlanQty", taskGroup.getGroupFinalPlanQty());
        evidence.put("tailDecisionMode", "LEGACY_TAIL_FLAG");
        return evidence;
    }

    private Map<String, BigDecimal> allocateByWeight(BigDecimal totalQty,
                                                      Map<String, BigDecimal> sourceWeightMap,
                                                      int calculationScale) {
        List<PlanQuantityAllocationItem> allocationItemList = sourceWeightMap.entrySet().stream()
                .map(entry -> new PlanQuantityAllocationItem(entry.getKey(), entry.getValue(), BigDecimal.ZERO))
                .collect(Collectors.toList());
        return PlanQuantityAllocationUtils.allocate(totalQty, allocationItemList, calculationScale).stream()
                .collect(Collectors.toMap(PlanQuantityAllocationItem::getSourceBusinessKey,
                        PlanQuantityAllocationItem::getAllocatedQty,
                        BigDecimal::add, LinkedHashMap::new));
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    public BigDecimal resolveGroupGuardDemandQty(List<T> sourceTaskList, BigDecimal currentShiftDemandQty) {
        return this.resolveGroupGuardDemandQty(sourceTaskList, currentShiftDemandQty,
                this.resolveGroupGuardWindowQtyMap(sourceTaskList));
    }

    private BigDecimal resolveGroupGuardDemandQty(List<T> sourceTaskList, BigDecimal currentShiftDemandQty,
                                                  Map<Integer, BigDecimal> guardWindowQtyMap) {
        boolean allNewSpec = this.isAllNewSpecGroup(sourceTaskList);
        if (!allNewSpec) {
            return sourceTaskList.stream().map(ScheduleTaskDraftModel::getGuardDemandQty)
                    .map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        Integer currentLogicalShiftOrder = this.resolveGroupCurrentLogicalShiftOrder(sourceTaskList,
                guardWindowQtyMap);
        if (currentLogicalShiftOrder != null) {
            return guardWindowQtyMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getKey() > currentLogicalShiftOrder)
                    .map(Map.Entry::getValue).map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal anchorQty = sourceTaskList.stream()
                .min(this.newSpecAnchorComparator())
                .map(ScheduleTaskDraftModel::getGuardDemandQty).map(this::nvl).orElse(BigDecimal.ZERO);
        return anchorQty.max(this.nvl(currentShiftDemandQty));
    }

    public Map<Integer, BigDecimal> resolveGroupGuardWindowQtyMap(List<T> sourceTaskList) {
        boolean allNewSpec = this.isAllNewSpecGroup(sourceTaskList);
        if (allNewSpec) {
            Map<Integer, BigDecimal> mergedWindowQtyMap = new LinkedHashMap<>();
            this.resolveNewSpecWindowAnchorList(sourceTaskList).forEach(anchorTask -> {
                Map<Integer, BigDecimal> sourceWindowQtyMap = anchorTask.getFormingGuardWindowQtyMap();
                if (sourceWindowQtyMap != null) {
                    sourceWindowQtyMap.forEach((logicalShiftOrder, quantity) ->
                            mergedWindowQtyMap.merge(logicalShiftOrder, this.nvl(quantity), BigDecimal::add));
                }
            });
            return mergedWindowQtyMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            BigDecimal::add, LinkedHashMap::new));
        }
        Map<Integer, BigDecimal> resultMap = new LinkedHashMap<>();
        sourceTaskList.stream().map(ScheduleTaskDraftModel::getFormingGuardWindowQtyMap)
                .filter(Objects::nonNull).forEach(window -> window.forEach(
                        (shift, quantity) -> resultMap.merge(shift, this.nvl(quantity), BigDecimal::add)));
        return resultMap;
    }

    /**
     * 解析计划组当班成型需求；新规格组按完整窗口最早逻辑班次取值，避免提前任务重复相加。
     *
     * @param sourceTaskList 来源任务
     * @param rawCurrentShiftDemandQty 原始来源任务当班需求合计
     * @param guardWindowQtyMap 已合并的成型需求窗口
     * @return 计划组当班成型需求
     */
    private BigDecimal resolveGroupCurrentShiftDemandQty(List<T> sourceTaskList,
                                                         BigDecimal rawCurrentShiftDemandQty,
                                                         Map<Integer, BigDecimal> guardWindowQtyMap) {
        if (!this.isAllNewSpecGroup(sourceTaskList) || guardWindowQtyMap == null
                || guardWindowQtyMap.isEmpty()) {
            return rawCurrentShiftDemandQty;
        }
        Integer currentLogicalShiftOrder = this.resolveGroupCurrentLogicalShiftOrder(sourceTaskList,
                guardWindowQtyMap);
        return currentLogicalShiftOrder == null || !guardWindowQtyMap.containsKey(currentLogicalShiftOrder)
                ? rawCurrentShiftDemandQty : this.nvl(guardWindowQtyMap.get(currentLogicalShiftOrder));
    }

    /**
     * 解析计划组当前成型逻辑班次。
     *
     * @param sourceTaskList 来源任务
     * @param guardWindowQtyMap 已合并的成型需求窗口
     * @return 当前成型逻辑班次；普通规格或空窗口返回空值
     */
    private Integer resolveGroupCurrentLogicalShiftOrder(List<T> sourceTaskList,
                                                          Map<Integer, BigDecimal> guardWindowQtyMap) {
        if (!this.isAllNewSpecGroup(sourceTaskList)) {
            return null;
        }
        if (guardWindowQtyMap == null || guardWindowQtyMap.isEmpty()) {
            return null;
        }
        Integer anchorLogicalShiftOrder = this.resolveNewSpecWindowAnchorList(sourceTaskList).stream()
                .map(ScheduleTaskDraftModel::getFormingLogicalShiftOrder)
                .filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
        return anchorLogicalShiftOrder == null
                ? this.resolveEarliestLogicalShiftOrder(guardWindowQtyMap) : anchorLogicalShiftOrder;
    }

    /**
     * 判断来源任务是否全部命中新规格提前排产。
     *
     * @param sourceTaskList 来源任务
     * @return true 表示全部为新规格任务
     */
    private boolean isAllNewSpecGroup(List<T> sourceTaskList) {
        return sourceTaskList != null && !sourceTaskList.isEmpty()
                && sourceTaskList.stream().allMatch(task -> task != null
                && task.getCommonNewSpecInfo() != null
                && task.getCommonNewSpecInfo().isNewSpecHit());
    }

    /**
     * 按成型来源键分组并为每个来源选择唯一窗口锚点。
     *
     * @param sourceTaskList 来源任务
     * @return 每个成型来源的窗口锚点任务
     */
    private List<T> resolveNewSpecWindowAnchorList(List<T> sourceTaskList) {
        Map<String, List<T>> sourceTaskMap = sourceTaskList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::resolveFormingSourceKey,
                        LinkedHashMap::new, Collectors.toList()));
        return sourceTaskMap.values().stream()
                .map(sourceTasks -> sourceTasks.stream().min(this.newSpecAnchorComparator()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构造新规格窗口锚点排序器。
     *
     * @return 按目标班次、来源班次和业务键排序的比较器
     */
    private Comparator<T> newSpecAnchorComparator() {
        return Comparator.comparing((T task) -> Optional.ofNullable(
                        task.getCommonNewSpecInfo().getNormalTargetShift()).orElse(Integer.MAX_VALUE))
                .thenComparing(task -> Optional.ofNullable(task.getSourceShiftOrder())
                        .orElse(Integer.MAX_VALUE))
                .thenComparing(task -> this.defaultString(task.getBusinessKey()));
    }

    /**
     * 解析运行态成型来源唯一键，并为旧测试或旧调用保留稳定降级键。
     *
     * @param task 来源任务
     * @return 成型来源唯一键
     */
    private String resolveFormingSourceKey(T task) {
        if (this.isNotBlank(task.getFormingSourceKey())) {
            return task.getFormingSourceKey();
        }
        if (this.isNotBlank(task.getSourceOrderNos())) {
            return task.getSourceOrderNos();
        }
        return this.defaultString(task.getBusinessKey());
    }

    /**
     * 解析窗口中的最早逻辑班次。
     *
     * @param guardWindowQtyMap 成型需求窗口
     * @return 最早逻辑班次；空窗口返回空值
     */
    private Integer resolveEarliestLogicalShiftOrder(Map<Integer, BigDecimal> guardWindowQtyMap) {
        return guardWindowQtyMap == null ? null : guardWindowQtyMap.keySet().stream()
                .filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
    }

    /**
     * 生成来源任务需求权重；新规格组按来源键和逻辑班次归属完整窗口。
     *
     * @param sourceTaskList 来源任务
     * @param aggregateTask 汇总任务
     * @param twoShiftStockCovered 是否命中两班库存覆盖
     * @return 来源业务键到需求权重的映射
     */
    private Map<String, BigDecimal> resolveSourceWeightMap(List<T> sourceTaskList, T aggregateTask,
                                                            boolean twoShiftStockCovered) {
        if (!this.isAllNewSpecGroup(sourceTaskList)
                || aggregateTask.getFormingGuardWindowQtyMap() == null
                || aggregateTask.getFormingGuardWindowQtyMap().isEmpty()
                || twoShiftStockCovered) {
            return sourceTaskList.stream().collect(Collectors.toMap(ScheduleTaskDraftModel::getBusinessKey,
                    sourceTask -> Boolean.TRUE.equals(aggregateTask.getFormingShutdownCloseOutFlag())
                            ? this.nvl(sourceTask.getFormingShutdownCloseOutDemandQty())
                            : this.nvl(sourceTask.getCurrentShiftDemandQty()).add(this.nvl(
                            twoShiftStockCovered ? sourceTask.getNextShiftDemandQty()
                                    : sourceTask.getGuardDemandQty())),
                    BigDecimal::add, LinkedHashMap::new));
        }
        Map<String, BigDecimal> sourceWeightMap = sourceTaskList.stream()
                .collect(Collectors.toMap(ScheduleTaskDraftModel::getBusinessKey,
                        sourceTask -> BigDecimal.ZERO, BigDecimal::add, LinkedHashMap::new));
        Map<String, List<T>> sourceTaskMap = sourceTaskList.stream()
                .collect(Collectors.groupingBy(this::resolveFormingSourceKey,
                        LinkedHashMap::new, Collectors.toList()));
        sourceTaskMap.values().forEach(sourceTasks -> {
            T anchorTask = sourceTasks.stream().min(this.newSpecAnchorComparator()).orElse(null);
            if (anchorTask == null || anchorTask.getFormingGuardWindowQtyMap() == null) {
                return;
            }
            anchorTask.getFormingGuardWindowQtyMap().forEach((logicalShiftOrder, quantity) -> {
                T targetTask = sourceTasks.stream()
                        .filter(sourceTask -> Objects.equals(sourceTask.getFormingLogicalShiftOrder(),
                                logicalShiftOrder))
                        .findFirst().orElse(anchorTask);
                sourceWeightMap.merge(targetTask.getBusinessKey(), this.nvl(quantity), BigDecimal::add);
            });
        });
        return sourceWeightMap;
    }

    public Map<Integer, BigDecimal> resolveGroupGuardWindowHoursMap(
            C context, Map<Integer, BigDecimal> guardWindowQtyMap, int maxShiftOrder) {
        Map<Integer, BigDecimal> resultMap = new LinkedHashMap<>();
        if (guardWindowQtyMap == null || guardWindowQtyMap.isEmpty()) {
            return resultMap;
        }
        guardWindowQtyMap.keySet().stream().filter(Objects::nonNull).forEach(logicalShift -> {
            int mappedShift = this.mapGuardLogicalShiftOrder(logicalShift, maxShiftOrder);
            BigDecimal shiftHours = context == null || context.getShiftHoursMap() == null
                    ? null : context.getShiftHoursMap().get(mappedShift);
            resultMap.put(logicalShift, shiftHours);
        });
        return resultMap;
    }

    public int mapGuardLogicalShiftOrder(int logicalShiftOrder, int maxShiftOrder) {
        return logicalShiftOrder <= maxShiftOrder ? logicalShiftOrder
                : ((logicalShiftOrder - maxShiftOrder - 1) % 3) + 1;
    }

    public String mergeDistinctText(List<T> sourceTaskList, Function<T, String> valueGetter) {
        String merged = sourceTaskList.stream().map(valueGetter).filter(this::isNotBlank)
                .flatMap(value -> Arrays.stream(value.split("[,，]"))).map(String::trim)
                .filter(this::isNotBlank).distinct().collect(Collectors.joining(","));
        return this.isNotBlank(merged) ? merged : null;
    }

    public boolean quantityEquals(BigDecimal first, BigDecimal second) {
        return this.nvl(first).compareTo(this.nvl(second)) == 0;
    }

    public Date offsetHours(Date source, BigDecimal hours, long millisPerHour) {
        long offsetMillis = hours.multiply(BigDecimal.valueOf(millisPerHour))
                .setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        return new Date(source.getTime() + offsetMillis);
    }

    public String displayQuantity(BigDecimal quantity) {
        return quantity == null ? "未计算" : quantity.stripTrailingZeros().toPlainString();
    }

    public BigDecimal resolveOriginalCurrentShiftDemandQty(T task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        return task.getOriginalCurrentShiftDemandQty() == null
                ? this.nvl(task.getCurrentShiftDemandQty()) : task.getOriginalCurrentShiftDemandQty();
    }

    public void applyDemandQtyResult(T task, ScheduleDemandQtyResultModel result) {
        if (result == null) {
            return;
        }
        task.setCurrentShiftDemandQty(result.getCurrentShiftDemandQty());
        task.setGuardDemandQty(result.getGuardDemandQty());
        task.setRollingStockQty(result.getRollingStockQty());
        task.setCurrentShiftStockGapQty(result.getCurrentShiftStockGapQty());
        task.setStockGapQty(result.getStockGapQty());
        task.setDemandQty(result.getDemandQty());
        task.setGuardShiftCount(result.getGuardShiftCount());
        task.setSupplyHours(result.getSupplyHours());
    }

    public Map<String, Object> buildTwoShiftCoverageEvidence(T task) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        boolean newSpecBypass = task.getCommonNewSpecInfo() != null && task.getCommonNewSpecInfo().isNewSpecHit();
        boolean experimentBypass = task.getCommonExperimentSpecInfo() != null
                && task.getCommonExperimentSpecInfo().isExperimentSpecHit();
        evidence.put("applicable", !newSpecBypass && !experimentBypass && task.getPlanQty() != null);
        evidence.put("bypassReason", newSpecBypass ? "NEW_SPEC_ADVANCE"
                : experimentBypass ? "EXPERIMENT_SPEC_ADVANCE_WINDOW" : null);
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("nextShiftDemandQty", task.getNextShiftDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("twoShiftDemandQty", task.getTwoShiftDemandQty());
        evidence.put("twoShiftStockGapQty", task.getTwoShiftStockGapQty());
        evidence.put("stockCovered", task.getTwoShiftStockCovered());
        evidence.put("twoShiftLeadTask", task.getTwoShiftLeadTask());
        evidence.put("finalPlanQty", task.getPlanQty());
        return evidence;
    }

    /**
     * 判断任务是否为预置计划任务。
     *
     * @param task 待判断任务
     * @return true 表示任务属于预置计划组
     */
    private boolean isPresetPlanTask(T task) {
        return task != null && task.getPlanGroupKey() != null
                && task.getPlanGroupKey().contains("|PRESET|");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
