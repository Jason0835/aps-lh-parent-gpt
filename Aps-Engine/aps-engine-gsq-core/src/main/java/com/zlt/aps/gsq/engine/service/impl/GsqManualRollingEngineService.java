package com.zlt.aps.gsq.engine.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintCalculator;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingCommand;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingCommandBatch;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingContext;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingOperationEnum;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingResult;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingScope;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualTaskDraft;
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
 * 钢丝圈人工操作纯滚动引擎。
 *
 * <p>统一完成插单、删除、调量、转机台的纯计算。数据库锁、事务、审计和实体持久化
 * 由 aps-gsq 业务层负责。与胎面差异：无工装账本、规格切换按 steelRingCode、repackMachine
 * 简化为按机台定额重装箱。</p>
 */
@Service
public class GsqManualRollingEngineService {

    /** 新建结果分组前缀 */
    private static final String NEW_GROUP_PREFIX = "MANUAL:";

    /** 转机台拆分结果分组前缀 */
    private static final String MOVE_GROUP_PREFIX = "MOVE:";

    /** 钢丝圈最大班次数（6班次跨3天） */
    private static final int GSQ_MAX_SHIFT_ORDER = 6;

    /** 规格切换约束纯计算器 */
    private final ScheduleConstraintCalculator constraintCalculator = new ScheduleConstraintCalculator();

    /**
     * 执行一批人工滚动命令。
     *
     * @param commandBatch 批量命令
     * @param context      运行态上下文
     * @return 完整滚动结果
     */
    public GsqManualRollingResult execute(GsqManualRollingCommandBatch commandBatch,
                                          GsqManualRollingContext context) {
        this.validateInput(commandBatch, context);
        // 1. 深拷贝任务列表，避免污染上下文
        List<GsqManualTaskDraft> taskList = context.getTaskList().stream()
                .filter(Objects::nonNull)
                .map(GsqManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> initialResultGroupKeySet = taskList.stream()
                .map(GsqManualTaskDraft::getResultGroupKey).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        GsqManualRollingResult rollingResult = new GsqManualRollingResult();
        rollingResult.setBeforeTotalQty(this.sumPlanQty(taskList));
        List<GsqManualTaskDraft> unplannedTaskList = new ArrayList<>();

        // 2. 遍历命令批次，分派到 applyInsert/applyDelete/applyChangeMachine/applyChangeQty
        Map<String, GsqManualRollingScope> scopeMap = new LinkedHashMap<>();
        BigDecimal commandDeltaQty = BigDecimal.ZERO;
        List<GsqManualRollingCommand> commandList = commandBatch.getCommandList();
        for (int commandIndex = 0; commandIndex < commandList.size(); commandIndex++) {
            GsqManualRollingCommand command = commandList.get(commandIndex);
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
        List<GsqManualRollingScope> scopeList = scopeMap.values().stream()
                .sorted(Comparator.comparing(GsqManualRollingScope::getMachineCode))
                .collect(Collectors.toList());
        rollingResult.setChainChangeSummaryList(scopeList.stream()
                .map(scope -> scope.getMachineCode() + ":CLASS" + scope.getStartShiftOrder()
                        + ":SEQ" + scope.getStartSequence())
                .collect(Collectors.toList()));
        for (GsqManualRollingScope scope : scopeList) {
            taskList = this.repackMachine(taskList, scope, context, unplannedTaskList);
        }

        // 5. 构建任务链并 resequence 重排顺位
        MachineShiftTaskChain<GsqManualTaskDraft> taskChainGroup = this.buildTaskChains(taskList, context);

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
        Set<Long> scheduledSourceIdSet = taskList.stream().map(GsqManualTaskDraft::getSourceResultId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> explicitDeleteResultIdSet = commandList.stream()
                .filter(cmd -> cmd != null && GsqManualRollingOperationEnum.DELETE == cmd.getOperationType())
                .map(GsqManualRollingCommand::getResultGroupKey).map(this::parseResultId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        commandList.stream()
                .filter(cmd -> cmd != null && GsqManualRollingOperationEnum.CHANGE_MACHINE == cmd.getOperationType())
                .map(GsqManualRollingCommand::getResultGroupKey).map(this::parseResultId)
                .filter(Objects::nonNull).filter(resultId -> !scheduledSourceIdSet.contains(resultId))
                .forEach(explicitDeleteResultIdSet::add);
        rollingResult.setExplicitDeleteResultIdSet(explicitDeleteResultIdSet);
        rollingResult.setContainsNonDeleteOperation(commandList.stream()
                .filter(Objects::nonNull)
                .anyMatch(cmd -> GsqManualRollingOperationEnum.DELETE != cmd.getOperationType()));
        Set<String> scheduledGroupKeySet = taskList.stream().map(GsqManualTaskDraft::getResultGroupKey)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        rollingResult.setMoveToUnplannedResultIdSet(unplannedTaskList.stream()
                .filter(task -> !scheduledGroupKeySet.contains(task.getResultGroupKey()))
                .map(GsqManualTaskDraft::getSourceResultId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        context.setTaskList(taskList);
        context.setTaskChainGroup(taskChainGroup);
        return rollingResult;
    }

    /**
     * 分派单条命令到对应处理器。
     */
    private BigDecimal applyCommand(List<GsqManualTaskDraft> taskList, GsqManualRollingCommand command,
                                    Map<String, GsqManualRollingScope> scopeMap,
                                    Set<String> affectedResultGroupKeySet) {
        GsqManualRollingOperationEnum operationType = command.getOperationType();
        if (GsqManualRollingOperationEnum.INSERT == operationType) {
            return this.applyInsert(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        if (GsqManualRollingOperationEnum.DELETE == operationType) {
            return this.applyDelete(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        if (GsqManualRollingOperationEnum.CHANGE_MACHINE == operationType) {
            return this.applyChangeMachine(taskList, command, scopeMap, affectedResultGroupKeySet);
        }
        return this.applyChangeQty(taskList, command, scopeMap, affectedResultGroupKeySet);
    }

    /**
     * 应用人工插单命令（支持锚点插入）。
     */
    private BigDecimal applyInsert(List<GsqManualTaskDraft> taskList, GsqManualRollingCommand command,
                                   Map<String, GsqManualRollingScope> scopeMap,
                                   Set<String> affectedResultGroupKeySet) {
        if (command.getInsertTask() == null) {
            throw new IllegalArgumentException("人工插单任务不能为空");
        }
        GsqManualTaskDraft insertTask = command.getInsertTask().copy();
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
    private Integer resolveAnchorSequence(List<GsqManualTaskDraft> taskList, String anchorTaskId,
                                          String machineCode, Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .filter(task -> Objects.equals(anchorTaskId, task.getTaskId()))
                .map(GsqManualTaskDraft::getSequence)
                .findFirst().orElse(null);
    }

    /**
     * 获取追加链尾的下一个 sequence。
     */
    private Integer resolveAppendSequence(List<GsqManualTaskDraft> taskList, String machineCode, Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .map(GsqManualTaskDraft::getSequence).filter(Objects::nonNull)
                .max(Integer::compareTo).map(seq -> seq + 1).orElse(1);
    }

    /**
     * 将同机台同班次内 sequence >= startSequence 的任务顺位后移。
     */
    private void shiftSequenceAfter(List<GsqManualTaskDraft> taskList, String machineCode,
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
    private BigDecimal applyDelete(List<GsqManualTaskDraft> taskList, GsqManualRollingCommand command,
                                   Map<String, GsqManualRollingScope> scopeMap,
                                   Set<String> affectedResultGroupKeySet) {
        List<GsqManualTaskDraft> deleteTaskList = taskList.stream()
                .filter(task -> Objects.equals(command.getResultGroupKey(), task.getResultGroupKey()))
                .collect(Collectors.toList());
        if (deleteTaskList.isEmpty()) {
            throw new IllegalArgumentException("待删除任务不存在:" + command.getResultGroupKey());
        }
        BigDecimal deleteQty = BigDecimal.ZERO;
        for (GsqManualTaskDraft deleteTask : deleteTaskList) {
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
    private BigDecimal applyChangeQty(List<GsqManualTaskDraft> taskList, GsqManualRollingCommand command,
                                      Map<String, GsqManualRollingScope> scopeMap,
                                      Set<String> affectedResultGroupKeySet) {
        GsqManualTaskDraft targetTask = this.findTask(taskList, command);
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
    private BigDecimal applyChangeMachine(List<GsqManualTaskDraft> taskList, GsqManualRollingCommand command,
                                          Map<String, GsqManualRollingScope> scopeMap,
                                          Set<String> affectedResultGroupKeySet) {
        GsqManualTaskDraft sourceTask = this.findTask(taskList, command);
        this.registerScope(scopeMap, sourceTask.getMachineCode(), sourceTask.getShiftOrder(), sourceTask.getSequence());
        BigDecimal finishQty = this.nvl(sourceTask.getFinishQty());
        BigDecimal moveQty = this.nvl(sourceTask.getPlanQty()).subtract(finishQty);
        if (moveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("任务没有可转移的未完成计划量:" + sourceTask.getTaskId());
        }

        GsqManualTaskDraft moveTask = sourceTask.copy();
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
        GsqManualTaskDraft mergeTarget = taskList.stream()
                .filter(task -> Objects.equals(moveTask.getMachineCode(), task.getMachineCode()))
                .filter(task -> Objects.equals(moveTask.getShiftOrder(), task.getShiftOrder()))
                .filter(task -> Objects.equals(moveTask.getSteelRingCode(), task.getSteelRingCode()))
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
    private List<GsqManualTaskDraft> repackMachine(List<GsqManualTaskDraft> allTaskList,
                                                   GsqManualRollingScope scope,
                                                   GsqManualRollingContext context,
                                                   List<GsqManualTaskDraft> unplannedTaskList) {
        BigDecimal machineCapacity = context.getMachineCapacityMap().get(scope.getMachineCode());
        if (machineCapacity == null || machineCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("机台定额未维护:" + scope.getMachineCode());
        }
        int startShiftOrder = scope.getStartShiftOrder() == null ? 1 : scope.getStartShiftOrder();
        int startSequence = scope.getStartSequence() == null ? 1 : scope.getStartSequence();
        // 拆分：未受影响任务、影响点之前的任务（固定）、已完成量任务（固定）、影响点之后的任务（参与重排）
        List<GsqManualTaskDraft> unaffectedTaskList = allTaskList.stream()
                .filter(task -> !Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .map(GsqManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        List<GsqManualTaskDraft> prefixTaskList = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() < startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) < startSequence))
                .map(GsqManualTaskDraft::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        List<GsqManualTaskDraft> lockedFinishTaskList = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) >= startSequence))
                .filter(task -> this.nvl(task.getFinishQty()).compareTo(BigDecimal.ZERO) > 0)
                .map(this::buildFinishedLockedTask)
                .collect(Collectors.toCollection(ArrayList::new));
        List<GsqManualTaskDraft> rollingQueue = allTaskList.stream()
                .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                .filter(task -> task.getShiftOrder() > startShiftOrder
                        || (task.getShiftOrder() == startShiftOrder
                        && this.defaultSequence(task.getSequence()) >= startSequence))
                .map(GsqManualTaskDraft::copy)
                .map(this::buildUnfinishedTask)
                .filter(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(this.taskOrderComparator())
                .collect(Collectors.toCollection(ArrayList::new));

        List<GsqManualTaskDraft> repackedTaskList = new ArrayList<>(unaffectedTaskList);
        repackedTaskList.addAll(prefixTaskList);
        repackedTaskList.addAll(lockedFinishTaskList);
        int fragmentIndex = 1;
        // 逐班次重装箱
        for (int shiftOrder = startShiftOrder; shiftOrder <= GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            final int currentShiftOrder = shiftOrder;
            BigDecimal shiftCapacity = this.resolveShiftCapacity(context, scope.getMachineCode(),
                    currentShiftOrder, machineCapacity);
            List<GsqManualTaskDraft> currentShiftTaskList = repackedTaskList.stream()
                    .filter(task -> Objects.equals(scope.getMachineCode(), task.getMachineCode()))
                    .filter(task -> Objects.equals(currentShiftOrder, task.getShiftOrder()))
                    .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                    .collect(Collectors.toList());
            BigDecimal usedCapacity = currentShiftTaskList.stream()
                    .map(GsqManualTaskDraft::getPlanQty).map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            GsqManualTaskDraft predecessorTask = this.findPredecessorTask(
                    repackedTaskList, context, scope.getMachineCode(), currentShiftOrder);
            BigDecimal existingSwitchCapacityDeduct = this.calculateSwitchCapacityDeduct(
                    currentShiftTaskList, predecessorTask, context);
            int nextSequence = currentShiftTaskList.size() + 1;
            while (!rollingQueue.isEmpty()) {
                GsqManualTaskDraft currentTask = rollingQueue.get(0);
                if (currentTask.getMinimumShiftOrder() != null && shiftOrder < currentTask.getMinimumShiftOrder()) {
                    break;
                }
                BigDecimal currentPlanQty = this.nvl(currentTask.getPlanQty());
                // 钢丝圈无单独 GsqMachineSpecSpeed 表，机台规格速度由 service 层依据
                // GsqMachineInfo.quata / 8小时 填充至 machineSpecSpeedMap（key=机台编码|钢丝圈编码）
                currentTask.setMachineSpeed(context.getMachineSpecSpeedMap().getOrDefault(
                        scope.getMachineCode() + "|" + currentTask.getSteelRingCode(), currentTask.getMachineSpeed()));
                GsqManualTaskDraft mergeTarget = this.findSameGroupTask(repackedTaskList,
                        scope.getMachineCode(), shiftOrder, currentTask.getResultGroupKey());
                GsqManualTaskDraft previousTask = this.findLastTask(currentShiftTaskList);
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
                    GsqManualTaskDraft assignedTask = currentTask.copy();
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
                    GsqManualTaskDraft carryTask = currentTask.copy();
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
        for (GsqManualTaskDraft remainTask : rollingQueue) {
            if (this.nvl(remainTask.getPlanQty()).compareTo(BigDecimal.ZERO) > 0) {
                unplannedTaskList.add(remainTask.copy());
            }
        }
        return repackedTaskList;
    }

    /**
     * 构建最终任务链并 resequence 重排顺位（关键：保证 sequence 连续）。
     *
     * <p>钢丝圈按"机台+日期+班次"分链，链内通过 sequence 排序后从1连续编号。</p>
     */
    private MachineShiftTaskChain<GsqManualTaskDraft> buildTaskChains(List<GsqManualTaskDraft> taskList,
                                                                      GsqManualRollingContext context) {
        MachineShiftTaskChain<GsqManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        List<GsqManualTaskDraft> sortedTaskList = taskList.stream()
                .filter(task -> this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(GsqManualTaskDraft::getMachineCode)
                        .thenComparing(GsqManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence()))
                        .thenComparing(task -> StrUtil.blankToDefault(task.getTaskId(), "")))
                .collect(Collectors.toList());
        // 按 machineCode+shiftOrder 分组重排 sequence
        Map<String, Integer> shiftSequenceCounter = new HashMap<>();
        for (GsqManualTaskDraft task : sortedTaskList) {
            String chainKey = task.getMachineCode() + "|" + scheduleDate + "|" + task.getShiftOrder();
            int seq = shiftSequenceCounter.getOrDefault(chainKey, 0) + 1;
            shiftSequenceCounter.put(chainKey, seq);
            task.setSequence(seq);
            ScheduleTaskLinkedList<GsqManualTaskDraft> chain = taskChainGroup.getOrCreate(
                    task.getMachineCode(), scheduleDate, task.getShiftOrder());
            ScheduleTaskNode<GsqManualTaskDraft> node = new ScheduleTaskNode<>(task.getTaskId(), task,
                    task.getMachineCode(), scheduleDate, "CLASS" + task.getShiftOrder(),
                    task.getShiftOrder(), task.getPlanQty());
            chain.append(node, new ScheduleOperationContext(context.getOperator(),
                    "GSQ_MANUAL_ROLLING", context.getTraceId()));
        }
        return taskChainGroup;
    }

    // ==================== 校验方法 ====================

    private void validateInput(GsqManualRollingCommandBatch commandBatch, GsqManualRollingContext context) {
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

    private void validateTaskLocation(GsqManualTaskDraft task) {
        if (task == null || StrUtil.isBlank(task.getTaskId()) || StrUtil.isBlank(task.getResultGroupKey())
                || StrUtil.isBlank(task.getMachineCode()) || task.getShiftOrder() == null
                || task.getShiftOrder() < 1 || task.getShiftOrder() > GSQ_MAX_SHIFT_ORDER
                || task.getSequence() == null || task.getSequence() < 1) {
            throw new IllegalStateException("人工滚动任务定位字段非法");
        }
    }

    private void validateResult(List<GsqManualTaskDraft> taskList, List<GsqManualTaskDraft> unplannedTaskList,
                                MachineShiftTaskChain<GsqManualTaskDraft> taskChainGroup,
                                GsqManualRollingContext context, BigDecimal beforeTotalQty,
                                BigDecimal commandDeltaQty, Set<String> initialResultGroupKeySet,
                                Set<String> affectedResultGroupKeySet) {
        // 校验顺序连续性、数量守恒、范围不越界
        Set<String> slotKeySet = new LinkedHashSet<>();
        for (GsqManualTaskDraft task : taskList) {
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
        Map<String, List<GsqManualTaskDraft>> machineShiftMap = taskList.stream()
                .collect(Collectors.groupingBy(task -> task.getMachineCode() + "|" + task.getShiftOrder(),
                        LinkedHashMap::new, Collectors.toList()));
        boolean resultGroupCrossMachine = taskList.stream()
                .collect(Collectors.groupingBy(GsqManualTaskDraft::getResultGroupKey,
                        Collectors.mapping(GsqManualTaskDraft::getMachineCode, Collectors.toSet())))
                .values().stream().anyMatch(machineCodeSet -> machineCodeSet.size() > 1);
        if (resultGroupCrossMachine) {
            throw new IllegalStateException("同一结果分组不允许跨机台装配");
        }
        for (Map.Entry<String, List<GsqManualTaskDraft>> entry : machineShiftMap.entrySet()) {
            List<GsqManualTaskDraft> shiftTaskList = entry.getValue().stream()
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

    private GsqManualTaskDraft findTask(List<GsqManualTaskDraft> taskList, GsqManualRollingCommand command) {
        List<GsqManualTaskDraft> candidateList = taskList.stream()
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

    private GsqManualTaskDraft findSameGroupTask(List<GsqManualTaskDraft> taskList, String machineCode,
                                                 int shiftOrder, String resultGroupKey) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> Objects.equals(shiftOrder, task.getShiftOrder()))
                .filter(task -> Objects.equals(resultGroupKey, task.getResultGroupKey()))
                .findFirst().orElse(null);
    }

    private GsqManualTaskDraft findPredecessorTask(List<GsqManualTaskDraft> taskList,
                                                   GsqManualRollingContext context,
                                                   String machineCode, Integer shiftOrder) {
        return taskList.stream()
                .filter(task -> Objects.equals(machineCode, task.getMachineCode()))
                .filter(task -> task.getShiftOrder() != null && task.getShiftOrder() < shiftOrder)
                .max(Comparator.comparing(GsqManualTaskDraft::getShiftOrder)
                        .thenComparing(task -> this.defaultSequence(task.getSequence())))
                .orElse(context.getPredecessorTaskMap().get(machineCode));
    }

    private GsqManualTaskDraft findLastTask(List<GsqManualTaskDraft> taskList) {
        return taskList == null || taskList.isEmpty() ? null : taskList.get(taskList.size() - 1);
    }

    private GsqManualTaskDraft buildFinishedLockedTask(GsqManualTaskDraft sourceTask) {
        GsqManualTaskDraft lockedTask = sourceTask.copy();
        lockedTask.setPlanQty(this.nvl(sourceTask.getFinishQty()));
        return lockedTask;
    }

    private GsqManualTaskDraft buildUnfinishedTask(GsqManualTaskDraft sourceTask) {
        BigDecimal unfinishedQty = this.nvl(sourceTask.getPlanQty())
                .subtract(this.nvl(sourceTask.getFinishQty())).max(BigDecimal.ZERO);
        sourceTask.setTaskId(sourceTask.getTaskId() + ":UNFINISHED");
        sourceTask.setPlanQty(unfinishedQty);
        sourceTask.setFinishQty(BigDecimal.ZERO);
        return sourceTask;
    }

    private BigDecimal resolveShiftCapacity(GsqManualRollingContext context, String machineCode,
                                            Integer shiftOrder, BigDecimal machineCapacity) {
        BigDecimal shiftCapacity = context.getShiftCapacityMap().get(machineCode + "|" + shiftOrder);
        return shiftCapacity == null ? machineCapacity : shiftCapacity;
    }

    private BigDecimal resolveMaintenanceCapacityDeduct(GsqManualRollingContext context, String machineCode,
                                                       Integer shiftOrder, BigDecimal machineSpeed) {
        BigDecimal maintenanceHours = context.getMaintenanceHoursMap().get(machineCode + "|" + shiftOrder);
        if (maintenanceHours == null || machineSpeed == null) {
            return BigDecimal.ZERO;
        }
        return maintenanceHours.max(BigDecimal.ZERO).multiply(machineSpeed.max(BigDecimal.ZERO));
    }

    private BigDecimal calculateSwitchCapacityDeduct(List<GsqManualTaskDraft> taskList,
                                                    GsqManualTaskDraft predecessorTask,
                                                    GsqManualRollingContext context) {
        BigDecimal totalCapacityDeduct = BigDecimal.ZERO;
        GsqManualTaskDraft previousTask = predecessorTask;
        for (GsqManualTaskDraft currentTask : taskList) {
            totalCapacityDeduct = totalCapacityDeduct.add(
                    this.calculateTransitionCapacityDeduct(previousTask, currentTask, context));
            previousTask = currentTask;
        }
        return totalCapacityDeduct;
    }

    /**
     * 钢丝圈规格切换产能扣减：按 steelRingCode 判断是否切换。
     */
    private BigDecimal calculateTransitionCapacityDeduct(GsqManualTaskDraft previousTask,
                                                        GsqManualTaskDraft currentTask,
                                                        GsqManualRollingContext context) {
        String previousSteelRingCode = previousTask == null ? null : previousTask.getSteelRingCode();
        if (currentTask == null || previousSteelRingCode == null
                || Objects.equals(previousSteelRingCode, currentTask.getSteelRingCode())) {
            return BigDecimal.ZERO;
        }
        // 切换产能扣减 = 切换时长(小时) × 机台速度
        // 钢丝圈默认切换时长 0.5 小时（与 GsqRollingUpdateServiceImpl.calculateSwitchTime 一致）
        BigDecimal switchHours = BigDecimal.valueOf(0.5);
        BigDecimal machineSpeed = currentTask.getMachineSpeed() == null ? BigDecimal.ZERO : currentTask.getMachineSpeed();
        return switchHours.multiply(machineSpeed).setScale(4, RoundingMode.HALF_UP);
    }

    private void registerScope(Map<String, GsqManualRollingScope> scopeMap, String machineCode,
                               Integer shiftOrder, Integer sequence) {
        if (StrUtil.isBlank(machineCode)) {
            throw new IllegalArgumentException("人工滚动机台不能为空");
        }
        GsqManualRollingScope scope = scopeMap.computeIfAbsent(machineCode, key -> {
            GsqManualRollingScope target = new GsqManualRollingScope();
            target.setMachineCode(key);
            return target;
        });
        scope.merge(shiftOrder, sequence);
    }

    private void markOperationPriority(GsqManualTaskDraft task, Integer commandOrder) {
        task.setOperationPriority(true);
        task.setOperationOrder(commandOrder == null ? 0 : commandOrder);
    }

    private Comparator<GsqManualTaskDraft> taskOrderComparator() {
        return Comparator.comparing(GsqManualTaskDraft::getShiftOrder)
                .thenComparing(task -> this.defaultSequence(task.getSequence()))
                .thenComparing(task -> task.isOperationPriority() ? 0 : 1)
                .thenComparing(task -> task.getOperationOrder() == null ? Integer.MAX_VALUE : task.getOperationOrder())
                .thenComparing(task -> StrUtil.blankToDefault(task.getTaskId(), ""));
    }

    private BigDecimal sumPlanQty(List<GsqManualTaskDraft> taskList) {
        if (taskList == null) {
            return BigDecimal.ZERO;
        }
        return taskList.stream().filter(Objects::nonNull).map(GsqManualTaskDraft::getPlanQty)
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
