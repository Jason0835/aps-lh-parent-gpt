package com.zlt.aps.tq.engine.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintCalculator;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingCommand;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingCommandBatch;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingContext;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingOperationEnum;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingResult;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingScope;
import com.zlt.aps.tq.engine.domain.manual.TqManualTaskDraft;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 胎圈人工操作纯滚动引擎。
 *
 * <p>统一完成插单、删除、调量、转机台的纯计算。数据库锁、事务、审计和实体持久化
 * 由 aps-tq 业务层负责。与胎面差异：无工装账本、规格切换按 beadCode、repackMachine
 * 简化为按机台定额重装箱。</p>
 */
@Service
public class TqManualRollingEngineService {

    /** 新建结果分组前缀 */
    private static final String NEW_GROUP_PREFIX = "MANUAL:";

    /** 转机台拆分结果分组前缀 */
    private static final String MOVE_GROUP_PREFIX = "MOVE:";

    /** 胎圈最大班次数（6班次跨3天） */
    private static final int TQ_MAX_SHIFT_ORDER = 6;

    /** 规格切换约束纯计算器 */
    private final ScheduleConstraintCalculator constraintCalculator = new ScheduleConstraintCalculator();

    /**
     * 执行一批人工滚动命令。
     *
     * @param commandBatch 批量命令
     * @param context      运行态上下文
     * @return 完整滚动结果
     */
    public TqManualRollingResult execute(TqManualRollingCommandBatch commandBatch,
                                         TqManualRollingContext context) {
        this.validateInput(commandBatch, context);
        // 1. 深拷贝任务列表，避免污染上下文
        List<TqManualTaskDraft> taskList = context.getTaskList().stream()
                .filter(Objects::nonNull)
                .map(TqManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> initialResultGroupKeySet = taskList.stream()
                .map(TqManualTaskDraft::getResultGroupKey).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        TqManualRollingResult rollingResult = new TqManualRollingResult();
        rollingResult.setBeforeTotalQty(this.sumPlanQty(taskList));
        List<TqManualTaskDraft> unplannedTaskList = new ArrayList<>();

        // 2. 遍历命令批次，分派到 applyInsert/applyDelete/applyChangeMachine/applyChangeQty
        Map<String, TqManualRollingScope> scopeMap = new LinkedHashMap<>();
        BigDecimal commandDeltaQty = BigDecimal.ZERO;
        List<TqManualRollingCommand> commandList = commandBatch.getCommandList();
        for (int commandIndex = 0; commandIndex < commandList.size(); commandIndex++) {
            TqManualRollingCommand command = commandList.get(commandIndex);
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

        // 3. 清理无效任务
        taskList.removeIf(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) <= 0
                && this.nvl(task.getFinishQty()).compareTo(BigDecimal.ZERO) <= 0);

        // 4. 按机台影响范围执行 repackMachine（重装箱，处理跨班顺延）
        List<TqManualRollingScope> scopeList = scopeMap.values().stream()
                .sorted(Comparator.comparing(TqManualRollingScope::getMachineCode))
                .collect(Collectors.toList());
        rollingResult.setChainChangeSummaryList(scopeList.stream()
                .map(scope -> scope.getMachineCode() + ":CLASS" + scope.getStartShiftOrder()
                        + ":SEQ" + scope.getStartSequence())
                .collect(Collectors.toList()));
        for (TqManualRollingScope scope : scopeList) {
            taskList = this.repackMachine(taskList, scope, context, unplannedTaskList);
        }

        // 5. 构建任务链并 resequence 重排顺位
        MachineShiftTaskChain<TqManualTaskDraft> taskChainGroup = this.buildTaskChains(taskList, context);

        // 6. 校验结果
        this.validateResult(taskList, unplannedTaskList, taskChainGroup, context,
                rollingResult.getBeforeTotalQty(), commandDeltaQty,
                initialResultGroupKeySet, rollingResult.getAffectedResultGroupKeySet());

        // 7. 填充返回结果
        rollingResult.setScheduledTaskList(taskList);
        rollingResult.setUnplannedTaskList(unplannedTaskList);
        rollingResult.setTaskChainGroup(taskChainGroup);
        rollingResult.setCommandDeltaQty(commandDeltaQty);
        rollingResult.setScheduledTotalQty(this.sumPlanQty(taskList));
        rollingResult.setUnplannedTotalQty(this.sumPlanQty(unplannedTaskList));
        rollingResult.setAffectedResultIdSet(rollingResult.getAffectedResultGroupKeySet().stream()
                .map(this::parseResultId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Set<Long> scheduledSourceIdSet = taskList.stream().map(TqManualTaskDraft::getSourceResultId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> explicitDeleteResultIdSet = commandList.stream()
                .filter(cmd -> cmd != null && TqManualRollingOperationEnum.DELETE == cmd.getOperationType())
                .map(TqManualRollingCommand::getResultGroupKey).map(this::parseResultId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        commandList.stream()
                .filter(cmd -> cmd != null && TqManualRollingOperationEnum.CHANGE_MACHINE == cmd.getOperationType())
                .map(TqManualRollingCommand::getResultGroupKey).map(this::parseResultId)
                .filter(Objects::nonNull).filter(resultId -> !scheduledSourceIdSet.contains(resultId))
                .forEach(explicitDeleteResultIdSet::add);
        rollingResult.setExplicitDeleteResultIdSet(explicitDeleteResultIdSet);
        rollingResult.setContainsNonDeleteOperation(commandList.stream()
                .filter(Objects::nonNull)
                .anyMatch(cmd -> TqManualRollingOperationEnum.DELETE != cmd.getOperationType()));
        Set<String> scheduledGroupKeySet = taskList.stream().map(TqManualTaskDraft::getResultGroupKey)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        rollingResult.setMoveToUnplannedResultIdSet(unplannedTaskList.stream()
                .filter(task -> !scheduledGroupKeySet.contains(task.getResultGroupKey()))
                .map(TqManualTaskDraft::getSourceResultId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        context.setTaskList(taskList);
        context.setTaskChainGroup(taskChainGroup);
        return rollingResult;
    }

    /**
     * 分派单条命令到对应处理器。
     */
    private BigDecimal applyCommand(List<TqManualTaskDraft> taskList, TqManualRollingCommand command,
                                    Map<String, TqManualRollingScope> scopeMap,
                                    Set<String> affectedResultGroupKeySet) {
        TqManualRollingOperationEnum operationType = command.getOperationType();
        if (TqManualRollingOperationEnum.INSERT == operationType) {
            return this.applyInsert(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        if (TqManualRollingOperationEnum.DELETE == operationType) {
            return this.applyDelete(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        if (TqManualRollingOperationEnum.CHANGE_MACHINE == operationType) {
            return this.applyChangeMachine(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        return this.applyChangeQty(taskList, command, scopeMap, affectedResultGroupKeySet);
    }

    /**
     * 应用人工插单命令（支持锚点插入）。
     */
    private BigDecimal applyInsert(List<TqManualTaskDraft> taskList, TqManualRollingCommand command,
                                   Map<String, TqManualRollingScope> scopeMap,
                                   Set<String> affectedResultGroupKeySet) {
        if (command.getInsertTask() == null) {
            throw new IllegalArgumentException("人工插单任务不能为空");
        }
        TqManualTaskDraft insertTask = command.getInsertTask().copy();
        int commandOrder = command.getCommandOrder() == null ? 0 : command.getCommandOrder();
        if (StrUtil.isBlank(insertTask.getResultGroupKey())) {
            insertTask.setResultGroupKey(NEW_GROUP_PREFIX + commandOrder + ":" + insertTask.getTaskId());
        }
        insertTask.setMachineCode(StrUtil.blankToDefault(command.getTargetMachineCode(), insertTask.getMachineCode()));
        insertTask.setShiftOrder(command.getTargetShiftOrder() == null
                ? insertTask.getShiftOrder() : command.getTargetShiftOrder());
        // 锚点优先：若提供 anchorTaskId，在锚点任务之后插入；否则用 targetSequence
        if (StrUtil.isNotBlank(command.getAnchorTaskId())) {
            Integer anchorSequence = this.resolveAnchorSequence(taskList, command.getAnchorTaskId(),
                    insertTask.getMachineCode(), insertTask.getShiftOrder());
            insertTask.setSequence(anchorSequence == null
                    ? this.resolveAppendSequence(taskList, insertTask.getMachineCode(), insertTask.getShiftOrder())
                    : anchorSequence + 1);
            // 锚点之后所有同机台同班次任务 sequence +1
            this.shiftSequenceAfter(taskList, insertTask.getMachineCode(),
                    insertTask.getShiftOrder(), insertTask.getSequence());
        } else {
            insertTask.setSequence(command.getTargetSequence() == null
                    ? this.resolveAppendSequence(taskList, insertTask.getMachineCode(), insertTask.getShiftOrder())
                    : command.getTargetSequence());
            this.shiftSequenceAfter(taskList, insertTask.getMachineCode(),
                    insertTask.getShiftOrder(), insertTask.getSequence());
        }
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
     * 解析锚点任务的 sequence（同机台同班次内查找）。
     */
    private Integer resolveAnchorSequence(List<TqManualTaskDraft> taskList, String anchorTaskId,
                                          String machineCode, Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .filter(task -> Objects.equals(anchorTaskId, task.getTaskId()))
                .map(TqManualTaskDraft::getSequence)
                .findFirst().orElse(null);
    }

    /**
     * 获取追加链尾的下一个 sequence。
     */
    private Integer resolveAppendSequence(List<TqManualTaskDraft> taskList, String machineCode, Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .map(TqManualTaskDraft::getSequence).filter(Objects::nonNull)
                .max(Integer::compareTo).map(seq -> seq + 1).orElse(1);
    }

    /**
     * 将同机台同班次内 sequence >= startSequence 的任务顺位后移。
     */
    private void shiftSequenceAfter(List<TqManualTaskDraft> taskList, String machineCode,
                                    Integer shiftOrder, Integer startSequence) {
        taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .filter(task -> task.getSequence() != null && task.getSequence() >= startSequence)
                .forEach(task -> task.setSequence(task.getSequence() + 1));
    }

    /**
     * 应用删除命令。
     */
    private BigDecimal applyDelete(List<TqManualTaskDraft> taskList, TqManualRollingCommand command,
                                   Map<String, TqManualRollingScope> scopeMap,
                                   Set<String> affectedResultGroupKeySet) {
        List<TqManualTaskDraft> deleteTaskList = taskList.stream()
                .filter(task -> Objects.equals(command.getResultGroupKey(), task.getResultGroupKey()))
                .collect(Collectors.toList());
        if (deleteTaskList.isEmpty()) {
            throw new IllegalArgumentException("待删除任务不存在:" + command.getResultGroupKey());
        }
        BigDecimal deleteQty = BigDecimal.ZERO;
        for (TqManualTaskDraft deleteTask : deleteTaskList) {
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
     * 应用调量命令。
     */
    private BigDecimal applyChangeQty(List<TqManualTaskDraft> taskList, TqManualRollingCommand command,
                                      Map<String, TqManualRollingScope> scopeMap,
                                      Set<String> affectedResultGroupKeySet) {
        TqManualTaskDraft targetTask = this.findTask(taskList, command);
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
     * 应用转机台命令（完成量留在来源机台，仅转移未完成部分；支持锚点）。
     */
    private BigDecimal applyChangeMachine(List<TqManualTaskDraft> taskList, TqManualRollingCommand command,
                                          Map<String, TqManualRollingScope> scopeMap,
                                          Set<String> affectedResultGroupKeySet) {
        TqManualTaskDraft sourceTask = this.findTask(taskList, command);
        this.registerScope(scopeMap, sourceTask.getMachineCode(), sourceTask.getShiftOrder(), sourceTask.getSequence());
        BigDecimal finishQty = this.nvl(sourceTask.getFinishQty());
        BigDecimal moveQty = this.nvl(sourceTask.getPlanQty()).subtract(finishQty);
        if (moveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("任务没有可转移的未完成计划量:" + sourceTask.getTaskId());
        }

        TqManualTaskDraft moveTask = sourceTask.copy();
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
        // 锚点优先
        if (StrUtil.isNotBlank(command.getAnchorTaskId())) {
            Integer anchorSequence = this.resolveAnchorSequence(taskList, command.getAnchorTaskId(),
                    command.getTargetMachineCode(), command.getTargetShiftOrder());
            moveTask.setSequence(anchorSequence == null
                    ? this.resolveAppendSequence(taskList, command.getTargetMachineCode(), command.getTargetShiftOrder())
                    : anchorSequence + 1);
        } else {
            Integer targetSequence = command.getTargetSequence();
            if (targetSequence == null) {
                targetSequence = this.resolveAppendSequence(taskList, command.getTargetMachineCode(),
                        command.getTargetShiftOrder());
            }
            moveTask.setSequence(targetSequence);
        }
        this.shiftSequenceAfter(taskList, moveTask.getMachineCode(), moveTask.getShiftOrder(), moveTask.getSequence());
        this.markOperationPriority(moveTask, command.getCommandOrder());
        this.validateTaskLocation(moveTask);

        // 合并同规格同机台同班次任务
        TqManualTaskDraft mergeTarget = taskList.stream()
                .filter(task -> Objects.equals(moveTask.getMachineCode(), task.getMachineCode()))
                .filter(task -> Objects.equals(moveTask.getShiftOrder(), task.getShiftOrder()))
                .filter(task -> Objects.equals(moveTask.getBeadCode(), task.getBeadCode()))
                .findFirst().orElse(null);
        if (mergeTarget == null) {
            taskList.add(moveTask);
            affectedResultGroupKeySet.add(moveTask.getResultGroupKey());
        } else {
            mergeTarget.setPlanQty(this.nvl(mergeTarget.getPlanQty()).add(moveQty));
            this.markOperationPriority(mergeTarget, command.getCommandOrder());
            affectedResultGroupKeySet.add(mergeTarget.getResultGroupKey());
        }
        affectedResultGroupKeySet.add(sourceTask.getResultGroupKey());
        Integer targetScopeSequence = mergeTarget == null ? moveTask.getSequence() : mergeTarget.getSequence();
        this.registerScope(scopeMap, command.getTargetMachineCode(), command.getTargetShiftOrder(), targetScopeSequence);
        return BigDecimal.ZERO;
    }

    /**
     * 单机台重装箱：从影响点开始，按机台定额重新分配任务到后续班次。
     */
    private List<TqManualTaskDraft> repackMachine(List<TqManualTaskDraft> allTaskList,
                                                  TqManualRollingScope scope,
                                                  TqManualRollingContext context,
                                                  List<TqManualTaskDraft> unplannedTaskList) {
        BigDecimal machineCapacity = context.getMachineCapacityMap().get(scope.getMachineCode());
        if (machineCapacity == null || machineCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("机台定额未维护:" + scope.getMachineCode());
        }
        int startShiftOrder = scope.getStartShiftOrder() == null ? 1 : scope.getStartShiftOrder();
        int startSequence = scope.getStartSequence() == null ? 1 : scope.getStartSequence();
        // 拆分：未受影响任务、影响点之前的任务（固定）、已完成量任务（固定）、影响点之后的任务（参与重排）
        List<TqManualTaskDraft> unaffectedTaskList = allTaskList.stream()
                .filter(task -> !Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .map(TqManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        List<TqManualTaskDraft> prefixTaskList = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() < startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) < startSequence))
                .map(TqManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        List<TqManualTaskDraft> lockedFinishTaskList = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) >= startSequence))
                .filter(task -> this.nvl(task.getFinishQty()).compareTo(BigDecimal.ZERO) > 0)
                .map(this::buildFinishedLockedTask)
                .collect(Collectors.toCollection(ArrayList::new));
        List<TqManualTaskDraft> rollingQueue = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) >= startSequence))
                .map(TqManualTaskDraft::copy)
                .map(this::buildUnfinishedTask)
                .filter(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(this.taskOrderComparator())
                .collect(Collectors.toCollection(ArrayList::new));

        List<TqManualTaskDraft> repackedTaskList = new ArrayList<>(unaffectedTaskList);
        repackedTaskList.addAll(prefixTaskList);
        repackedTaskList.addAll(lockedFinishTaskList);
        int fragmentIndex = 1;
        // 逐班次重装箱
        for (int shiftOrder = startShiftOrder; shiftOrder <= TQ_MAX_SHIFT_ORDER; shiftOrder++) {
            final int currentShiftOrder = shiftOrder;
            BigDecimal shiftCapacity = this.resolveShiftCapacity(context, scope.getMachineCode(),
                    currentShiftOrder, machineCapacity);
            List<TqManualTaskDraft> currentShiftTaskList = repackedTaskList.stream()
                    .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                    .filter(task -> Objects.equals(currentShiftOrder, task.getShiftOrder()))
                    .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                    .collect(Collectors.toList());
            BigDecimal usedCapacity = currentShiftTaskList.stream()
                    .map(TqManualTaskDraft::getPlanQty).map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            TqManualTaskDraft predecessorTask = this.findPredecessorTask(
                    repackedTaskList, context, scope.getMachineCode(), currentShiftOrder);
            BigDecimal existingSwitchCapacityDeduct = this.calculateSwitchCapacityDeduct(
                    currentShiftTaskList, predecessorTask, context);
            int nextSequence = currentShiftTaskList.size() + 1;
            while (!rollingQueue.isEmpty()) {
                TqManualTaskDraft currentTask = rollingQueue.get(0);
                if (currentTask.getMinimumShiftOrder() != null && shiftOrder < currentTask.getMinimumShiftOrder()) {
                    break;
                }
                BigDecimal currentPlanQty = this.nvl(currentTask.getPlanQty());
                currentTask.setMachineSpeed(context.getMachineSpecSpeedMap().getOrDefault(
                        scope.getMachineCode() + "|" + currentTask.getBeadCode(), currentTask.getMachineSpeed()));
                TqManualTaskDraft mergeTarget = this.findSameGroupTask(repackedTaskList,
                        scope.getMachineCode(), shiftOrder, currentTask.getResultGroupKey());
                TqManualTaskDraft previousTask = this.findLastTask(currentShiftTaskList);
                if (previousTask == null) {
                    previousTask = predecessorTask;
                }
                BigDecimal currentSwitchCapacityDeduct = mergeTarget == null
                        ? this.calculateTransitionCapacityDeduct(previousTask, currentTask, context)
                        : BigDecimal.ZERO;
                BigDecimal maintenanceCapacityDeduct = this.resolveMaintenanceCapacityDeduct(
                        context, scope.getMachineCode(), currentShiftOrder, currentTask.getMachineSpeed());
                BigDecimal availablePlanCapacity = this.constraintCalculator.calculateRemainCapacity(
                        shiftCapacity.subtract(maintenanceCapacityDeduct).max(BigDecimal.ZERO),
                        usedCapacity, existingSwitchCapacityDeduct.add(currentSwitchCapacityDeduct));
                if (availablePlanCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                rollingQueue.remove(0);
                BigDecimal assignedQty = currentPlanQty.min(availablePlanCapacity);
                if (this.nvl(currentTask.getFinishQty()).compareTo(assignedQty) > 0) {
                    assignedQty = this.nvl(currentTask.getFinishQty());
                }
                if (assignedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (mergeTarget == null) {
                    TqManualTaskDraft assignedTask = currentTask.copy();
                    assignedTask.setShiftOrder(shiftOrder);
                    assignedTask.setSequence(nextSequence++);
                    assignedTask.setPlanQty(assignedQty);
                    assignedTask.setFinishQty(this.nvl(currentTask.getFinishQty()).min(assignedQty));
                    assignedTask.setOperationPriority(false);
                    repackedTaskList.add(assignedTask);
                    currentShiftTaskList.add(assignedTask);
                } else {
                    mergeTarget.setPlanQty(this.nvl(mergeTarget.getPlanQty()).add(assignedQty));
                    mergeTarget.setFinishQty(this.nvl(mergeTarget.getFinishQty())
                            .add(this.nvl(currentTask.getFinishQty()).min(assignedQty)));
                }
                usedCapacity = usedCapacity.add(assignedQty);
                existingSwitchCapacityDeduct = existingSwitchCapacityDeduct.add(currentSwitchCapacityDeduct);
                BigDecimal overflowQty = currentPlanQty.subtract(assignedQty);
                if (overflowQty.compareTo(BigDecimal.ZERO) > 0) {
                    TqManualTaskDraft carryTask = currentTask.copy();
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
        // 剩余任务转入未排
        for (TqManualTaskDraft remainTask : rollingQueue) {
            if (this.nvl(remainTask.getPlanQty()).compareTo(BigDecimal.ZERO) > 0) {
                unplannedTaskList.add(remainTask.copy());
            }
        }
        return repackedTaskList;
    }

    /**
     * 构建最终任务链并 resequence 重排顺位（关键：保证 sequence 连续）。
     *
     * <p>胎圈按"机台+日期+班次"分链，链内通过 sequence 排序后从1连续编号。</p>
     */
    private MachineShiftTaskChain<TqManualTaskDraft> buildTaskChains(List<TqManualTaskDraft> taskList,
                                                                     TqManualRollingContext context) {
        MachineShiftTaskChain<TqManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        List<TqManualTaskDraft> sortedTaskList = taskList.stream()
                .filter(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TqManualTaskDraft::getMachineCode)
                        .thenComparing(TqManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence()))
                        .thenComparing(task -> StrUtil.blankToDefault(task.getTaskId(), "")))
                .collect(Collectors.toList());
        // 按 machineCode+shiftOrder 分组重排 sequence
        Map<String, Integer> shiftSequenceCounter = new HashMap<>();
        for (TqManualTaskDraft task : sortedTaskList) {
            String chainKey = task.getMachineCode() + "|" + scheduleDate + "|" + task.getShiftOrder();
            int seq = shiftSequenceCounter.getOrDefault(chainKey, 0) + 1;
            shiftSequenceCounter.put(chainKey, seq);
            task.setSequence(seq);
            ScheduleTaskLinkedList<TqManualTaskDraft> chain = taskChainGroup.getOrCreate(
                    task.getMachineCode(), scheduleDate, task.getShiftOrder());
            ScheduleTaskNode<TqManualTaskDraft> node = new ScheduleTaskNode<>(task.getTaskId(), task,
                    task.getMachineCode(), scheduleDate, "CLASS" + task.getShiftOrder(),
                    task.getShiftOrder(), task.getPlanQty());
            chain.append(node, new ScheduleOperationContext(context.getOperator(),
                    "TQ_MANUAL_ROLLING", context.getTraceId()));
        }
        return taskChainGroup;
    }

    // ==================== 校验方法 ====================

    private void validateInput(TqManualRollingCommandBatch commandBatch, TqManualRollingContext context) {
        if (commandBatch == null || commandBatch.getCommandList() == null
                || commandBatch.getCommandList().isEmpty()) {
            throw new IllegalArgumentException("人工滚动命令不能为空");
        }
        if (context == null || context.getScheduleDate() == null) {
            throw new IllegalArgumentException("人工滚动上下文及排程日期不能为空");
        }
        if (context.getTaskList() == null || context.getMachineCapacityMap() == null) {
            throw new IllegalArgumentException("人工滚动任务或机台定额不能为空");
        }
    }

    private void validateTaskLocation(TqManualTaskDraft task) {
        if (task == null || StrUtil.isBlank(task.getTaskId()) || StrUtil.isBlank(task.getResultGroupKey())
                || StrUtil.isBlank(task.getMachineCode()) || task.getShiftOrder() == null
                || task.getShiftOrder() < 1 || task.getShiftOrder() > TQ_MAX_SHIFT_ORDER
                || task.getSequence() == null || task.getSequence() < 1) {
            throw new IllegalStateException("人工滚动任务定位字段非法");
        }
    }

    private void validateResult(List<TqManualTaskDraft> taskList, List<TqManualTaskDraft> unplannedTaskList,
                                MachineShiftTaskChain<TqManualTaskDraft> taskChainGroup,
                                TqManualRollingContext context, BigDecimal beforeTotalQty,
                                BigDecimal commandDeltaQty, Set<String> initialResultGroupKeySet,
                                Set<String> affectedResultGroupKeySet) {
        // 校验顺序连续性、数量守恒、范围不越界
        Set<String> slotKeySet = new LinkedHashSet<>();
        for (TqManualTaskDraft task : taskList) {
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
        // 顺序连续性校验
        Map<String, List<TqManualTaskDraft>> machineShiftMap = taskList.stream()
                .collect(Collectors.groupingBy(task -> task.getMachineCode() + "|" + task.getShiftOrder(),
                        LinkedHashMap::new, Collectors.toList()));
        boolean resultGroupCrossMachine = taskList.stream()
                .collect(Collectors.groupingBy(TqManualTaskDraft::getResultGroupKey,
                        Collectors.mapping(TqManualTaskDraft::getMachineCode, Collectors.toSet())))
                .values().stream().anyMatch(machineCodeSet -> machineCodeSet.size() > 1);
        if (resultGroupCrossMachine) {
            throw new IllegalStateException("同一结果分组不允许跨机台装配");
        }
        for (Map.Entry<String, List<TqManualTaskDraft>> entry : machineShiftMap.entrySet()) {
            List<TqManualTaskDraft> shiftTaskList = entry.getValue().stream()
                    .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                    .collect(Collectors.toList());
            for (int index = 0; index < shiftTaskList.size(); index++) {
                if (!Objects.equals(index + 1, shiftTaskList.get(index).getSequence())) {
                    throw new IllegalStateException("机台班次顺序不连续:" + entry.getKey());
                }
            }
        }
        // 数量守恒
        BigDecimal actualTotalQty = this.sumPlanQty(taskList).add(this.sumPlanQty(unplannedTaskList));
        BigDecimal expectedTotalQty = beforeTotalQty.add(commandDeltaQty);
        if (expectedTotalQty.compareTo(actualTotalQty) != 0) {
            throw new IllegalStateException("人工滚动数量不守恒, expected=" + expectedTotalQty
                    + ", actual=" + actualTotalQty);
        }
    }

    // ==================== 辅助方法 ====================

    private TqManualTaskDraft findTask(List<TqManualTaskDraft> taskList, TqManualRollingCommand command) {
        List<TqManualTaskDraft> candidateList = taskList.stream()
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

    private TqManualTaskDraft findSameGroupTask(List<TqManualTaskDraft> taskList, String machineCode,
                                                int shiftOrder, String resultGroupKey) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .filter(task -> Objects.equals(resultGroupKey, task.getResultGroupKey()))
                .findFirst().orElse(null);
    }

    private TqManualTaskDraft findPredecessorTask(List<TqManualTaskDraft> taskList,
                                                  TqManualRollingContext context,
                                                  String machineCode, Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> task.getShiftOrder() != null && task.getShiftOrder() < shiftOrder)
                .max(Comparator.comparing(TqManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence())))
                .orElse(context.getPredecessorTaskMap().get(machineCode));
    }

    private TqManualTaskDraft findLastTask(List<TqManualTaskDraft> taskList) {
        return taskList == null || taskList.isEmpty() ? null : taskList.get(taskList.size() - 1);
    }

    private TqManualTaskDraft buildFinishedLockedTask(TqManualTaskDraft sourceTask) {
        TqManualTaskDraft lockedTask = sourceTask.copy();
        lockedTask.setPlanQty(this.nvl(sourceTask.getFinishQty()));
        return lockedTask;
    }

    private TqManualTaskDraft buildUnfinishedTask(TqManualTaskDraft sourceTask) {
        BigDecimal unfinishedQty = this.nvl(sourceTask.getPlanQty())
                .subtract(this.nvl(sourceTask.getFinishQty())).max(BigDecimal.ZERO);
        sourceTask.setTaskId(sourceTask.getTaskId() + ":UNFINISHED");
        sourceTask.setPlanQty(unfinishedQty);
        sourceTask.setFinishQty(BigDecimal.ZERO);
        return sourceTask;
    }

    private BigDecimal resolveShiftCapacity(TqManualRollingContext context, String machineCode,
                                            Integer shiftOrder, BigDecimal machineCapacity) {
        BigDecimal shiftCapacity = context.getShiftCapacityMap().get(machineCode + "|" + shiftOrder);
        return shiftCapacity == null ? machineCapacity : shiftCapacity;
    }

    private BigDecimal resolveMaintenanceCapacityDeduct(TqManualRollingContext context, String machineCode,
                                                        Integer shiftOrder, BigDecimal machineSpeed) {
        BigDecimal maintenanceHours = context.getMaintenanceHoursMap().get(machineCode + "|" + shiftOrder);
        if (maintenanceHours == null || machineSpeed == null) {
            return BigDecimal.ZERO;
        }
        return maintenanceHours.max(BigDecimal.ZERO).multiply(machineSpeed.max(BigDecimal.ZERO));
    }

    private BigDecimal calculateSwitchCapacityDeduct(List<TqManualTaskDraft> taskList,
                                                     TqManualTaskDraft predecessorTask,
                                                     TqManualRollingContext context) {
        BigDecimal totalCapacityDeduct = BigDecimal.ZERO;
        TqManualTaskDraft previousTask = predecessorTask;
        for (TqManualTaskDraft currentTask : taskList) {
            totalCapacityDeduct = totalCapacityDeduct.add(
                    this.calculateTransitionCapacityDeduct(previousTask, currentTask, context));
            previousTask = currentTask;
        }
        return totalCapacityDeduct;
    }

    /**
     * 胎圈规格切换产能扣减：按 beadCode 判断是否切换。
     */
    private BigDecimal calculateTransitionCapacityDeduct(TqManualTaskDraft previousTask,
                                                         TqManualTaskDraft currentTask,
                                                         TqManualRollingContext context) {
        String previousBeadCode = previousTask == null ? null : previousTask.getBeadCode();
        if (currentTask == null || previousBeadCode == null
                || Objects.equals(previousBeadCode, currentTask.getBeadCode())) {
            return BigDecimal.ZERO;
        }
        // 切换产能扣减 = 切换时长(小时) × 机台速度
        // 胎圈默认切换时长 0.5 小时（与 TqRollingUpdateServiceImpl.calculateSwitchTime 一致）
        BigDecimal switchHours = BigDecimal.valueOf(0.5);
        BigDecimal machineSpeed = currentTask.getMachineSpeed() == null ? BigDecimal.ZERO : currentTask.getMachineSpeed();
        return switchHours.multiply(machineSpeed).setScale(4, RoundingMode.HALF_UP);
    }

    private void registerScope(Map<String, TqManualRollingScope> scopeMap, String machineCode,
                               Integer shiftOrder, Integer sequence) {
        if (StrUtil.isBlank(machineCode)) {
            throw new IllegalArgumentException("人工滚动机台不能为空");
        }
        TqManualRollingScope scope = scopeMap.computeIfAbsent(machineCode, key -> {
            TqManualRollingScope target = new TqManualRollingScope();
            target.setMachineCode(key);
            return target;
        });
        scope.merge(shiftOrder, sequence);
    }

    private void markOperationPriority(TqManualTaskDraft task, Integer commandOrder) {
        task.setOperationPriority(true);
        task.setOperationOrder(commandOrder == null ? 0 : commandOrder);
    }

    private Comparator<TqManualTaskDraft> taskOrderComparator() {
        return Comparator.comparing(TqManualTaskDraft::getShiftOrder)
                .thenComparing(task -> this.defaultSequence(task.getSequence()))
                .thenComparing(task -> task.isOperationPriority() ? 0 : 1)
                .thenComparing(task -> task.getOperationOrder() == null ? Integer.MAX_VALUE : task.getOperationOrder())
                .thenComparing(task -> StrUtil.blankToDefault(task.getTaskId(), ""));
    }

    private BigDecimal sumPlanQty(List<TqManualTaskDraft> taskList) {
        if (taskList == null) {
            return BigDecimal.ZERO;
        }
        return taskList.stream().filter(Objects::nonNull).map(TqManualTaskDraft::getPlanQty)
                .map(this::nvl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultSequence(Integer sequence) {
        return sequence == null ? Integer.MAX_VALUE : sequence;
    }

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

    private LocalDate toLocalDate(Date scheduleDate) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("排程日期不能为空");
        }
        return Instant.ofEpochMilli(scheduleDate.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
