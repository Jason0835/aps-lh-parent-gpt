package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintCalculator;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleTaskConstraint;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.engine.domain.manual.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧人工滚动纯计算引擎。
 *
 * <p>引擎仅操作独立任务片段，在一个上下文内连续应用全部命令，再从每台机台最早受影响位置
 * 统一重装箱。数据库实体、事务、锁和审计均由应用层负责。</p>
 */
@Service
public class TcManualRollingEngineService {

    /** 新人工结果分组前缀。 */
    private static final String MANUAL_GROUP_PREFIX = "MANUAL:";

    /** 转机台新结果分组前缀。 */
    private static final String MOVE_GROUP_PREFIX = "MOVE:";

    /** 胎面、胎侧共用排程约束纯计算器。 */
    private final ScheduleConstraintCalculator constraintCalculator = new ScheduleConstraintCalculator();

    /**
     * 批量执行人工滚动命令。
     *
     * @param commandBatch 命令批次
     * @param context 运行态上下文
     * @return 最终任务链结果
     * @throws IllegalArgumentException 命令目标非法时抛出
     * @throws IllegalStateException 数量、产能或链表校验失败时抛出
     */
    public TcManualRollingResult execute(TcManualRollingCommandBatch commandBatch,
                                         TcManualRollingContext context) {
        this.validateInput(commandBatch, context);
        List<TcManualTaskDraft> taskList = context.getTaskList().stream()
                .filter(Objects::nonNull).map(TcManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> initialGroupSet = taskList.stream().map(TcManualTaskDraft::getResultGroupKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        TcManualRollingResult result = new TcManualRollingResult();
        result.setBeforeTotalQty(this.sumQty(taskList));
        Map<String, Integer[]> scopeMap = new LinkedHashMap<>();
        BigDecimal commandDeltaQty = BigDecimal.ZERO;
        for (int commandIndex = 0; commandIndex < commandBatch.getCommandList().size(); commandIndex++) {
            TcManualRollingCommand command = commandBatch.getCommandList().get(commandIndex);
            if (command == null || command.getOperationType() == null) {
                throw new IllegalArgumentException("胎侧人工滚动命令不能为空");
            }
            command.setCommandOrder(command.getCommandOrder() == null ? commandIndex : command.getCommandOrder());
            BigDecimal beforeQty = this.sumQty(taskList);
            BigDecimal declaredDelta = this.applyCommand(taskList, command, scopeMap,
                    result.getAffectedResultGroupKeySet());
            if (this.sumQty(taskList).subtract(beforeQty).compareTo(declaredDelta) != 0) {
                throw new IllegalStateException("胎侧人工滚动单命令数量不守恒");
            }
            commandDeltaQty = commandDeltaQty.add(declaredDelta);
        }
        List<TcManualTaskDraft> unplannedList = new ArrayList<>();
        for (Map.Entry<String, Integer[]> scopeEntry : scopeMap.entrySet()) {
            Integer[] scope = scopeEntry.getValue();
            taskList = this.repackMachine(taskList, scopeEntry.getKey(), scope[0], scope[1], context,
                    unplannedList);
            result.getChainChangeSummaryList().add(scopeEntry.getKey() + ":CLASS" + scope[0] + ":SEQ" + scope[1]);
        }
        taskList = this.applyToolLimit(taskList, context, unplannedList);
        this.normalizeSequences(taskList);
        MachineShiftTaskChain<TcManualTaskDraft> taskChainGroup = this.buildTaskChains(taskList, context);
        this.validateResult(taskList, unplannedList, result.getBeforeTotalQty(), commandDeltaQty,
                initialGroupSet, result.getAffectedResultGroupKeySet());
        result.setScheduledTaskList(taskList);
        result.setUnplannedTaskList(unplannedList);
        result.setTaskChainGroup(taskChainGroup);
        result.setCommandDeltaQty(commandDeltaQty);
        result.setScheduledTotalQty(this.sumQty(taskList));
        result.setUnplannedTotalQty(this.sumQty(unplannedList));
        result.setAffectedResultIdSet(result.getAffectedResultGroupKeySet().stream()
                .map(this::parseResultId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        context.setTaskList(taskList);
        context.setTaskChainGroup(taskChainGroup);
        return result;
    }

    /**
     * 将最终任务按机台和班次装入通用双向任务链。
     *
     * @param taskList 最终任务
     * @param context 运行态上下文
     * @return 机台班次任务链集合
     */
    private MachineShiftTaskChain<TcManualTaskDraft> buildTaskChains(List<TcManualTaskDraft> taskList,
                                                                      TcManualRollingContext context) {
        MachineShiftTaskChain<TcManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
        LocalDate scheduleDate = context.getScheduleDate() == null ? LocalDate.of(1970, 1, 1)
                : Instant.ofEpochMilli(context.getScheduleDate().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
        List<TcManualTaskDraft> sortedList = taskList.stream().sorted(
                Comparator.comparing(TcManualTaskDraft::getMachineCode)
                        .thenComparing(TcManualTaskDraft::getShiftOrder)
                        .thenComparing(TcManualTaskDraft::getSequence)).collect(Collectors.toList());
        for (TcManualTaskDraft task : sortedList) {
            ScheduleTaskLinkedList<TcManualTaskDraft> chain = taskChainGroup.getOrCreate(
                    task.getMachineCode(), scheduleDate, task.getShiftOrder());
            ScheduleTaskNode<TcManualTaskDraft> node = new ScheduleTaskNode<>(task.getTaskId(), task,
                    task.getMachineCode(), scheduleDate, "CLASS" + task.getShiftOrder(),
                    task.getShiftOrder(), task.getPlanQty());
            chain.append(node, new ScheduleOperationContext("TC_MANUAL", "TC_MANUAL_ROLLING", null));
            task.setSequence(node.getSequence());
        }
        return taskChainGroup;
    }

    /**
     * 应用单条命令。
     *
     * @param taskList 当前任务
     * @param command 命令
     * @param scopeMap 受影响范围
     * @param affectedGroupSet 受影响分组
     * @return 命令净数量变化
     */
    private BigDecimal applyCommand(List<TcManualTaskDraft> taskList, TcManualRollingCommand command,
                                    Map<String, Integer[]> scopeMap, Set<String> affectedGroupSet) {
        if (TcManualRollingOperationEnum.INSERT == command.getOperationType()) {
            return this.applyInsert(taskList, command, scopeMap, affectedGroupSet);
        }
        if (TcManualRollingOperationEnum.DELETE == command.getOperationType()) {
            return this.applyDelete(taskList, command, scopeMap, affectedGroupSet);
        }
        if (TcManualRollingOperationEnum.CHANGE_MACHINE == command.getOperationType()) {
            return this.applyTransfer(taskList, command, scopeMap, affectedGroupSet);
        }
        return this.applyChangeQty(taskList, command, scopeMap, affectedGroupSet);
    }

    /** 执行插单。 */
    private BigDecimal applyInsert(List<TcManualTaskDraft> taskList, TcManualRollingCommand command,
                                   Map<String, Integer[]> scopeMap, Set<String> affectedGroupSet) {
        TcManualTaskDraft task = command.getInsertTask() == null ? null : command.getInsertTask().copy();
        if (task == null) {
            throw new IllegalArgumentException("胎侧插单任务不能为空");
        }
        if (StrUtil.isBlank(task.getResultGroupKey())) {
            task.setResultGroupKey(MANUAL_GROUP_PREFIX + command.getCommandOrder() + ":" + task.getTaskId());
        }
        task.setMachineCode(StrUtil.blankToDefault(command.getTargetMachineCode(), task.getMachineCode()));
        task.setShiftOrder(command.getTargetShiftOrder() == null ? task.getShiftOrder() : command.getTargetShiftOrder());
        task.setSequence(command.getTargetSequence() == null ? task.getSequence() : command.getTargetSequence());
        task.setMinimumShiftOrder(task.getShiftOrder());
        task.setInsertTask(true);
        task.setOperationOrder(command.getCommandOrder());
        this.validateLocation(task);
        taskList.add(task);
        this.registerScope(scopeMap, task.getMachineCode(), task.getShiftOrder(), task.getSequence());
        affectedGroupSet.add(task.getResultGroupKey());
        return this.qty(task.getPlanQty());
    }

    /** 执行整行删除。 */
    private BigDecimal applyDelete(List<TcManualTaskDraft> taskList, TcManualRollingCommand command,
                                   Map<String, Integer[]> scopeMap, Set<String> affectedGroupSet) {
        List<TcManualTaskDraft> deletedList = taskList.stream()
                .filter(task -> Objects.equals(command.getResultGroupKey(), task.getResultGroupKey()))
                .collect(Collectors.toList());
        if (deletedList.isEmpty()) {
            throw new IllegalArgumentException("胎侧待删除任务不存在");
        }
        if (deletedList.stream().anyMatch(task -> this.qty(task.getFinishQty()).signum() > 0)) {
            throw new IllegalStateException("存在完成量的胎侧任务不允许删除");
        }
        deletedList.forEach(task -> this.registerScope(scopeMap, task.getMachineCode(),
                task.getShiftOrder(), task.getSequence()));
        BigDecimal deletedQty = this.sumQty(deletedList);
        taskList.removeAll(deletedList);
        affectedGroupSet.add(command.getResultGroupKey());
        return deletedQty.negate();
    }

    /** 执行调量或自动滚动调量。 */
    private BigDecimal applyChangeQty(List<TcManualTaskDraft> taskList, TcManualRollingCommand command,
                                      Map<String, Integer[]> scopeMap, Set<String> affectedGroupSet) {
        TcManualTaskDraft task = this.findTask(taskList, command);
        BigDecimal newPlanQty = this.qty(command.getPlanQty());
        if (newPlanQty.compareTo(this.qty(task.getFinishQty())) < 0) {
            throw new IllegalStateException("胎侧计划量不能小于完成量");
        }
        BigDecimal deltaQty = newPlanQty.subtract(this.qty(task.getPlanQty()));
        task.setPlanQty(newPlanQty);
        task.setAnalysis(command.getAnalysis());
        task.setOperationOrder(command.getCommandOrder());
        this.registerScope(scopeMap, task.getMachineCode(), task.getShiftOrder(), task.getSequence());
        affectedGroupSet.add(task.getResultGroupKey());
        return deltaQty;
    }

    /** 执行转机台，完成量固定留在源班次。 */
    private BigDecimal applyTransfer(List<TcManualTaskDraft> taskList, TcManualRollingCommand command,
                                     Map<String, Integer[]> scopeMap, Set<String> affectedGroupSet) {
        TcManualTaskDraft source = this.findTask(taskList, command);
        BigDecimal finishQty = this.qty(source.getFinishQty());
        BigDecimal moveQty = this.qty(source.getPlanQty()).subtract(finishQty);
        if (moveQty.signum() <= 0) {
            throw new IllegalStateException("胎侧任务没有可转移的未完成量");
        }
        this.registerScope(scopeMap, source.getMachineCode(), source.getShiftOrder(), source.getSequence());
        TcManualTaskDraft moved = source.copy();
        moved.setTaskId(source.getTaskId() + ":MOVE:" + command.getCommandOrder());
        moved.setResultGroupKey(MOVE_GROUP_PREFIX + source.getResultGroupKey() + ":" + command.getCommandOrder());
        moved.setSourceResultId(null);
        moved.setFinishQty(BigDecimal.ZERO);
        moved.setPlanQty(moveQty);
        moved.setMachineCode(command.getTargetMachineCode());
        moved.setShiftOrder(command.getTargetShiftOrder());
        moved.setSequence(command.getTargetSequence() == null ? 1 : command.getTargetSequence());
        moved.setMinimumShiftOrder(moved.getShiftOrder());
        moved.setOperationOrder(command.getCommandOrder());
        this.validateLocation(moved);
        if (finishQty.signum() > 0) {
            source.setPlanQty(finishQty);
        } else {
            taskList.remove(source);
        }
        TcManualTaskDraft compatible = taskList.stream()
                .filter(task -> Objects.equals(moved.getMachineCode(), task.getMachineCode()))
                .filter(task -> Objects.equals(moved.getMergeGrainKey(), task.getMergeGrainKey()))
                .sorted(this.taskComparator()).findFirst().orElse(null);
        if (compatible != null) {
            moved.setResultGroupKey(compatible.getResultGroupKey());
            moved.setSourceResultId(compatible.getSourceResultId());
        }
        TcManualTaskDraft mergeTarget = taskList.stream()
                .filter(task -> Objects.equals(moved.getMachineCode(), task.getMachineCode()))
                .filter(task -> Objects.equals(moved.getShiftOrder(), task.getShiftOrder()))
                .filter(task -> Objects.equals(moved.getResultGroupKey(), task.getResultGroupKey()))
                .findFirst().orElse(null);
        if (mergeTarget == null) {
            taskList.add(moved);
        } else {
            mergeTarget.setPlanQty(this.qty(mergeTarget.getPlanQty()).add(moveQty));
        }
        affectedGroupSet.add(source.getResultGroupKey());
        affectedGroupSet.add(moved.getResultGroupKey());
        this.registerScope(scopeMap, moved.getMachineCode(), moved.getShiftOrder(), moved.getSequence());
        return BigDecimal.ZERO;
    }

    /**
     * 对单台机台受影响窗口重新装箱。
     */
    private List<TcManualTaskDraft> repackMachine(List<TcManualTaskDraft> allTaskList, String machineCode,
                                                   int startShiftOrder, int startSequence,
                                                   TcManualRollingContext context,
                                                   List<TcManualTaskDraft> unplannedList) {
        List<TcManualTaskDraft> retainedList = allTaskList.stream()
                .filter(task -> !Objects.equals(machineCode, task.getMachineCode())
                        || task.getShiftOrder() < startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder && this.sequence(task) < startSequence))
                .map(TcManualTaskDraft::copy).collect(Collectors.toCollection(ArrayList::new));
        List<TcManualTaskDraft> windowList = allTaskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder && this.sequence(task) >= startSequence))
                .sorted(this.taskComparator()).map(TcManualTaskDraft::copy).collect(Collectors.toList());
        List<TcManualTaskDraft> queue = new ArrayList<>();
        for (TcManualTaskDraft task : windowList) {
            BigDecimal finishQty = this.qty(task.getFinishQty());
            if (finishQty.signum() > 0) {
                TcManualTaskDraft locked = task.copy();
                locked.setPlanQty(finishQty);
                retainedList.add(locked);
            }
            BigDecimal remainingQty = this.qty(task.getPlanQty()).subtract(finishQty);
            if (remainingQty.signum() > 0) {
                TcManualTaskDraft rolling = task.copy();
                rolling.setPlanQty(remainingQty);
                rolling.setFinishQty(BigDecimal.ZERO);
                queue.add(rolling);
            }
        }
        queue.sort(this.taskComparator());
        int fragmentIndex = 1;
        for (int shiftOrder = startShiftOrder; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            if (queue.isEmpty()) {
                break;
            }
            BigDecimal capacity = context.getShiftCapacityMap().get(this.capacityKey(machineCode, shiftOrder));
            if (capacity == null || capacity.signum() <= 0) {
                throw new IllegalStateException("胎侧机台班次有效产能未维护:" + machineCode + ":" + shiftOrder);
            }
            final int currentShiftOrder = shiftOrder;
            List<TcManualTaskDraft> currentShiftTaskList = retainedList.stream()
                    .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                    .filter(task -> Objects.equals(currentShiftOrder, task.getShiftOrder()))
                    .sorted(this.taskComparator()).collect(Collectors.toList());
            BigDecimal usedQty = currentShiftTaskList.stream().map(TcManualTaskDraft::getPlanQty)
                    .map(this::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
            TcManualTaskDraft predecessorTask = this.findPredecessorTask(
                    retainedList, context, machineCode, currentShiftOrder);
            BigDecimal existingSwitchCapacityDeduct = this.calculateSwitchCapacityDeduct(
                    currentShiftTaskList, predecessorTask, context);
            BigDecimal remainCapacity = this.constraintCalculator.calculateRemainCapacity(
                    capacity, usedQty, existingSwitchCapacityDeduct);
            int nextSequence = (int) retainedList.stream()
                    .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                    .filter(task -> Objects.equals(currentShiftOrder, task.getShiftOrder())).count() + 1;
            while (!queue.isEmpty() && remainCapacity.signum() > 0) {
                TcManualTaskDraft task = queue.get(0);
                if (task.getMinimumShiftOrder() != null && shiftOrder < task.getMinimumShiftOrder()) {
                    break;
                }
                task.setMachineSpeed(context.getMachineSpecSpeedMap().getOrDefault(
                        machineCode + "|" + task.getSidewallCode(), task.getMachineSpeed()));
                TcManualTaskDraft mergeTarget = retainedList.stream()
                        .filter(item -> Objects.equals(machineCode, item.getMachineCode()))
                        .filter(item -> Objects.equals(currentShiftOrder, item.getShiftOrder()))
                    .filter(item -> Objects.equals(task.getResultGroupKey(), item.getResultGroupKey()))
                    .findFirst().orElse(null);
                TcManualTaskDraft previousTask = this.findLastTask(currentShiftTaskList);
                if (previousTask == null) {
                    previousTask = predecessorTask;
                }
                BigDecimal currentSwitchCapacityDeduct = mergeTarget == null
                        ? this.calculateTransitionCapacityDeduct(
                        previousTask, task, context)
                        : BigDecimal.ZERO;
                BigDecimal availablePlanCapacity = remainCapacity.subtract(currentSwitchCapacityDeduct)
                        .max(BigDecimal.ZERO);
                if (availablePlanCapacity.signum() <= 0) {
                    break;
                }
                BigDecimal assignedQty = this.qty(task.getPlanQty()).min(availablePlanCapacity);
                if (mergeTarget == null) {
                    TcManualTaskDraft assigned = task.copy();
                    assigned.setShiftOrder(shiftOrder);
                    assigned.setSequence(nextSequence++);
                    assigned.setPlanQty(assignedQty);
                    assigned.setFinishQty(BigDecimal.ZERO);
                    retainedList.add(assigned);
                    currentShiftTaskList.add(assigned);
                } else {
                    mergeTarget.setPlanQty(this.qty(mergeTarget.getPlanQty()).add(assignedQty));
                }
                remainCapacity = remainCapacity.subtract(currentSwitchCapacityDeduct)
                        .subtract(assignedQty).max(BigDecimal.ZERO);
                BigDecimal overflowQty = this.qty(task.getPlanQty()).subtract(assignedQty);
                queue.remove(0);
                if (overflowQty.signum() > 0) {
                    TcManualTaskDraft carry = task.copy();
                    carry.setTaskId(task.getTaskId() + ":CARRY:" + fragmentIndex++);
                    carry.setPlanQty(overflowQty);
                    carry.setCarryoverTask(true);
                    queue.add(0, carry);
                    break;
                }
            }
        }
        unplannedList.addAll(queue.stream().filter(task -> this.qty(task.getPlanQty()).signum() > 0)
                .map(TcManualTaskDraft::copy).collect(Collectors.toList()));
        return retainedList;
    }

    /**
     * 计算胎侧班次完整任务链切换产能扣减。
     *
     * @param taskList 班次有序任务
     * @param predecessorTask 班次开始前的前置任务
     * @param context 人工滚动上下文
     * @return 切换产能扣减合计
     */
    private BigDecimal calculateSwitchCapacityDeduct(List<TcManualTaskDraft> taskList,
                                                      TcManualTaskDraft predecessorTask,
                                                      TcManualRollingContext context) {
        BigDecimal totalCapacityDeduct = BigDecimal.ZERO;
        TcManualTaskDraft previousTask = predecessorTask;
        for (TcManualTaskDraft currentTask : taskList) {
            totalCapacityDeduct = totalCapacityDeduct.add(
                    this.calculateTransitionCapacityDeduct(previousTask, currentTask, context));
            previousTask = currentTask;
        }
        return totalCapacityDeduct;
    }

    /**
     * 查找指定班次开始前的同机台链尾任务。
     *
     * @param taskList 当前重装箱任务
     * @param context 人工滚动上下文
     * @param machineCode 机台编码
     * @param shiftOrder 班次顺序
     * @return 前一班链尾；一班没有当日前置时返回前日链尾
     */
    private TcManualTaskDraft findPredecessorTask(List<TcManualTaskDraft> taskList,
                                                  TcManualRollingContext context,
                                                  String machineCode,
                                                  Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> task.getShiftOrder() != null && shiftOrder != null
                        && task.getShiftOrder() < shiftOrder)
                .max(Comparator.comparing(TcManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence())))
                .orElse(context.getPredecessorTaskMap().get(machineCode));
    }

    /**
     * 计算两个相邻胎侧任务的切换产能。
     *
     * @param previousTask 前置任务
     * @param currentTask 当前任务
     * @param context 人工滚动上下文
     * @return 相邻任务切换产能扣减
     */
    private BigDecimal calculateTransitionCapacityDeduct(TcManualTaskDraft previousTask,
                                                         TcManualTaskDraft currentTask,
                                                         TcManualRollingContext context) {
        return this.constraintCalculator.calculateTransition(this.toConstraintTask(previousTask),
                this.toConstraintTask(currentTask), context.getConstraintConfig()).getTotalCapacityDeduct();
    }

    /**
     * 将胎侧人工任务映射为共用约束快照。
     *
     * @param task 胎侧人工任务
     * @return 共用约束快照；任务为空时返回空
     */
    private ScheduleTaskConstraint toConstraintTask(TcManualTaskDraft task) {
        if (task == null) {
            return null;
        }
        ScheduleTaskConstraint constraintTask = new ScheduleTaskConstraint();
        constraintTask.setSpecCode(task.getSidewallCode());
        constraintTask.setGlueCode(task.getGlueCode());
        constraintTask.setMachineSpeed(task.getMachineSpeed());
        return constraintTask;
    }

    /**
     * 获取胎侧班次任务链尾任务。
     *
     * @param taskList 班次有序任务
     * @return 链尾任务；空链返回空
     */
    private TcManualTaskDraft findLastTask(List<TcManualTaskDraft> taskList) {
        return taskList == null || taskList.isEmpty() ? null : taskList.get(taskList.size() - 1);
    }

    /**
     * 按当前批次全局可用工装限制胎侧已排任务，并将溢出量转为未排。
     *
     * @param taskList 当前已排任务
     * @param context 人工滚动上下文
     * @param unplannedList 未排任务收集器
     * @return 应用工装限制后的已排任务
     */
    private List<TcManualTaskDraft> applyToolLimit(List<TcManualTaskDraft> taskList,
                                                   TcManualRollingContext context,
                                                   List<TcManualTaskDraft> unplannedList) {
        if (context.getInitialAvailableToolQty() == null) {
            return taskList;
        }
        BigDecimal availableToolQty = context.getInitialAvailableToolQty().max(BigDecimal.ZERO);
        List<TcManualTaskDraft> sortedTaskList = taskList.stream()
                .sorted(Comparator.comparing(TcManualTaskDraft::getShiftOrder)
                        .thenComparing(TcManualTaskDraft::getMachineCode)
                        .thenComparing(TcManualTaskDraft::getSequence))
                .collect(Collectors.toList());
        List<TcManualTaskDraft> limitedTaskList = new ArrayList<>();
        for (TcManualTaskDraft task : sortedTaskList) {
            BigDecimal originalPlanQty = this.qty(task.getPlanQty());
            BigDecimal limitedPlanQty = this.constraintCalculator.limitPlanQtyByTool(
                    originalPlanQty, availableToolQty, task.getCurlRollLength());
            limitedPlanQty = limitedPlanQty.max(this.qty(task.getFinishQty())).min(originalPlanQty);
            if (limitedPlanQty.signum() > 0) {
                TcManualTaskDraft limitedTask = task.copy();
                limitedTask.setPlanQty(limitedPlanQty);
                limitedTaskList.add(limitedTask);
                availableToolQty = availableToolQty.subtract(
                        this.constraintCalculator.calculateToolUsedQty(
                                limitedPlanQty, task.getCurlRollLength())).max(BigDecimal.ZERO);
            }
            BigDecimal overflowQty = originalPlanQty.subtract(limitedPlanQty).max(BigDecimal.ZERO);
            if (overflowQty.signum() > 0) {
                TcManualTaskDraft overflowTask = task.copy();
                overflowTask.setTaskId(task.getTaskId() + ":TOOL");
                overflowTask.setPlanQty(overflowQty);
                overflowTask.setFinishQty(BigDecimal.ZERO);
                overflowTask.setCarryoverTask(true);
                unplannedList.add(overflowTask);
            }
        }
        return limitedTaskList;
    }

    /** 将每个机台班次顺序强制整理为 1..N。 */
    private void normalizeSequences(List<TcManualTaskDraft> taskList) {
        Map<String, List<TcManualTaskDraft>> chainMap = taskList.stream().collect(Collectors.groupingBy(
                task -> this.capacityKey(task.getMachineCode(), task.getShiftOrder()), LinkedHashMap::new,
                Collectors.toList()));
        chainMap.values().forEach(chain -> {
            chain.sort(this.taskComparator());
            for (int index = 0; index < chain.size(); index++) {
                chain.get(index).setSequence(index + 1);
            }
        });
    }

    /** 强制校验最终数量和链表。 */
    private void validateResult(List<TcManualTaskDraft> scheduledList, List<TcManualTaskDraft> unplannedList,
                                BigDecimal beforeQty, BigDecimal commandDeltaQty, Set<String> initialGroupSet,
                                Set<String> affectedGroupSet) {
        Set<String> uniqueNodeSet = new LinkedHashSet<>();
        for (TcManualTaskDraft task : scheduledList) {
            this.validateLocation(task);
            if (this.qty(task.getPlanQty()).compareTo(this.qty(task.getFinishQty())) < 0
                    || this.qty(task.getFinishQty()).signum() < 0) {
                throw new IllegalStateException("胎侧任务计划量或完成量非法");
            }
            String uniqueKey = task.getResultGroupKey() + "|" + task.getMachineCode() + "|" + task.getShiftOrder();
            if (!uniqueNodeSet.add(uniqueKey)) {
                throw new IllegalStateException("胎侧同一结果分组在同机台同班次重复");
            }
            if (!initialGroupSet.contains(task.getResultGroupKey())
                    && !task.isInsertTask() && !task.getResultGroupKey().startsWith(MOVE_GROUP_PREFIX)) {
                throw new IllegalStateException("胎侧人工滚动输出超出锁定范围");
            }
        }
        BigDecimal afterQty = this.sumQty(scheduledList).add(this.sumQty(unplannedList));
        if (afterQty.compareTo(beforeQty.add(commandDeltaQty)) != 0) {
            throw new IllegalStateException("胎侧人工滚动总数量不守恒");
        }
        affectedGroupSet.remove(null);
    }

    /** 查找指定来源结果和班次任务。 */
    private TcManualTaskDraft findTask(List<TcManualTaskDraft> taskList, TcManualRollingCommand command) {
        return taskList.stream()
                .filter(task -> Objects.equals(command.getResultGroupKey(), task.getResultGroupKey()))
                .filter(task -> command.getSourceShiftOrder() == null
                        || Objects.equals(command.getSourceShiftOrder(), task.getShiftOrder()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("胎侧人工滚动目标任务不存在"));
    }

    /** 合并机台最早受影响位置。 */
    private void registerScope(Map<String, Integer[]> scopeMap, String machineCode,
                               Integer shiftOrder, Integer sequence) {
        int normalizedShift = shiftOrder == null ? 1 : shiftOrder;
        int normalizedSequence = sequence == null ? 1 : sequence;
        Integer[] current = scopeMap.get(machineCode);
        if (current == null || normalizedShift < current[0]
                || (normalizedShift == current[0] && normalizedSequence < current[1])) {
            scopeMap.put(machineCode, new Integer[]{normalizedShift, normalizedSequence});
        }
    }

    /** 校验基础输入。 */
    private void validateInput(TcManualRollingCommandBatch commandBatch, TcManualRollingContext context) {
        if (commandBatch == null || commandBatch.getCommandList() == null
                || commandBatch.getCommandList().isEmpty() || context == null) {
            throw new IllegalArgumentException("胎侧人工滚动上下文和命令不能为空");
        }
    }

    /** 校验任务位置。 */
    private void validateLocation(TcManualTaskDraft task) {
        if (task == null || StrUtil.isBlank(task.getMachineCode()) || task.getShiftOrder() == null
                || task.getShiftOrder() < 1 || task.getShiftOrder() > TcScheduleConstants.TC_MAX_SHIFT_ORDER
                || task.getSequence() == null || task.getSequence() < 1) {
            throw new IllegalArgumentException("胎侧任务机台、班次或顺序非法");
        }
    }

    /** 构造稳定任务排序器。 */
    private Comparator<TcManualTaskDraft> taskComparator() {
        return Comparator.comparing(TcManualTaskDraft::getShiftOrder)
                .thenComparing(this::sequence)
                .thenComparing(task -> task.getOperationOrder() == null ? Integer.MAX_VALUE : task.getOperationOrder())
                .thenComparing(TcManualTaskDraft::getTaskId, Comparator.nullsLast(String::compareTo));
    }

    /** 读取默认顺序。 */
    private int sequence(TcManualTaskDraft task) {
        return task.getSequence() == null ? Integer.MAX_VALUE : task.getSequence();
    }

    /** 构造产能键。 */
    public String capacityKey(String machineCode, Integer shiftOrder) {
        return machineCode + "|" + shiftOrder;
    }

    /** 汇总计划量。 */
    private BigDecimal sumQty(List<TcManualTaskDraft> taskList) {
        return taskList.stream().map(TcManualTaskDraft::getPlanQty).map(this::qty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 将空顺序映射为最大值，保证缺少顺序的任务不会被误判为链尾。
     *
     * @param sequence 任务顺序
     * @return 用于排序的顺序值
     */
    private Integer defaultSequence(Integer sequence) {
        return sequence == null ? Integer.MAX_VALUE : sequence;
    }

    /** 空数量按零处理。 */
    private BigDecimal qty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 从既有结果分组解析结果 ID。 */
    private Long parseResultId(String resultGroupKey) {
        if (resultGroupKey == null || !resultGroupKey.matches("\\d+")) {
            return null;
        }
        return Long.valueOf(resultGroupKey);
    }
}
