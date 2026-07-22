package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.engine.domain.manual.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面人工操作纯滚动引擎。
 *
 * <p>该服务只处理与数据库实体解耦的任务草稿，统一完成插单、删除、调量、转机台和自动滚动后的
 * 机台班次重装箱。数据库锁、事务、审计和实体持久化由 aps-tm 业务层负责。</p>
 */
@Service
public class TmManualRollingEngineService {

    /** 新建结果分组前缀 */
    private static final String NEW_GROUP_PREFIX = "MANUAL:";

    /** 转机台拆分结果分组前缀 */
    private static final String MOVE_GROUP_PREFIX = "MOVE:";

    /**
     * 执行一批人工滚动命令。
     *
     * @param commandBatch 批量命令
     * @param context      运行态上下文
     * @return 完整滚动结果
     * @throws IllegalArgumentException 参数或目标任务非法时抛出
     * @throws IllegalStateException    产能、顺序、数量守恒或任务状态非法时抛出
     */
    public TmManualRollingResult execute(TmManualRollingCommandBatch commandBatch,
                                         TmManualRollingContext context) {
        this.validateInput(commandBatch, context);
        List<TmManualTaskDraft> taskList = context.getTaskList().stream()
                .filter(Objects::nonNull)
                .map(TmManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> initialResultGroupKeySet = taskList.stream()
                .map(TmManualTaskDraft::getResultGroupKey).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        TmManualRollingResult rollingResult = new TmManualRollingResult();
        rollingResult.setBeforeTotalQty(this.sumPlanQty(taskList));

        Map<String, TmManualRollingScope> scopeMap = new LinkedHashMap<>();
        BigDecimal commandDeltaQty = BigDecimal.ZERO;
        List<TmManualRollingCommand> commandList = commandBatch.getCommandList();
        for (int commandIndex = 0; commandIndex < commandList.size(); commandIndex++) {
            TmManualRollingCommand command = commandList.get(commandIndex);
            if (command == null || command.getOperationType() == null) {
                throw new IllegalArgumentException("人工滚动命令及操作类型不能为空");
            }
            command.setCommandOrder(command.getCommandOrder() == null ? commandIndex : command.getCommandOrder());
            BigDecimal beforeCommandQty = this.sumPlanQty(taskList);
            BigDecimal currentCommandDeltaQty = this.applyCommand(taskList, command, scopeMap,
                    rollingResult.getAffectedResultGroupKeySet());
            BigDecimal actualCommandDeltaQty = this.sumPlanQty(taskList).subtract(beforeCommandQty);
            if (currentCommandDeltaQty.compareTo(actualCommandDeltaQty) != 0) {
                throw new IllegalStateException("人工滚动单命令数量不守恒:" + command.getOperationType());
            }
            commandDeltaQty = commandDeltaQty.add(currentCommandDeltaQty);
        }

        List<TmManualTaskDraft> unplannedTaskList = new ArrayList<>();
        List<TmManualRollingScope> scopeList = scopeMap.values().stream()
                .sorted(Comparator.comparing(TmManualRollingScope::getMachineCode))
                .collect(Collectors.toList());
        rollingResult.setChainChangeSummaryList(scopeList.stream()
                .map(scope -> scope.getMachineCode() + ":CLASS" + scope.getStartShiftOrder()
                        + ":SEQ" + scope.getStartSequence())
                .collect(Collectors.toList()));
        for (TmManualRollingScope scope : scopeList) {
            taskList = this.repackMachine(taskList, scope, context, unplannedTaskList);
        }

        MachineShiftTaskChain<TmManualTaskDraft> taskChainGroup = this.buildTaskChains(taskList, context);
        this.validateResult(taskList, unplannedTaskList, taskChainGroup, context,
                rollingResult.getBeforeTotalQty(), commandDeltaQty,
                initialResultGroupKeySet, rollingResult.getAffectedResultGroupKeySet());
        rollingResult.setScheduledTaskList(taskList);
        rollingResult.setUnplannedTaskList(unplannedTaskList);
        rollingResult.setTaskChainGroup(taskChainGroup);
        rollingResult.setCommandDeltaQty(commandDeltaQty);
        rollingResult.setScheduledTotalQty(this.sumPlanQty(taskList));
        rollingResult.setUnplannedTotalQty(this.sumPlanQty(unplannedTaskList));
        rollingResult.setAffectedResultIdSet(rollingResult.getAffectedResultGroupKeySet().stream()
                .map(this::parseResultId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        context.setTaskList(taskList);
        context.setTaskChainGroup(taskChainGroup);
        return rollingResult;
    }

    /**
     * 应用单条业务命令并返回计划量净变化。
     *
     * @param taskList                 当前任务
     * @param command                  业务命令
     * @param scopeMap                 机台影响范围
     * @param affectedResultGroupKeySet 受影响结果分组
     * @return 命令造成的计划量净变化
     */
    private BigDecimal applyCommand(List<TmManualTaskDraft> taskList, TmManualRollingCommand command,
                                    Map<String, TmManualRollingScope> scopeMap,
                                    Set<String> affectedResultGroupKeySet) {
        TmManualRollingOperationEnum operationType = command.getOperationType();
        if (TmManualRollingOperationEnum.INSERT == operationType) {
            return this.applyInsert(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        if (TmManualRollingOperationEnum.DELETE == operationType) {
            return this.applyDelete(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        if (TmManualRollingOperationEnum.CHANGE_MACHINE == operationType) {
            return this.applyChangeMachine(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        return this.applyChangeQty(taskList, command, scopeMap, affectedResultGroupKeySet);
    }

    /**
     * 应用人工插单命令。
     *
     * @param taskList 当前任务
     * @param command  插单命令
     * @param scopeMap 影响范围
     * @param affectedResultGroupKeySet 受影响结果分组
     * @return 插单增加量
     */
    private BigDecimal applyInsert(List<TmManualTaskDraft> taskList, TmManualRollingCommand command,
                                   Map<String, TmManualRollingScope> scopeMap,
                                   Set<String> affectedResultGroupKeySet) {
        if (command.getInsertTask() == null) {
            throw new IllegalArgumentException("人工插单任务不能为空");
        }
        TmManualTaskDraft insertTask = command.getInsertTask().copy();
        int commandOrder = command.getCommandOrder() == null ? 0 : command.getCommandOrder();
        if (StrUtil.isBlank(insertTask.getResultGroupKey())) {
            insertTask.setResultGroupKey(NEW_GROUP_PREFIX + commandOrder + ":" + insertTask.getTaskId());
        }
        insertTask.setMachineCode(StrUtil.blankToDefault(command.getTargetMachineCode(), insertTask.getMachineCode()));
        insertTask.setShiftOrder(command.getTargetShiftOrder() == null
                ? insertTask.getShiftOrder() : command.getTargetShiftOrder());
        insertTask.setSequence(command.getTargetSequence() == null
                ? insertTask.getSequence() : command.getTargetSequence());
        insertTask.setSourceShiftOrder(insertTask.getShiftOrder());
        insertTask.setSourceSequence(insertTask.getSequence());
        insertTask.setMinimumShiftOrder(insertTask.getShiftOrder());
        insertTask.setInsertTask(true);
        this.markOperationPriority(insertTask, commandOrder);
        this.validateTaskLocation(insertTask);
        taskList.add(insertTask);
        this.registerScope(scopeMap, insertTask.getMachineCode(), insertTask.getShiftOrder(), insertTask.getSequence());
        affectedResultGroupKeySet.add(insertTask.getResultGroupKey());
        return this.nvl(insertTask.getPlanQty());
    }

    /**
     * 应用删除命令。
     *
     * @param taskList 当前任务
     * @param command  删除命令
     * @param scopeMap 影响范围
     * @param affectedResultGroupKeySet 受影响结果分组
     * @return 删除造成的负向变化量
     */
    private BigDecimal applyDelete(List<TmManualTaskDraft> taskList, TmManualRollingCommand command,
                                   Map<String, TmManualRollingScope> scopeMap,
                                   Set<String> affectedResultGroupKeySet) {
        List<TmManualTaskDraft> deleteTaskList = taskList.stream()
                .filter(task -> Objects.equals(command.getResultGroupKey(), task.getResultGroupKey()))
                .collect(Collectors.toList());
        if (deleteTaskList.isEmpty()) {
            throw new IllegalArgumentException("待删除任务不存在:" + command.getResultGroupKey());
        }
        BigDecimal deleteQty = BigDecimal.ZERO;
        for (TmManualTaskDraft deleteTask : deleteTaskList) {
            if (this.nvl(deleteTask.getFinishQty()).compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalStateException("已存在完成量的任务不允许删除:" + deleteTask.getTaskId());
            }
            deleteQty = deleteQty.add(this.nvl(deleteTask.getPlanQty()));
            this.registerScope(scopeMap, deleteTask.getMachineCode(), deleteTask.getShiftOrder(), deleteTask.getSequence());
        }
        taskList.removeAll(deleteTaskList);
        affectedResultGroupKeySet.add(command.getResultGroupKey());
        return deleteQty.negate();
    }

    /**
     * 应用调量或自动滚动命令。
     *
     * @param taskList 当前任务
     * @param command  调量命令
     * @param scopeMap 影响范围
     * @param affectedResultGroupKeySet 受影响结果分组
     * @return 调量净变化
     */
    private BigDecimal applyChangeQty(List<TmManualTaskDraft> taskList, TmManualRollingCommand command,
                                      Map<String, TmManualRollingScope> scopeMap,
                                      Set<String> affectedResultGroupKeySet) {
        TmManualTaskDraft targetTask = this.findTask(taskList, command);
        BigDecimal targetPlanQty = this.nvl(command.getPlanQty());
        if (targetPlanQty.compareTo(this.nvl(targetTask.getFinishQty())) < 0) {
            throw new IllegalStateException("调整后计划量不能小于完成量:" + targetTask.getTaskId());
        }
        BigDecimal deltaQty = targetPlanQty.subtract(this.nvl(targetTask.getPlanQty()));
        targetTask.setPlanQty(targetPlanQty);
        if (command.getAnalysis() != null) {
            targetTask.setAnalysis(command.getAnalysis());
        }
        this.markOperationPriority(targetTask, command.getCommandOrder());
        this.registerScope(scopeMap, targetTask.getMachineCode(), targetTask.getShiftOrder(), targetTask.getSequence());
        affectedResultGroupKeySet.add(targetTask.getResultGroupKey());
        return deltaQty;
    }

    /**
     * 应用转机台命令，完成量留在来源机台，仅转移未完成部分。
     *
     * @param taskList 当前任务
     * @param command  转机命令
     * @param scopeMap 影响范围
     * @param affectedResultGroupKeySet 受影响结果分组
     * @return 转机台净变化，恒为0
     */
    private BigDecimal applyChangeMachine(List<TmManualTaskDraft> taskList, TmManualRollingCommand command,
                                          Map<String, TmManualRollingScope> scopeMap,
                                          Set<String> affectedResultGroupKeySet) {
        TmManualTaskDraft sourceTask = this.findTask(taskList, command);
        this.registerScope(scopeMap, sourceTask.getMachineCode(), sourceTask.getShiftOrder(), sourceTask.getSequence());
        BigDecimal finishQty = this.nvl(sourceTask.getFinishQty());
        BigDecimal moveQty = this.nvl(sourceTask.getPlanQty()).subtract(finishQty);
        if (moveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("任务没有可转移的未完成计划量:" + sourceTask.getTaskId());
        }

        TmManualTaskDraft moveTask = sourceTask.copy();
        moveTask.setResultGroupKey(MOVE_GROUP_PREFIX + sourceTask.getResultGroupKey() + ":" + command.getCommandOrder());
        moveTask.setTaskId(moveTask.getTaskId() + ":MOVE:" + command.getCommandOrder());
        if (finishQty.compareTo(BigDecimal.ZERO) > 0) {
            sourceTask.setPlanQty(finishQty);
            moveTask.setFinishQty(BigDecimal.ZERO);
        } else {
            taskList.remove(sourceTask);
        }
        moveTask.setPlanQty(moveQty);
        moveTask.setMachineCode(command.getTargetMachineCode());
        moveTask.setShiftOrder(command.getTargetShiftOrder());
        Integer targetSequence = command.getTargetSequence();
        if (targetSequence == null) {
            targetSequence = taskList.stream()
                    .filter(task -> Objects.equals(command.getTargetMachineCode(), task.getMachineCode()))
                    .filter(task -> Objects.equals(command.getTargetShiftOrder(), task.getShiftOrder()))
                    .map(TmManualTaskDraft::getSequence).filter(Objects::nonNull)
                    .max(Integer::compareTo).map(sequence -> sequence + 1).orElse(1);
        }
        moveTask.setSequence(targetSequence);
        this.markOperationPriority(moveTask, command.getCommandOrder());
        this.validateTaskLocation(moveTask);

        TmManualTaskDraft compatibleTarget = taskList.stream()
                .filter(task -> Objects.equals(moveTask.getMachineCode(), task.getMachineCode()))
                .filter(task -> Objects.equals(moveTask.getTreadCode(), task.getTreadCode()))
                .sorted(Comparator.comparing(TmManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence())))
                .findFirst().orElse(null);
        TmManualTaskDraft mergeTarget = taskList.stream()
                .filter(task -> compatibleTarget != null
                        && Objects.equals(compatibleTarget.getResultGroupKey(), task.getResultGroupKey()))
                .filter(task -> Objects.equals(moveTask.getMachineCode(), task.getMachineCode()))
                .filter(task -> Objects.equals(moveTask.getShiftOrder(), task.getShiftOrder()))
                .findFirst().orElse(null);
        if (mergeTarget == null) {
            if (compatibleTarget != null) {
                moveTask.setResultGroupKey(compatibleTarget.getResultGroupKey());
                moveTask.setSourceResultId(compatibleTarget.getSourceResultId());
            }
            taskList.add(moveTask);
            affectedResultGroupKeySet.add(moveTask.getResultGroupKey());
        } else {
            mergeTarget.setPlanQty(this.nvl(mergeTarget.getPlanQty()).add(moveQty));
            this.markOperationPriority(mergeTarget, command.getCommandOrder());
            affectedResultGroupKeySet.add(mergeTarget.getResultGroupKey());
        }
        affectedResultGroupKeySet.add(sourceTask.getResultGroupKey());
        Integer targetScopeSequence = mergeTarget == null ? targetSequence : mergeTarget.getSequence();
        this.registerScope(scopeMap, command.getTargetMachineCode(), command.getTargetShiftOrder(), targetScopeSequence);
        return BigDecimal.ZERO;
    }

    /**
     * 对单机台受影响窗口执行连续重装箱。
     *
     * @param allTaskList       全部任务
     * @param scope             当前机台影响范围
     * @param context           运行态上下文
     * @param unplannedTaskList 未排任务收集器
     * @return 重装箱后的全部任务
     */
    private List<TmManualTaskDraft> repackMachine(List<TmManualTaskDraft> allTaskList,
                                                  TmManualRollingScope scope,
                                                  TmManualRollingContext context,
                                                  List<TmManualTaskDraft> unplannedTaskList) {
        BigDecimal machineCapacity = context.getMachineCapacityMap().get(scope.getMachineCode());
        if (machineCapacity == null || machineCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("机台最大班产未维护:" + scope.getMachineCode());
        }
        int startShiftOrder = scope.getStartShiftOrder() == null ? 1 : scope.getStartShiftOrder();
        int startSequence = scope.getStartSequence() == null ? 1 : scope.getStartSequence();
        List<TmManualTaskDraft> unaffectedTaskList = allTaskList.stream()
                .filter(task -> !Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .map(TmManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        List<TmManualTaskDraft> prefixTaskList = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() < startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) < startSequence))
                .map(TmManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        List<TmManualTaskDraft> lockedFinishTaskList = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) >= startSequence))
                .filter(task -> this.nvl(task.getFinishQty()).compareTo(BigDecimal.ZERO) > 0)
                .map(task -> this.buildFinishedLockedTask(task))
                .collect(Collectors.toCollection(ArrayList::new));
        List<TmManualTaskDraft> rollingQueue = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) >= startSequence))
                .map(TmManualTaskDraft::copy)
                .map(task -> this.buildUnfinishedTask(task))
                .filter(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(this.taskOrderComparator())
                .collect(Collectors.toCollection(ArrayList::new));

        List<TmManualTaskDraft> repackedTaskList = new ArrayList<>(unaffectedTaskList);
        repackedTaskList.addAll(prefixTaskList);
        repackedTaskList.addAll(lockedFinishTaskList);
        int fragmentIndex = 1;
        for (int shiftOrder = startShiftOrder; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            final int currentShiftOrder = shiftOrder;
            List<TmManualTaskDraft> currentShiftTaskList = repackedTaskList.stream()
                    .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                    .filter(task -> Objects.equals(currentShiftOrder, task.getShiftOrder()))
                    .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                    .collect(Collectors.toList());
            BigDecimal usedCapacity = currentShiftTaskList.stream()
                    .map(TmManualTaskDraft::getPlanQty).map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remainCapacity = machineCapacity.subtract(usedCapacity).max(BigDecimal.ZERO);
            int nextSequence = currentShiftTaskList.size() + 1;
            while (!rollingQueue.isEmpty() && remainCapacity.compareTo(BigDecimal.ZERO) > 0) {
                TmManualTaskDraft currentTask = rollingQueue.get(0);
                if (currentTask.getMinimumShiftOrder() != null
                        && shiftOrder < currentTask.getMinimumShiftOrder()) {
                    break;
                }
                rollingQueue.remove(0);
                BigDecimal currentPlanQty = this.nvl(currentTask.getPlanQty());
                BigDecimal assignedQty = currentPlanQty.min(remainCapacity);
                if (this.nvl(currentTask.getFinishQty()).compareTo(assignedQty) > 0) {
                    assignedQty = this.nvl(currentTask.getFinishQty());
                }
                if (assignedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                TmManualTaskDraft mergeTarget = this.findSameGroupTask(repackedTaskList,
                        scope.getMachineCode(), shiftOrder, currentTask.getResultGroupKey());
                if (mergeTarget == null) {
                    TmManualTaskDraft assignedTask = currentTask.copy();
                    assignedTask.setShiftOrder(shiftOrder);
                    assignedTask.setSequence(nextSequence++);
                    assignedTask.setPlanQty(assignedQty);
                    assignedTask.setFinishQty(this.nvl(currentTask.getFinishQty()).min(assignedQty));
                    assignedTask.setOperationPriority(false);
                    repackedTaskList.add(assignedTask);
                } else {
                    mergeTarget.setPlanQty(this.nvl(mergeTarget.getPlanQty()).add(assignedQty));
                    mergeTarget.setFinishQty(this.nvl(mergeTarget.getFinishQty())
                            .add(this.nvl(currentTask.getFinishQty()).min(assignedQty)));
                }
                remainCapacity = remainCapacity.subtract(assignedQty).max(BigDecimal.ZERO);
                BigDecimal overflowQty = currentPlanQty.subtract(assignedQty);
                if (overflowQty.compareTo(BigDecimal.ZERO) > 0) {
                    TmManualTaskDraft carryTask = currentTask.copy();
                    carryTask.setTaskId(currentTask.getTaskId() + ":CARRY:" + fragmentIndex++);
                    carryTask.setPlanQty(overflowQty);
                    carryTask.setFinishQty(BigDecimal.ZERO);
                    carryTask.setCarryoverTask(true);
                    carryTask.setOperationPriority(true);
                    rollingQueue.add(0, carryTask);
                    break;
                }
            }
        }
        for (TmManualTaskDraft remainTask : rollingQueue) {
            if (this.nvl(remainTask.getPlanQty()).compareTo(BigDecimal.ZERO) > 0) {
                unplannedTaskList.add(remainTask.copy());
            }
        }
        return repackedTaskList;
    }

    /**
     * 构建最终机台班次任务链并以链表结果统一重排顺序。
     *
     * @param taskList 最终任务
     * @param context  运行态上下文
     * @return 机台班次任务链
     */
    private MachineShiftTaskChain<TmManualTaskDraft> buildTaskChains(List<TmManualTaskDraft> taskList,
                                                                      TmManualRollingContext context) {
        MachineShiftTaskChain<TmManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        List<TmManualTaskDraft> sortedTaskList = taskList.stream()
                .filter(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TmManualTaskDraft::getMachineCode)
                        .thenComparing(TmManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence()))
                        .thenComparing(task -> StrUtil.blankToDefault(task.getTaskId(), "")))
                .collect(Collectors.toList());
        for (TmManualTaskDraft task : sortedTaskList) {
            ScheduleTaskLinkedList<TmManualTaskDraft> chain = taskChainGroup.getOrCreate(
                    task.getMachineCode(), scheduleDate, task.getShiftOrder());
            ScheduleTaskNode<TmManualTaskDraft> node = new ScheduleTaskNode<>(task.getTaskId(), task,
                    task.getMachineCode(), scheduleDate, "CLASS" + task.getShiftOrder(),
                    task.getShiftOrder(), task.getPlanQty());
            chain.append(node, new ScheduleOperationContext(context.getOperator(),
                    "TM_MANUAL_ROLLING", context.getTraceId()));
            task.setSequence(node.getSequence());
        }
        return taskChainGroup;
    }

    /**
     * 校验最终顺序、分组唯一性、完成量和数量守恒。
     *
     * @param taskList          已排任务
     * @param unplannedTaskList 未排任务
     * @param taskChainGroup    最终任务链
     * @param context           运行态上下文
     * @param beforeTotalQty    计算前总量
     * @param commandDeltaQty   命令净变化量
     * @param initialResultGroupKeySet 锁定快照中的结果分组
     * @param affectedResultGroupKeySet 本次命令影响的结果分组
     */
    private void validateResult(List<TmManualTaskDraft> taskList,
                                List<TmManualTaskDraft> unplannedTaskList,
                                MachineShiftTaskChain<TmManualTaskDraft> taskChainGroup,
                                TmManualRollingContext context,
                                BigDecimal beforeTotalQty,
                                BigDecimal commandDeltaQty,
                                Set<String> initialResultGroupKeySet,
                                Set<String> affectedResultGroupKeySet) {
        Set<String> slotKeySet = new LinkedHashSet<>();
        for (TmManualTaskDraft task : taskList) {
            this.validateTaskLocation(task);
            if (this.nvl(task.getFinishQty()).compareTo(BigDecimal.ZERO) < 0
                    || this.nvl(task.getPlanQty()).compareTo(this.nvl(task.getFinishQty())) < 0) {
                throw new IllegalStateException("计划量或完成量非法:" + task.getTaskId());
            }
            String slotKey = task.getResultGroupKey() + "|" + task.getMachineCode() + "|" + task.getShiftOrder();
            if (!slotKeySet.add(slotKey)) {
                throw new IllegalStateException("同一结果分组在同机台同班次重复落位:" + slotKey);
            }
        }
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        Map<String, List<TmManualTaskDraft>> machineShiftMap = taskList.stream()
                .collect(Collectors.groupingBy(task -> task.getMachineCode() + "|" + task.getShiftOrder(),
                        LinkedHashMap::new, Collectors.toList()));
        boolean resultGroupCrossMachine = taskList.stream()
                .collect(Collectors.groupingBy(TmManualTaskDraft::getResultGroupKey,
                        Collectors.mapping(TmManualTaskDraft::getMachineCode, Collectors.toSet())))
                .values().stream().anyMatch(machineCodeSet -> machineCodeSet.size() > 1);
        if (resultGroupCrossMachine) {
            throw new IllegalStateException("同一结果分组不允许跨机台装配");
        }
        for (Map.Entry<String, List<TmManualTaskDraft>> entry : machineShiftMap.entrySet()) {
            List<TmManualTaskDraft> shiftTaskList = entry.getValue().stream()
                    .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                    .collect(Collectors.toList());
            for (int index = 0; index < shiftTaskList.size(); index++) {
                if (!Objects.equals(index + 1, shiftTaskList.get(index).getSequence())) {
                    throw new IllegalStateException("机台班次顺序不连续:" + entry.getKey());
                }
            }
            String[] keyParts = entry.getKey().split("\\|");
            if (taskChainGroup.get(keyParts[0], scheduleDate, Integer.parseInt(keyParts[1])) == null) {
                throw new IllegalStateException("机台班次任务链缺失:" + entry.getKey());
            }
        }
        BigDecimal actualTotalQty = this.sumPlanQty(taskList).add(this.sumPlanQty(unplannedTaskList));
        BigDecimal expectedTotalQty = beforeTotalQty.add(commandDeltaQty);
        if (expectedTotalQty.compareTo(actualTotalQty) != 0) {
            throw new IllegalStateException("人工滚动数量不守恒, expected=" + expectedTotalQty
                    + ", actual=" + actualTotalQty);
        }
        List<TmManualTaskDraft> outputTaskList = new ArrayList<>(taskList);
        outputTaskList.addAll(unplannedTaskList);
        boolean outsideMachineScope = outputTaskList.stream()
                .anyMatch(task -> !context.getMachineCapacityMap().containsKey(task.getMachineCode()));
        boolean outsideResultScope = outputTaskList.stream()
                .anyMatch(task -> !initialResultGroupKeySet.contains(task.getResultGroupKey())
                        && !task.isInsertTask()
                        && !task.getResultGroupKey().startsWith(MOVE_GROUP_PREFIX));
        if (outsideMachineScope || outsideResultScope) {
            throw new IllegalStateException("人工滚动输出超出本次锁定范围");
        }
    }

    /**
     * 将排程日期转换为本地日期。
     *
     * <p>通过毫秒时间戳转换，兼容 {@link java.sql.Date} 不支持 {@code toInstant()} 的实现。</p>
     *
     * @param scheduleDate 排程日期
     * @return 本地日期
     * @throws IllegalArgumentException 排程日期为空时抛出
     */
    private LocalDate toLocalDate(Date scheduleDate) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("排程日期不能为空");
        }
        return Instant.ofEpochMilli(scheduleDate.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 按命令定位唯一任务。
     *
     * @param taskList 当前任务
     * @param command  业务命令
     * @return 目标任务
     */
    private TmManualTaskDraft findTask(List<TmManualTaskDraft> taskList, TmManualRollingCommand command) {
        List<TmManualTaskDraft> candidateList = taskList.stream()
                .filter(task -> Objects.equals(command.getResultGroupKey(), task.getResultGroupKey()))
                .filter(task -> command.getSourceShiftOrder() == null
                        || Objects.equals(command.getSourceShiftOrder(), task.getShiftOrder()))
                .filter(task -> StrUtil.isBlank(command.getSourceMachineCode())
                        || Objects.equals(command.getSourceMachineCode(), task.getMachineCode()))
                .collect(Collectors.toList());
        if (candidateList.size() != 1) {
            throw new IllegalArgumentException("人工滚动目标任务不唯一:" + command.getResultGroupKey()
                    + ", shiftOrder=" + command.getSourceShiftOrder());
        }
        return candidateList.get(0);
    }

    /**
     * 查找已写入同一结果分组、机台和班次的任务。
     *
     * @param taskList       当前输出任务
     * @param machineCode    机台编码
     * @param shiftOrder     班次
     * @param resultGroupKey 结果分组
     * @return 已有任务，不存在返回空
     */
    private TmManualTaskDraft findSameGroupTask(List<TmManualTaskDraft> taskList, String machineCode,
                                                int shiftOrder, String resultGroupKey) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .filter(task -> Objects.equals(resultGroupKey, task.getResultGroupKey()))
                .findFirst().orElse(null);
    }

    /**
     * 构造固定在来源结果和来源班次的完成量片段。
     *
     * @param sourceTask 来源任务
     * @return 只承载完成量的固定片段
     */
    private TmManualTaskDraft buildFinishedLockedTask(TmManualTaskDraft sourceTask) {
        TmManualTaskDraft lockedTask = sourceTask.copy();
        lockedTask.setPlanQty(this.nvl(sourceTask.getFinishQty()));
        return lockedTask;
    }

    /**
     * 构造可参与滚动的未完成量片段。
     *
     * @param sourceTask 来源任务副本
     * @return 未完成量片段；无未完成量时计划量为0
     */
    private TmManualTaskDraft buildUnfinishedTask(TmManualTaskDraft sourceTask) {
        BigDecimal unfinishedQty = this.nvl(sourceTask.getPlanQty())
                .subtract(this.nvl(sourceTask.getFinishQty())).max(BigDecimal.ZERO);
        sourceTask.setTaskId(sourceTask.getTaskId() + ":UNFINISHED");
        sourceTask.setPlanQty(unfinishedQty);
        sourceTask.setFinishQty(BigDecimal.ZERO);
        return sourceTask;
    }

    /**
     * 注册机台最早影响范围。
     *
     * @param scopeMap    范围集合
     * @param machineCode 机台编码
     * @param shiftOrder  班次
     * @param sequence    顺序
     */
    private void registerScope(Map<String, TmManualRollingScope> scopeMap, String machineCode,
                               Integer shiftOrder, Integer sequence) {
        if (StrUtil.isBlank(machineCode)) {
            throw new IllegalArgumentException("人工滚动机台不能为空");
        }
        TmManualRollingScope scope = scopeMap.computeIfAbsent(machineCode, key -> {
            TmManualRollingScope target = new TmManualRollingScope();
            target.setMachineCode(key);
            return target;
        });
        scope.merge(shiftOrder, sequence);
    }

    /**
     * 标记任务在相同原顺序下优先参与本次操作。
     *
     * @param task         任务
     * @param commandOrder 命令顺序
     */
    private void markOperationPriority(TmManualTaskDraft task, Integer commandOrder) {
        task.setOperationPriority(true);
        task.setOperationOrder(commandOrder == null ? 0 : commandOrder);
    }

    /**
     * 构造确定性的原任务队列顺序。
     *
     * @return 任务比较器
     */
    private Comparator<TmManualTaskDraft> taskOrderComparator() {
        return Comparator.comparing(TmManualTaskDraft::getShiftOrder)
                .thenComparing(task -> this.defaultSequence(task.getSequence()))
                .thenComparing(task -> task.isOperationPriority() ? 0 : 1)
                .thenComparing(task -> task.getOperationOrder() == null ? Integer.MAX_VALUE : task.getOperationOrder())
                .thenComparing(task -> StrUtil.blankToDefault(task.getTaskId(), ""));
    }

    /**
     * 校验引擎输入。
     *
     * @param commandBatch 批量命令
     * @param context      运行态上下文
     */
    private void validateInput(TmManualRollingCommandBatch commandBatch, TmManualRollingContext context) {
        if (commandBatch == null || commandBatch.getCommandList() == null
                || commandBatch.getCommandList().isEmpty()) {
            throw new IllegalArgumentException("人工滚动命令不能为空");
        }
        if (context == null || context.getScheduleDate() == null) {
            throw new IllegalArgumentException("人工滚动上下文及排程日期不能为空");
        }
        if (context.getTaskList() == null || context.getMachineCapacityMap() == null) {
            throw new IllegalArgumentException("人工滚动任务或机台产能不能为空");
        }
    }

    /**
     * 校验任务定位字段。
     *
     * @param task 任务
     */
    private void validateTaskLocation(TmManualTaskDraft task) {
        if (task == null || StrUtil.isBlank(task.getTaskId()) || StrUtil.isBlank(task.getResultGroupKey())
                || StrUtil.isBlank(task.getMachineCode()) || task.getShiftOrder() == null
                || task.getShiftOrder() < 1 || task.getShiftOrder() > TmScheduleConstants.TM_MAX_SHIFT_ORDER
                || task.getSequence() == null || task.getSequence() < 1) {
            throw new IllegalStateException("人工滚动任务定位字段非法");
        }
    }

    /**
     * 汇总任务计划量。
     *
     * @param taskList 任务集合
     * @return 计划量合计
     */
    private BigDecimal sumPlanQty(List<TmManualTaskDraft> taskList) {
        if (taskList == null) {
            return BigDecimal.ZERO;
        }
        return taskList.stream().filter(Objects::nonNull).map(TmManualTaskDraft::getPlanQty)
                .map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 空数值转0。
     *
     * @param value 原数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 空顺序排在末尾。
     *
     * @param sequence 顺序
     * @return 排序值
     */
    private Integer defaultSequence(Integer sequence) {
        return sequence == null ? Integer.MAX_VALUE : sequence;
    }

    /**
     * 将既有结果分组转换为数据库结果ID。
     *
     * @param resultGroupKey 结果分组
     * @return 数字结果ID，新分组返回空
     */
    private Long parseResultId(String resultGroupKey) {
        if (StrUtil.isBlank(resultGroupKey)) {
            return null;
        }
        try {
            return Long.valueOf(resultGroupKey);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
