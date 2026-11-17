package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationItem;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationUtils;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleTailQuotaCollectResult;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleTailQuotaLedger;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleTailQuotaSourceCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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

    private static final Logger LOG = LoggerFactory.getLogger(PlanCalculationEngine.class);

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
            policy.beforeCalculatePlanQty(context, shiftTaskList);
            List<T> beforeSortTaskList = new ArrayList<>(shiftTaskList);
            int sortStartIndex = orderIndex + 1;
            policy.sortPlanCalcShiftTasks(context, shiftTaskList);
            for (T task : shiftTaskList) {
                task.setPlanCalcOrderIndex(++orderIndex);
                orderedTaskList.add(task);
            }
            policy.recordPlanCalcSortSnapshot(context, beforeSortTaskList, new ArrayList<>(shiftTaskList),
                    sortStartIndex, orderIndex);
            for (T task : shiftTaskList) {
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
        this.prepareTailQuota(context, policy);
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
        this.applyTailQuotaAvailableQty(context, shiftTaskList, policy);
        try {
            // 拆分逐班重算，区分需求准备、排序及每条任务计划计算的累计开销。
            long phaseStartNanos = System.nanoTime();
            policy.prepareShiftDemandAndSupply(context, shiftTaskList, stockForecastMap, planningStockMap,
                    demandStrategy, demandAlgorithmCode);
            long demandNanos = System.nanoTime() - phaseStartNanos;
            phaseStartNanos = System.nanoTime();
            policy.beforeCalculatePlanQty(context, shiftTaskList);
            long beforeNanos = System.nanoTime() - phaseStartNanos;
            List<T> beforeSortTaskList = new ArrayList<>(shiftTaskList);
            phaseStartNanos = System.nanoTime();
            policy.sortPlanCalcShiftTasks(context, shiftTaskList);
            long sortNanos = System.nanoTime() - phaseStartNanos;
            int orderIndex = this.resolveNextPlanCalcOrderIndex(context);
            int sortStartIndex = orderIndex + 1;
            BigDecimal remainingToolQty = context.getCurrentAvailableToolQty();
            for (T task : shiftTaskList) {
                task.setPlanCalcOrderIndex(++orderIndex);
                task.setBaseSortIndex(orderIndex);
                if (context.isTwoPhaseEnabled()) {
                    task.setTwoPhaseDuplicateTask(false);
                }
                if (!this.isPresetPlanTask(task)) {
                    task.setPlanQty(null);
                }
            }
            phaseStartNanos = System.nanoTime();
            policy.recordPlanCalcSortSnapshot(context, beforeSortTaskList, new ArrayList<>(shiftTaskList),
                    sortStartIndex, orderIndex);
            long snapshotNanos = System.nanoTime() - phaseStartNanos;
            long calculationNanos = 0L;
            long maxTaskNanos = 0L;
            String slowestBusinessKey = null;
            int calculatedCount = 0;
            phaseStartNanos = System.nanoTime();
            for (T task : shiftTaskList) {
                String productCode = task.getProcessCode();
                if (productCode == null || productCode.trim().isEmpty()) {
                    continue;
                }
                // 同一产品同班只计算首个非预置任务，保留原有计划组去重语义，避免局部库存被重复扣减。
                if (!this.isPresetPlanTask(task) && !recalculatedProductCodeSet.add(productCode)) {
                    if (context.isTwoPhaseEnabled()) {
                        task.setTwoPhaseDuplicateTask(true);
                    }
                    task.setStockCoverageStockGapQty(null);
                    task.setStockCoverageCovered(null);
                    // 同班重复任务不参与二次库存判断，清除历史兼容标记避免日志误判为覆盖。
                    task.setTwoShiftStockCovered(null);
                    task.setPreLossPlanQty(BigDecimal.ZERO);
                    task.setLossAddQty(BigDecimal.ZERO);
                    task.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
                    task.setPlanQty(BigDecimal.ZERO);
                    task.setPlanStockQty(planningStockMap.getOrDefault(productCode,
                            this.nvl(task.getRollingStockQty())));
                    continue;
                }
                long taskStartNanos = System.nanoTime();
                remainingToolQty = policy.calculatePlanQtyForTask(context, task, stockForecastMap,
                        planningStockMap, remainingToolQty, planStrategy, planStrategyCode, demandAlgorithmCode);
                long taskNanos = System.nanoTime() - taskStartNanos;
                calculationNanos += taskNanos;
                calculatedCount++;
                if (taskNanos > maxTaskNanos) {
                    maxTaskNanos = taskNanos;
                    slowestBusinessKey = task.getBusinessKey();
                }
                if (productCode != null && !productCode.trim().isEmpty()) {
                    planningStockMap.put(productCode, this.nvl(task.getPlanStockQty()));
                }
            }
            long loopNanos = System.nanoTime() - phaseStartNanos;
            context.setCurrentAvailableToolQty(remainingToolQty);
            String batchNo = context instanceof ScheduleContextModel
                    ? ((ScheduleContextModel<?, ?, ?, ?, ?>) context).getBatchNo() : null;
            LOG.info("[SCHEDULE_PERF] 逐班计划重算内部耗时 batchNo={}, factoryCode={}, scheduleDate={}, shiftOrder={}, taskCount={}, calculatedCount={}, demandMs={}, beforeMs={}, sortAndLogMs={}, snapshotMs={}, taskLoopMs={}, calculateMs={}, slowestTask={}, slowestMs={}",
                    batchNo, context.getFactoryCode(), context.getScheduleDate(), shiftTaskList.get(0).getShiftOrder(),
                    shiftTaskList.size(), calculatedCount, demandNanos / 1000000, beforeNanos / 1000000,
                    sortNanos / 1000000, snapshotNanos / 1000000, loopNanos / 1000000,
                    calculationNanos / 1000000, slowestBusinessKey, maxTaskNanos / 1000000);
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
     * 初始化当日成型总余量额度台账。
     *
     * <p>必须在汇总任务之后执行：{@link #aggregateTaskDrafts} 在已有汇总组时会提前返回，
     * 把初始化放进去会被跳过。幂等标记独立保存，并在工作区复制时同步，
     * 避免两阶段重入把已扣减的额度冲回总余量。</p>
     *
     * @param context 排程上下文
     * @param policy  领域差异策略
     */
    private void prepareTailQuota(C context, PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        if (context.isTailQuotaPrepared()) {
            return;
        }
        context.setTailQuotaPrepared(true);
        List<T> sourceTaskList = context.getSourceTaskDraftList();
        if (sourceTaskList == null || sourceTaskList.isEmpty()) {
            return;
        }
        ScheduleTailQuotaCollectResult collectResult = new ScheduleTailQuotaSourceCollector()
                .collect(context.getFactoryCode(), context.getScheduleDate(), sourceTaskList,
                        policy::resolveTailQuotaRemainQty, policy::resolveTailQuotaUnitLength);
        context.setTailQuotaLedgerMap(new LinkedHashMap<>(collectResult.getLedgerMap()));
        if (!collectResult.getWarningList().isEmpty()) {
            LOG.warn("成型总余量额度采集告警 batchNo={}, warnings={}",
                    context.getFactoryCode(), collectResult.getWarningList());
        }
    }

    /**
     * 把本班任务可用的成型总余量额度下发到任务运行态。
     *
     * <p>额度为空表示该产品未启用额度，下游按未启用处理，不做任何封顶。</p>
     *
     * @param context       排程上下文
     * @param shiftTaskList 当前班次任务
     * @param policy        领域差异策略
     */
    private void applyTailQuotaAvailableQty(C context, List<T> shiftTaskList,
                                            PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        Map<String, ScheduleTailQuotaLedger> ledgerMap = context.getTailQuotaLedgerMap();
        for (T task : shiftTaskList) {
            if (task == null) {
                continue;
            }
            if (ledgerMap == null || ledgerMap.isEmpty()) {
                task.setTailQuotaAvailableQty(null);
                continue;
            }
            BigDecimal unitLength = policy.resolveTailQuotaUnitLength(task);
            String bucketKey = ScheduleTailQuotaSourceCollector.resolveBucketKey(context.getFactoryCode(),
                    context.getScheduleDate(), ScheduleTailQuotaSourceCollector.resolveProductCode(task),
                    unitLength);
            ScheduleTailQuotaLedger ledger = ledgerMap.get(bucketKey);
            task.setTailQuotaAvailableQty(ledger == null ? null : ledger.availableFor(task.getBusinessKey()));
        }
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
        if (context.isTwoPhaseEnabled()) {
            originalTaskList.stream().filter(Objects::nonNull)
                    .forEach(task -> this.freezeTwoPhaseInput(context, task));
        }
        SchedulePlanTailCalculator.prepareTailMetadata(originalTaskList);
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
            SchedulePlanTailCalculator.mergeRuntimeMetadata(aggregateTask, groupSourceList);
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
            boolean coverageWindowAvailable = groupSourceList.stream()
                    .allMatch(sourceTask -> sourceTask.getStockCoverageDemandQty() != null);
            BigDecimal stockCoverageDemandQty = coverageWindowAvailable
                    ? this.sum(groupSourceList, ScheduleTaskDraftModel::getStockCoverageDemandQty) : null;
            ScheduleStockCoverageWindowModel stockCoverageWindow = this.mergeStockCoverageWindow(groupSourceList,
                    context.isTwoPhaseEnabled());
            BigDecimal guardDemandQty = this.resolveGroupGuardDemandQty(groupSourceList,
                    rawCurrentShiftDemandQty, guardWindowQtyMap);
            Integer formingLogicalShiftOrder = this.resolveGroupCurrentLogicalShiftOrder(groupSourceList,
                    guardWindowQtyMap);

            if (context.isTwoPhaseEnabled() && !formingShutdownCloseOut
                    && groupSourceList.stream().allMatch(source -> source.getFormingLogicalShiftOrder() != null)) {
                // 前移后同班的多个派生草稿按原消费事件合并，保留窗口并集而非仅保留一个锚点。
                ScheduleTwoPhaseSourceWindows windows = new ScheduleTwoPhaseSourceWindows();
                guardWindowQtyMap = windows.fullWindow(groupSourceList);
                currentShiftDemandQty = windows.baseDemand(groupSourceList, false);
                nextShiftDemandQty = windows.baseDemand(groupSourceList, true)
                        .subtract(currentShiftDemandQty).max(BigDecimal.ZERO);
                guardDemandQty = guardWindowQtyMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .subtract(currentShiftDemandQty).max(BigDecimal.ZERO);
                formingLogicalShiftOrder = groupSourceList.stream()
                        .map(ScheduleTaskDraftModel::getFormingLogicalShiftOrder).min(Integer::compareTo).orElse(null);
            }
            if (context.isTwoPhaseEnabled() && !formingShutdownCloseOut) {
                // 覆盖使用来源配置窗口；来源窗口缺失时明确回退两班，确保数值与窗口标签一致。
                stockCoverageDemandQty = stockCoverageWindow == null
                        ? currentShiftDemandQty.add(nextShiftDemandQty)
                        : this.sumStockCoverageWindow(stockCoverageWindow);
            }

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
            aggregateTask.setStockCoverageDemandQty(stockCoverageDemandQty);
            aggregateTask.setStockCoverageWindow(stockCoverageWindow);
            aggregateTask.setGuardDemandQty(guardDemandQty);
            aggregateTask.setFormingGuardWindowQtyMap(guardWindowQtyMap);
            if (formingLogicalShiftOrder != null) {
                aggregateTask.setFormingLogicalShiftOrder(formingLogicalShiftOrder);
            }
            aggregateTask.setFormingGuardWindowHoursMap(
                    this.resolveGroupGuardWindowHoursMap(context, guardWindowQtyMap, 6));
            policy.applyTailDecision(aggregateTask, groupSourceList);
            aggregateTask.setDemandQty(null);
            if (groupSourceList.size() > 1) {
                aggregateTask.setPlanQty(null);
            }

            G taskGroup = policy.createPlanTaskGroup();
            if (context.isTwoPhaseEnabled() && groupSourceList.size() > 1) {
                aggregateTask.setInputSnapshotKey(null);
                this.freezeTwoPhaseInput(context, aggregateTask);
            }
            taskGroup.setPlanGroupKey(planGroupKey);
            taskGroup.setAggregateTask(aggregateTask);
            taskGroup.setSourceTaskList(sourceSnapshotList);
            taskGroup.setGroupCurrentShiftDemandQty(currentShiftDemandQty);
            taskGroup.setGroupNextShiftDemandQty(nextShiftDemandQty);
            taskGroup.setGroupStockCoverageDemandQty(stockCoverageDemandQty);
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

    /**
     * 构建任务所属计划组的唯一业务键。
     */
    private String buildPlanGroupKey(C context, T task) {
        String dateText = context.getScheduleDate() == null ? null
                : context.getScheduleDate().toString();
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

    /** 对任务列表中的指定数值字段求和。 */
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
     * @param policy           领域计划量策略，用于清理覆盖命中的专有调整量
     */
    public void applyPlanGroupResult(C context, T aggregateTask, int calculationScale,
                                     String formulaDesc, PlanGroupResultTracePort<C, T, G> tracePort,
                                     PlanCalculationPolicy<C, T, F, G, P, D> policy) {
        G taskGroup = context.getPlanTaskGroupMap().get(aggregateTask.getPlanGroupKey());
        if (taskGroup == null || taskGroup.getSourceTaskList() == null
                || taskGroup.getSourceTaskList().isEmpty()) {
            return;
        }
        boolean stockCoverageCovered = aggregateTask.getStockCoverageCovered() == null
                ? Boolean.TRUE.equals(aggregateTask.getTwoShiftStockCovered())
                : Boolean.TRUE.equals(aggregateTask.getStockCoverageCovered());
        Map<String, BigDecimal> sourceWeightMap = this.resolveSourceWeightMap(taskGroup.getSourceTaskList(),
                aggregateTask, stockCoverageCovered);
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
        Map<String, BigDecimal> postTailBlockedAllocationMap = this.allocateByWeight(
                aggregateTask.getPostTailBlockedQty(), sourceWeightMap, calculationScale);
        for (T sourceTask : taskGroup.getSourceTaskList()) {
            String sourceBusinessKey = sourceTask.getBusinessKey();
            boolean sourceHasPostTailBlockedReason = SchedulePostTailTaskRuleApplier
                    .POST_TAIL_BLOCKED_REASON_CODE.equals(sourceTask.getUnplannedReasonCode());
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
            sourceTask.setStockCoverageStockGapQty(aggregateTask.getStockCoverageStockGapQty());
            sourceTask.setStockCoverageCovered(aggregateTask.getStockCoverageCovered());
            sourceTask.setPostTailShiftParameter(aggregateTask.getPostTailShiftParameter());
            sourceTask.setPostTailDecision(aggregateTask.getPostTailDecision());
            sourceTask.setPostTailCloseOutState(aggregateTask.getPostTailCloseOutState());
            sourceTask.setPostTailShiftBlocked(aggregateTask.getPostTailShiftBlocked());
            sourceTask.setPostTailBlockedQty(postTailBlockedAllocationMap.get(sourceBusinessKey));
            sourceTask.setPostTailSkippedShiftOrders(aggregateTask.getPostTailSkippedShiftOrders());
            if (aggregateTask.getUnplannedReasonCode() != null
                    && !aggregateTask.getUnplannedReasonCode().trim().isEmpty()) {
                sourceTask.setMachineCode(null);
                sourceTask.setUnplannedReasonCode(aggregateTask.getUnplannedReasonCode());
                sourceTask.setUnplannedReasonDesc(aggregateTask.getUnplannedReasonDesc());
            } else if (sourceHasPostTailBlockedReason
                    && this.nvl(aggregateTask.getPostTailBlockedQty()).compareTo(BigDecimal.ZERO) <= 0) {
                // 仅在没有禁排缺口时清除来源任务的旧未排原因。
                sourceTask.setMachineCode(null);
                sourceTask.setUnplannedReasonCode(null);
                sourceTask.setUnplannedReasonDesc(null);
            }
            if (stockCoverageCovered) {
                sourceTask.setLossAddQty(BigDecimal.ZERO);
                sourceTask.setToolLimitAdjustQty(BigDecimal.ZERO);
                sourceTask.setToolOverflowQty(BigDecimal.ZERO);
                sourceTask.setMinStartAdjustQty(BigDecimal.ZERO);
                sourceTask.setTailRoundAdjustQty(BigDecimal.ZERO);
                sourceTask.setCapacityAdjustQty(BigDecimal.ZERO);
                sourceTask.setPreLossPlanQty(BigDecimal.ZERO);
                sourceTask.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
                policy.clearAdditionalPlanAdjustments(sourceTask);
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

    /** 将计划组汇总字段回填到任务对象。 */
    private void fillGroupFields(T task, G taskGroup) {
        task.setPlanGroupKey(taskGroup.getPlanGroupKey());
        task.setGroupSourceCount(taskGroup.getSourceTaskList().size());
        boolean stockCoverageCovered = task.getStockCoverageCovered() == null
                ? Boolean.TRUE.equals(task.getTwoShiftStockCovered())
                : Boolean.TRUE.equals(task.getStockCoverageCovered());
        BigDecimal groupRequiredQty = Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag())
                ? this.nvl(task.getFormingShutdownCloseOutDemandQty())
                : stockCoverageCovered
                ? this.nvl(taskGroup.getGroupStockCoverageDemandQty() == null
                ? task.getStockCoverageDemandQty() : taskGroup.getGroupStockCoverageDemandQty())
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
        evidence.put("groupStockCoverageDemandQty", taskGroup.getGroupStockCoverageDemandQty());
        evidence.put("groupGuardDemandQty", taskGroup.getGroupGuardDemandQty());
        evidence.put("groupBaseDemandQty", taskGroup.getGroupBaseDemandQty());
        evidence.put("groupMinStartAdjustQty", taskGroup.getGroupMinStartAdjustQty());
        evidence.put("groupRoundAdjustQty", taskGroup.getGroupRoundAdjustQty());
        evidence.put("groupFinalPlanQty", taskGroup.getGroupFinalPlanQty());
        evidence.put("tailDecisionMode", "PRODUCT_CODE_NORMAL_FIRST");
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

    /** 空字符串兜底转换。 */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 冻结两阶段来源；缺失元数据时保留完整任务证据并重抛原异常。
     *
     * @param context 排程上下文
     * @param task    待冻结的来源或汇总任务
     * @throws RuntimeException 来源或消费归属不合法时原样抛出
     */
    private void freezeTwoPhaseInput(C context, T task) {
        try {
            new ScheduleTwoPhaseRequestPreparer().freezeInput(task, context.getBatchNo(),
                    context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException exception) {
            String traceId = context instanceof TaskChainContextAccess
                    ? ((TaskChainContextAccess<?>) context).getTraceId() : org.slf4j.MDC.get("traceId");
            LOG.error("[TWO_PHASE_INPUT_INVALID] batchNo={}, traceId={}, businessKey={}, processCode={}, "
                            + "formingSourceKey={}, sourceShift={}, targetShift={}, newSpec={}, experiment={}, "
                            + "normalTargetShift={}", context.getBatchNo(), traceId, task.getBusinessKey(),
                    task.getProcessCode(), task.getFormingSourceKey(), task.getSourceShiftOrder(), task.getShiftOrder(),
                    task.getCommonNewSpecInfo() != null && task.getCommonNewSpecInfo().isNewSpecHit(),
                    task.getCommonExperimentSpecInfo() != null && task.getCommonExperimentSpecInfo().isExperimentSpecHit(),
                    task.getCommonExperimentSpecInfo() != null && task.getCommonExperimentSpecInfo().isExperimentSpecHit()
                            ? task.getCommonExperimentSpecInfo().getNormalTargetShift()
                            : task.getCommonNewSpecInfo() == null ? null
                            : task.getCommonNewSpecInfo().getNormalTargetShift(), exception);
            throw exception;
        }
    }

    /**
     * 解析计划组内的保证需求量（新规格组按逻辑班次窗口去重求和，普通组直接累加）。
     *
     * @param sourceTaskList        组内来源任务列表
     * @param currentShiftDemandQty 当班需求量
     * @return 组级保证需求量（米）
     */
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
     * @param stockCoverageCovered 是否命中当前适用的库存覆盖窗口
     * @return 来源业务键到需求权重的映射
     */
    private Map<String, BigDecimal> resolveSourceWeightMap(List<T> sourceTaskList, T aggregateTask,
                                                           boolean stockCoverageCovered) {
        if (aggregateTask.getInputSnapshotKey() != null && sourceTaskList.stream().allMatch(source ->
                source.getFormingLogicalShiftOrder() != null && source.getFormingGuardWindowQtyMap() != null
                        && !source.getFormingGuardWindowQtyMap().isEmpty())) {
            return new ScheduleTwoPhaseSourceWindows().sourceWeights(sourceTaskList);
        }
        if (!this.isAllNewSpecGroup(sourceTaskList)
                || aggregateTask.getFormingGuardWindowQtyMap() == null
                || aggregateTask.getFormingGuardWindowQtyMap().isEmpty()
                || stockCoverageCovered) {
            return sourceTaskList.stream().collect(Collectors.toMap(ScheduleTaskDraftModel::getBusinessKey,
                    sourceTask -> Boolean.TRUE.equals(aggregateTask.getFormingShutdownCloseOutFlag())
                            ? this.nvl(sourceTask.getFormingShutdownCloseOutDemandQty())
                            : stockCoverageCovered
                            ? this.nvl(sourceTask.getStockCoverageDemandQty() == null
                            ? this.nvl(sourceTask.getCurrentShiftDemandQty()).add(
                            this.nvl(sourceTask.getNextShiftDemandQty()))
                            : sourceTask.getStockCoverageDemandQty())
                            : this.nvl(sourceTask.getCurrentShiftDemandQty()).add(this.nvl(
                            sourceTask.getGuardDemandQty())),
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

    /**
     * 合并计划组来源任务的库存覆盖窗口。
     *
     * @param sourceTaskList 来源任务快照
     * @param deduplicateSourceWindows 是否按来源和原消费班合并两阶段窗口
     * @return 合并后的库存覆盖窗口；来源缺少窗口时返回 null
     */
    private ScheduleStockCoverageWindowModel mergeStockCoverageWindow(List<T> sourceTaskList,
                                                                      boolean deduplicateSourceWindows) {
        if (sourceTaskList == null || sourceTaskList.isEmpty()
                || sourceTaskList.stream().anyMatch(task -> task.getStockCoverageWindow() == null)) {
            return null;
        }
        if (!deduplicateSourceWindows) {
            return this.mergeLegacyStockCoverageWindow(sourceTaskList);
        }
        return this.mergeTwoPhaseStockCoverageWindows(sourceTaskList);
    }

    /**
     * 保留非两阶段入口既有的库存覆盖窗口聚合口径。
     *
     * @param sourceTaskList 来源任务
     * @return 按旧兼容规则合并的窗口
     */
    private ScheduleStockCoverageWindowModel mergeLegacyStockCoverageWindow(List<T> sourceTaskList) {
        ScheduleStockCoverageWindowModel first = sourceTaskList.get(0).getStockCoverageWindow();
        ScheduleStockCoverageWindowModel target = first.copy();
        Map<Integer, BigDecimal> demandMap = new LinkedHashMap<>();
        sourceTaskList.forEach(sourceTask -> sourceTask.getStockCoverageWindow().getDemandQtyByOffset()
                .forEach((offset, quantity) -> demandMap.merge(offset, this.nvl(quantity), BigDecimal::add)));
        target.setDemandQtyByOffset(demandMap);
        BigDecimal totalDemandQty = demandMap.values().stream().map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer extrapolatedShiftCount = first.getExtrapolatedShiftCount();
        if (extrapolatedShiftCount != null && extrapolatedShiftCount > 0) {
            BigDecimal extrapolatedDemandQty = sourceTaskList.stream()
                    .map(task -> task.getStockCoverageWindow().getExtrapolatedDemandQtyPerShift())
                    .map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add);
            target.setExtrapolatedStartOffset(first.getExtrapolatedStartOffset());
            target.setExtrapolatedShiftCount(extrapolatedShiftCount);
            target.setExtrapolatedDemandQtyPerShift(extrapolatedDemandQty);
            totalDemandQty = totalDemandQty.add(extrapolatedDemandQty.multiply(
                    BigDecimalUtils.valueOf(extrapolatedShiftCount)));
        }
        target.setTotalDemandQty(totalDemandQty);
        return target;
    }

    /**
     * 将来源窗口按原消费班对齐，同一来源同一消费班只保留一次。
     *
     * @param sourceTaskList 同一生产计划组的来源任务
     * @return 合并后的完整窗口；班数不一致时返回空值
     */
    private ScheduleStockCoverageWindowModel mergeTwoPhaseStockCoverageWindows(List<T> sourceTaskList) {
        ScheduleStockCoverageWindowModel first = sourceTaskList.get(0).getStockCoverageWindow();
        Integer shiftCount = first.getShiftCount();
        if (shiftCount == null || shiftCount < 1 || sourceTaskList.stream()
                .anyMatch(task -> !Objects.equals(shiftCount, task.getStockCoverageWindow().getShiftCount()))) {
            return null;
        }
        if (sourceTaskList.size() == 1) {
            return first.copy();
        }
        int firstShift = sourceTaskList.stream().map(this::resolveStockCoverageWindowStartShift)
                .min(Integer::compareTo).orElse(0);
        ScheduleStockCoverageWindowModel target = first.copy();
        Map<Integer, BigDecimal> demandMap = new LinkedHashMap<>();
        int finiteLength = this.resolveStockCoverageFinitePrefixLength(sourceTaskList, firstShift, shiftCount);
        for (int offset = 0; offset < finiteLength; offset++) {
            demandMap.put(offset, this.mergeStockCoverageDemandAtOffset(sourceTaskList, firstShift, offset));
        }
        target.setDemandQtyByOffset(demandMap);
        int extrapolatedCount = shiftCount - finiteLength;
        BigDecimal extrapolatedDemand = extrapolatedCount == 0 ? BigDecimal.ZERO
                : this.mergeStockCoverageDemandAtOffset(sourceTaskList, firstShift, finiteLength);
        // 连续外推段保持紧凑表示，避免配置班数较大时逐班创建大量对象。
        target.setExtrapolatedStartOffset(extrapolatedCount == 0 ? null : finiteLength);
        target.setExtrapolatedShiftCount(extrapolatedCount == 0 ? null : extrapolatedCount);
        target.setExtrapolatedDemandQtyPerShift(extrapolatedCount == 0 ? null : extrapolatedDemand);
        BigDecimal total = demandMap.values().stream().map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add);
        target.setTotalDemandQty(total.add(extrapolatedDemand.multiply(BigDecimalUtils.valueOf(extrapolatedCount))));
        return target;
    }

    /**
     * 确定合并窗口中需逐班表示的前缀，后续需求为稳定外推值。
     *
     * @param tasks      来源任务
     * @param firstShift 组内最早来源班次
     * @param shiftCount 配置覆盖班数
     * @return 逐班明细长度
     */
    private int resolveStockCoverageFinitePrefixLength(List<T> tasks, int firstShift, int shiftCount) {
        long finiteLength = 0;
        for (T task : tasks) {
            long startOffset = (long) this.resolveStockCoverageWindowStartShift(task) - firstShift;
            ScheduleStockCoverageWindowModel window = task.getStockCoverageWindow();
            if (window.getDemandQtyByOffset() != null) {
                for (Integer offset : window.getDemandQtyByOffset().keySet()) {
                    if (offset != null && offset >= 0 && offset < shiftCount) {
                        finiteLength = Math.max(finiteLength, startOffset + offset + 1);
                    }
                }
            }
            Integer extrapolatedStart = window.getExtrapolatedStartOffset();
            Integer extrapolatedCount = window.getExtrapolatedShiftCount();
            if (extrapolatedStart != null && extrapolatedCount != null && extrapolatedCount > 0) {
                long start = startOffset + extrapolatedStart;
                long end = start + extrapolatedCount;
                finiteLength = Math.max(finiteLength, start);
                if (end < shiftCount) {
                    finiteLength = Math.max(finiteLength, end);
                }
            }
        }
        return (int) Math.min(shiftCount, Math.max(0, finiteLength));
    }

    /**
     * 对齐指定班次并合并需求，同来源取最大值，不同来源相加。
     *
     * @param tasks       来源任务
     * @param firstShift  组内最早来源班次
     * @param groupOffset 组窗口内的班次偏移
     * @return 该班次的去重需求
     */
    private BigDecimal mergeStockCoverageDemandAtOffset(List<T> tasks, int firstShift, int groupOffset) {
        Map<String, BigDecimal> demandBySource = new LinkedHashMap<>();
        tasks.forEach(task -> {
            int sourceOffset = groupOffset - (this.resolveStockCoverageWindowStartShift(task) - firstShift);
            if (sourceOffset >= 0 && sourceOffset < task.getStockCoverageWindow().getShiftCount()) {
                demandBySource.merge(this.resolveStockCoverageSourceWindowKey(task),
                        this.resolveStockCoverageDemandAtOffset(task.getStockCoverageWindow(), sourceOffset),
                        BigDecimal::max);
            }
        });
        return demandBySource.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 解析一个覆盖窗口在指定偏移班的需求量。
     *
     * @param window 库存覆盖窗口
     * @param offset 从当前班开始的偏移量
     * @return 指定班需求量；窗口未记录该班时返回零
     */
    private BigDecimal resolveStockCoverageDemandAtOffset(ScheduleStockCoverageWindowModel window, int offset) {
        Map<Integer, BigDecimal> demandMap = window.getDemandQtyByOffset();
        if (demandMap != null && demandMap.containsKey(offset)) {
            return this.nvl(demandMap.get(offset));
        }
        Integer extrapolatedStartOffset = window.getExtrapolatedStartOffset();
        Integer extrapolatedShiftCount = window.getExtrapolatedShiftCount();
        if (extrapolatedStartOffset != null && extrapolatedShiftCount != null
                && offset >= extrapolatedStartOffset && offset < extrapolatedStartOffset + extrapolatedShiftCount) {
            return this.nvl(window.getExtrapolatedDemandQtyPerShift());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 解析来源窗口去重键，沿用来源日期、来源键及工序口径。
     *
     * @param task 来源任务
     * @return 用于区分不同来源的稳定键
     */
    private String resolveStockCoverageSourceWindowKey(T task) {
        String sourceKey = task.getFormingSourceKey();
        if (sourceKey == null || sourceKey.trim().isEmpty()) {
            sourceKey = task.getBusinessKey();
        }
        return Objects.toString(task.getFormingSourceScheduleDate(), "") + "|" + sourceKey + "|"
                + Objects.toString(task.getProcessCode(), "");
    }

    /**
     * 获取来源覆盖窗口的起始消费班，与两阶段来源窗口的原消费班口径一致。
     *
     * @param task 来源任务
     * @return 原成型逻辑班；缺失时依次回退来源排程班、生产班和零
     */
    private int resolveStockCoverageWindowStartShift(T task) {
        Integer shift = task.getFormingLogicalShiftOrder() == null
                ? task.getSourceShiftOrder() : task.getFormingLogicalShiftOrder();
        return shift == null ? (task.getShiftOrder() == null ? 0 : task.getShiftOrder()) : shift;
    }

    /**
     * 读取合并窗口累计需求；窗口模型缺少总量时按逐班明细重新累计。
     *
     * @param window 合并后的库存覆盖窗口
     * @return 配置窗口累计需求
     */
    private BigDecimal sumStockCoverageWindow(ScheduleStockCoverageWindowModel window) {
        if (window.getTotalDemandQty() != null) {
            return window.getTotalDemandQty();
        }
        BigDecimal totalDemandQty = window.getDemandQtyByOffset().values().stream().map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer extrapolatedShiftCount = window.getExtrapolatedShiftCount();
        if (extrapolatedShiftCount != null && extrapolatedShiftCount > 0) {
            totalDemandQty = totalDemandQty.add(this.nvl(window.getExtrapolatedDemandQtyPerShift())
                    .multiply(BigDecimalUtils.valueOf(extrapolatedShiftCount)));
        }
        return totalDemandQty;
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

    /**
     * 将超出最大班次数的逻辑班次映射回工厂标准物理班次（1~3 班循环）。
     *
     * @param logicalShiftOrder 逻辑班次序号
     * @param maxShiftOrder     最大排程班次数
     * @return 映射后的物理班次序号
     */
    public int mapGuardLogicalShiftOrder(int logicalShiftOrder, int maxShiftOrder) {
        return logicalShiftOrder <= maxShiftOrder ? logicalShiftOrder
                : ((logicalShiftOrder - maxShiftOrder - 1) % 3) + 1;
    }

    /**
     * 合并来源任务中去重后的非空白文本（逗号分隔）。
     *
     * @param sourceTaskList 来源任务列表
     * @param valueGetter    字段提取器
     * @return 合并后的逗号分隔文本，全为空时返回 null
     */
    public String mergeDistinctText(List<T> sourceTaskList, Function<T, String> valueGetter) {
        String merged = sourceTaskList.stream().map(valueGetter).filter(this::isNotBlank)
                .flatMap(value -> Arrays.stream(value.split("[,，]"))).map(String::trim)
                .filter(this::isNotBlank).distinct().collect(Collectors.joining(","));
        return this.isNotBlank(merged) ? merged : null;
    }

    /**
     * 比较两个数量是否数值相等（null 视为 0）。
     *
     * @param first  第一个数量
     * @param second 第二个数量
     * @return 数值相等返回 true
     */
    public boolean quantityEquals(BigDecimal first, BigDecimal second) {
        return this.nvl(first).compareTo(this.nvl(second)) == 0;
    }

    /**
     * 按小时偏移量计算目标时间。
     *
     * @param source        基准时间
     * @param hours         偏移小时数
     * @param millisPerHour 每小时毫秒数
     * @return 偏移后的新时间对象
     */
    public Date offsetHours(Date source, BigDecimal hours, long millisPerHour) {
        long offsetMillis = hours.multiply(BigDecimal.valueOf(millisPerHour))
                .setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        return new Date(source.getTime() + offsetMillis);
    }

    /**
     * 格式化展示数量值。
     *
     * @param quantity 数量值
     * @return 去除尾随零的字符串，为空时返回“未计算”
     */
    public String displayQuantity(BigDecimal quantity) {
        return quantity == null ? "未计算" : quantity.stripTrailingZeros().toPlainString();
    }

    /**
     * 获取任务未经前移或停产调整前的原始当班需求量。
     *
     * @param task 当前任务
     * @return 原始当班需求量
     */
    public BigDecimal resolveOriginalCurrentShiftDemandQty(T task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        return task.getOriginalCurrentShiftDemandQty() == null
                ? this.nvl(task.getCurrentShiftDemandQty()) : task.getOriginalCurrentShiftDemandQty();
    }

    /**
     * 将需求计算结果模型回填到任务对象。
     *
     * @param task   当前任务
     * @param result 需求计算结果
     */
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
        boolean newSpecBypass = task.getInputSnapshotKey() == null
                && task.getCommonNewSpecInfo() != null && task.getCommonNewSpecInfo().isNewSpecHit();
        boolean experimentBypass = task.getInputSnapshotKey() == null
                && task.getCommonExperimentSpecInfo() != null
                && task.getCommonExperimentSpecInfo().isExperimentSpecHit();
        boolean formingShutdownCloseOut = Boolean.TRUE.equals(task.getFormingShutdownCloseOutFlag());
        evidence.put("applicable", !newSpecBypass && !experimentBypass && !formingShutdownCloseOut
                && task.getPlanQty() != null);
        evidence.put("bypassReason", newSpecBypass ? "NEW_SPEC_ADVANCE"
                : experimentBypass ? "EXPERIMENT_SPEC_ADVANCE_WINDOW"
                : formingShutdownCloseOut ? "FORMING_SHUTDOWN_CLOSE_OUT" : null);
        evidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
        evidence.put("nextShiftDemandQty", task.getNextShiftDemandQty());
        evidence.put("rollingStockQty", task.getRollingStockQty());
        evidence.put("twoShiftDemandQty", task.getTwoShiftDemandQty());
        evidence.put("twoShiftStockGapQty", task.getTwoShiftStockGapQty());
        evidence.put("stockCovered", task.getTwoShiftStockCovered());
        evidence.put("stockCoverageWindow", task.getStockCoverageWindow());
        evidence.put("stockCoverageDemandQty", task.getStockCoverageDemandQty());
        evidence.put("stockCoverageStockGapQty", task.getStockCoverageStockGapQty());
        evidence.put("stockCoverageCovered", task.getStockCoverageCovered());
        ScheduleStockCoverageWindowModel coverageWindow = task.getStockCoverageWindow();
        evidence.put("stockCoverageParamCode", coverageWindow == null ? null : coverageWindow.getParamCode());
        evidence.put("stockCoverageShiftCount", coverageWindow == null ? null : coverageWindow.getShiftCount());
        evidence.put("stockCoverageParamSource", coverageWindow == null ? null : coverageWindow.getParamSource());
        evidence.put("stockCoverageFallbackReason", coverageWindow == null ? null : coverageWindow.getFallbackReason());
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

    /** 判断字符串是否非空白。 */
    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
