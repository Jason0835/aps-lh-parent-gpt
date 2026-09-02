package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自动排程机台分配公共主流程引擎。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 */
public final class MachineAssignmentEngine<C extends MachineAssignmentContext<T>,
        T extends ScheduleTaskDraftModel, M extends ScheduleMachineCandidateModel, P> {

    /**
     * 执行逐班机台分配主流程。
     *
     * @param context 排程上下文
     * @param policy 领域策略
     */
    public void assign(C context, MachineAssignmentPolicy<C, T, P> policy) {
        policy.validateContext(context);
        List<T> taskDraftList = context.getTaskDraftList() == null
                ? Collections.emptyList() : new ArrayList<>(context.getTaskDraftList());
        Map<Integer, List<ScheduleShiftToolDemand>> shiftToolDemandMap = policy.snapshotShiftToolDemand(context);
        if (taskDraftList.isEmpty()
                && shiftToolDemandMap.isEmpty()) {
            return;
        }
        if (context.getCurrentAvailableToolQty() == null) {
            context.setCurrentAvailableToolQty(context.getInitialAvailableToolQty());
        }
        Map<String, BigDecimal> runtimeStockMap = new java.util.HashMap<>(context.getInitialStockMap());
        boolean inventoryClosedLoopEnabled = policy.isInventoryClosedLoopEnabled(context);
        Map<Integer, List<T>> shiftTaskMap = taskDraftList.stream()
                .filter(Objects::nonNull)
                .filter(task -> !Boolean.TRUE.equals(task.getSourceExplainTask()))
                .collect(Collectors.groupingBy(task -> this.normalizeShiftOrder(task.getShiftOrder(),
                        context.getMaxShiftOrder()),
                        TreeMap::new, Collectors.toList()));
        int maxShiftOrder = this.resolveMaxShiftOrder(context, shiftTaskMap, shiftToolDemandMap);
        for (int shiftOrder = 1; shiftOrder <= maxShiftOrder; shiftOrder++) {
            BigDecimal availableToolQtyBeforeShift = context.getCurrentAvailableToolQty();
            List<ScheduleShiftToolDemand> currentShiftDemandList = shiftToolDemandMap.getOrDefault(
                    shiftOrder, Collections.emptyList());
            BigDecimal availableToolQtyBeforePlan = policy.prepareShiftToolLedger(context, shiftOrder,
                    availableToolQtyBeforeShift, currentShiftDemandList);
            context.setCurrentAvailableToolQty(availableToolQtyBeforePlan);
            List<T> shiftTaskList = shiftTaskMap.getOrDefault(shiftOrder, Collections.emptyList());
            if (!shiftTaskList.isEmpty()) {
                if (inventoryClosedLoopEnabled) {
                    policy.recalculateShiftPlans(context, shiftTaskList, runtimeStockMap);
                }
                List<T> ledgerOrderedTaskList = new ArrayList<>(shiftTaskList);
                policy.prepareTaskOrder(ledgerOrderedTaskList);
                List<T> remainingTaskList = ledgerOrderedTaskList.stream()
                        .filter(policy::isMachineAssignmentRequired)
                        .collect(Collectors.toCollection(LinkedList::new));
                Map<T, Integer> ledgerOrderMap = new IdentityHashMap<>();
                for (int index = 0; index < ledgerOrderedTaskList.size(); index++) {
                    T task = ledgerOrderedTaskList.get(index);
                    if (task != null) {
                        ledgerOrderMap.put(task, index);
                    }
                }
                List<T> nonAssignedTaskList = ledgerOrderedTaskList.stream()
                        .filter(Objects::nonNull)
                        .filter(task -> !policy.isMachineAssignmentRequired(task))
                        .collect(Collectors.toCollection(LinkedList::new));
                int assignmentSequence = 0;
                while (!remainingTaskList.isEmpty()) {
                    T task = policy.selectNextTask(remainingTaskList, context, ++assignmentSequence);
                    while (!nonAssignedTaskList.isEmpty()
                            && this.resolveLedgerOrder(ledgerOrderMap, nonAssignedTaskList.get(0))
                            < this.resolveLedgerOrder(ledgerOrderMap, task)) {
                        policy.settleNonAssignedTaskToolLedger(context, nonAssignedTaskList.remove(0));
                    }
                    remainingTaskList.remove(task);
                    policy.assignSingleTask(task, context);
                    if (!policy.isMachineAssignmentRequired(task)) {
                        policy.settleNonAssignedTaskToolLedger(context, task);
                    }
                }
                while (!nonAssignedTaskList.isEmpty()) {
                    policy.settleNonAssignedTaskToolLedger(context, nonAssignedTaskList.remove(0));
                }
                policy.fillCurrentShiftIdleCapacity(shiftOrder, shiftTaskMap, context);
                if (inventoryClosedLoopEnabled) {
                    this.closeShiftInventory(context, shiftOrder, shiftTaskList, runtimeStockMap, policy);
                }
            }
            policy.settleShiftToolLedger(context, shiftOrder, availableToolQtyBeforeShift,
                    shiftToolDemandMap.getOrDefault(shiftOrder + 1, Collections.emptyList()));
        }
        policy.finishAssignment(context);
    }

    /**
     * 获取任务在本班账本事件中的稳定顺序。
     *
     * @param ledgerOrderMap 任务顺序映射
     * @param task 当前任务
     * @return 任务顺序，无法解析时返回最大值
     */
    private int resolveLedgerOrder(Map<T, Integer> ledgerOrderMap, T task) {
        return ledgerOrderMap.getOrDefault(task, Integer.MAX_VALUE);
    }

    /**
     * 解析本次排程需要处理的最后班次，保证停产或空班也能执行班次结算。
     *
     * @param context 排程上下文
     * @param shiftTaskMap 班次任务集合
     * @param shiftToolDemandMap 班次成型需求快照
     * @return 最后班次序号
     */
    private int resolveMaxShiftOrder(C context, Map<Integer, List<T>> shiftTaskMap,
                                     Map<Integer, List<ScheduleShiftToolDemand>> shiftToolDemandMap) {
        int configuredMaxShiftOrder = context.getMaxShiftOrder();
        if (configuredMaxShiftOrder > 0 && configuredMaxShiftOrder < Integer.MAX_VALUE) {
            return configuredMaxShiftOrder;
        }
        return Stream.concat(shiftTaskMap.keySet().stream(), shiftToolDemandMap.keySet().stream())
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * 按产品稳定顺序重算当前班次计划量，并维护重算期间的产品库存游标。
     *
     * @param context 排程上下文
     * @param shiftTaskList 当前班次任务
     * @param runtimeStockMap 当前运行库存
     * @param policy 领域机台分配策略
     */
    public void recalculateShiftPlans(C context, List<T> shiftTaskList,
                                       Map<String, BigDecimal> runtimeStockMap,
                                       MachineAssignmentPolicy<C, T, P> policy) {
        if (shiftTaskList == null || shiftTaskList.isEmpty()) {
            return;
        }
        P planQtyStrategy = policy.resolvePlanQtyStrategy(context);
        Map<String, BigDecimal> planningStockMap = new java.util.HashMap<>(runtimeStockMap);
        Set<String> recalculatedProductCodeSet = new java.util.HashSet<>();
        // 原实现只对流排序，不改变班次任务原列表；保留原列表顺序，避免影响后续补量和关账遍历。
        List<T> sortedTaskList = new ArrayList<>(shiftTaskList);
        policy.sortRecalculationTasks(sortedTaskList);
        sortedTaskList.stream()
                .filter(Objects::nonNull)
                .forEach(task -> {
                    String productCode = policy.getProductCode(task);
                    if (this.isBlank(productCode) || this.isNotBlank(task.getUnplannedReasonCode())) {
                        return;
                    }
                    BigDecimal openingStock = planningStockMap.getOrDefault(productCode,
                            this.nvl(task.getRollingStockQty()));
                    runtimeStockMap.putIfAbsent(productCode, openingStock);
                    task.setRollingStockQty(openingStock);
                    if (policy.isPresetPlanTask(task)) {
                        planningStockMap.put(productCode, openingStock.add(this.nvl(task.getPlanQty()))
                                .subtract(this.nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
                        return;
                    }
                    if (!recalculatedProductCodeSet.add(productCode)) {
                        this.clearDuplicateProductShiftPlan(task, openingStock);
                        return;
                    }
                    task.setPlanQty(null);
                    policy.recalculateTaskPlanQty(context, task, planQtyStrategy);
                    planningStockMap.put(productCode, this.nvl(task.getPlanStockQty()));
                });
    }

    /**
     * 使用本班实际承接量关账，生成产品库存和班次短缺台账。
     *
     * @param context 排程上下文
     * @param shiftOrder 当前班次
     * @param shiftTaskList 当前班次任务
     * @param runtimeStockMap 当前运行库存
     * @param policy 领域机台分配策略
     */
    public void closeShiftInventory(C context, Integer shiftOrder, List<T> shiftTaskList,
                                     Map<String, BigDecimal> runtimeStockMap,
                                     MachineAssignmentPolicy<C, T, P> policy) {
        Map<String, BigDecimal> demandQtyMap = shiftTaskList.stream()
                .filter(Objects::nonNull)
                .filter(task -> this.isNotBlank(policy.getProductCode(task)))
                .collect(Collectors.groupingBy(policy::getProductCode, java.util.LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getCurrentShiftDemandQty()), BigDecimal::max)));
        Map<String, BigDecimal> assignedQtyMap = context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> Objects.equals(this.normalizeShiftOrder(task.getShiftOrder(),
                                context.getMaxShiftOrder()), shiftOrder))
                .filter(task -> this.isNotBlank(policy.getProductCode(task)))
                .collect(Collectors.groupingBy(policy::getProductCode, java.util.LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPlanQty()), BigDecimal::add)));
        Set<String> productCodeSet = new java.util.LinkedHashSet<>(demandQtyMap.keySet());
        productCodeSet.addAll(assignedQtyMap.keySet());
        for (String productCode : productCodeSet) {
            BigDecimal rawBalance = runtimeStockMap.getOrDefault(productCode, BigDecimal.ZERO)
                    .add(assignedQtyMap.getOrDefault(productCode, BigDecimal.ZERO))
                    .subtract(demandQtyMap.getOrDefault(productCode, BigDecimal.ZERO));
            BigDecimal shortageQty = rawBalance.min(BigDecimal.ZERO).abs();
            BigDecimal availableStockQty = rawBalance.max(BigDecimal.ZERO);
            runtimeStockMap.put(productCode, availableStockQty);
            context.getProductShiftShortageMap().put(productCode + "|" + shiftOrder, shortageQty);
            shiftTaskList.stream()
                    .filter(task -> Objects.equals(productCode, policy.getProductCode(task)))
                    .forEach(task -> task.setPlanStockQty(availableStockQty));
        }
        context.setRemainingStockMap(new java.util.HashMap<>(runtimeStockMap));
    }

    private Integer normalizeShiftOrder(Integer shiftOrder, int maxShiftOrder) {
        if (shiftOrder == null || shiftOrder < 1) {
            return 1;
        }
        int normalizedMaxShiftOrder = maxShiftOrder < 1 ? Integer.MAX_VALUE : maxShiftOrder;
        return Math.min(shiftOrder, normalizedMaxShiftOrder);
    }

    private boolean isNotBlank(String value) {
        return !this.isBlank(value);
    }

    public boolean isMachineAssignmentRequired(T task) {
        return task != null && (this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0
                || this.nvl(task.getToolOverflowQty()).compareTo(BigDecimal.ZERO) > 0)
                && this.isBlank(task.getUnplannedReasonCode());
    }

    public void clearDuplicateProductShiftPlan(T task, BigDecimal openingStock) {
        task.setPreLossPlanQty(BigDecimal.ZERO);
        task.setLossAddQty(BigDecimal.ZERO);
        task.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
        task.setPlanQty(BigDecimal.ZERO);
        task.setPlanStockQty(openingStock);
    }

    public BigDecimal deductQty(BigDecimal value, BigDecimal deductedQty) {
        return value == null ? null : value.subtract(this.nvl(deductedQty)).max(BigDecimal.ZERO);
    }

    public String resolveCarryoverSourceType(BigDecimal capacityOverflowQty, BigDecimal toolOverflowQty,
                                              String capacitySource, String toolSource) {
        boolean capacityOverflow = this.nvl(capacityOverflowQty).compareTo(BigDecimal.ZERO) > 0;
        boolean toolOverflow = this.nvl(toolOverflowQty).compareTo(BigDecimal.ZERO) > 0;
        if (capacityOverflow && toolOverflow) {
            return capacitySource + "," + toolSource;
        }
        return toolOverflow ? toolSource : capacitySource;
    }

    public BigDecimal getCandidateEvidenceDecimal(Map<String, Object> evidence, String key) {
        Object value = evidence.get(key);
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }

    public String normalizeBusinessKeyToken(String value) {
        if (this.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9]", "_");
        String prefix = normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
        return prefix + "_" + Integer.toHexString(value.hashCode());
    }

    public void applyCapacitySplitResult(T task, BigDecimal beforeAssignQty, BigDecimal assignedQty,
                                         BigDecimal remainCapacity, BigDecimal machineSpeed, String splitDesc) {
        task.setPlanQty(assignedQty);
        task.setMachineRemainCapacity(remainCapacity);
        task.setMachineSpeed(machineSpeed);
        task.setCapacityAdjustQty(this.nvl(task.getCapacityAdjustQty())
                .add(assignedQty.subtract(this.nvl(beforeAssignQty))));
        task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), splitDesc));
    }

    public String appendFormulaDesc(String current, String addition) {
        if (this.isBlank(current)) {
            return addition;
        }
        return current.contains(addition) ? current : current + "->" + addition;
    }

    public M findCandidateByMachineCode(List<M> candidates, String machineCode) {
        if (candidates == null || candidates.isEmpty() || this.isBlank(machineCode)) {
            return null;
        }
        return candidates.stream().filter(candidate -> machineCode.equals(candidate.getMachineCode()))
                .findFirst().orElse(null);
    }

    public boolean contains(Set<String> values, String value) {
        return values != null && !this.isBlank(value) && values.contains(value);
    }

    public boolean isGlueSwitch(String previousGlueCode, String currentGlueCode) {
        return !this.isBlank(previousGlueCode) && !this.isBlank(currentGlueCode)
                && !Objects.equals(previousGlueCode.trim(), currentGlueCode.trim());
    }

    public BigDecimal resolveMaintenanceHours(T task, M candidate, int maxShiftOrder) {
        Map<Integer, BigDecimal> hoursByShift = candidate.getMaintenanceHoursByShift();
        if (hoursByShift != null && !hoursByShift.isEmpty()) {
            int shiftOrder = task.getShiftOrder() == null || task.getShiftOrder() < 1
                    ? 1 : Math.min(task.getShiftOrder(), maxShiftOrder);
            return this.nvl(hoursByShift.get(shiftOrder));
        }
        return this.nvl(candidate.getMaintenanceHours());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
