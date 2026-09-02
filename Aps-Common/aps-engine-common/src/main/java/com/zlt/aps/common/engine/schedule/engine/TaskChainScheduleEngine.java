package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 自动排程任务链公共主流程引擎。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 */
public final class TaskChainScheduleEngine<C extends TaskChainContextAccess<T>,
        T extends ScheduleTaskDraftModel> {

    private final TaskChainSettings settings;

    /**
     * 创建任务链公共引擎。
     *
     * @param settings 领域常量
     */
    public TaskChainScheduleEngine(TaskChainSettings settings) {
        this.settings = settings;
    }

    public ScheduleChainChangeResult<T> appendAutoTask(T task, ScheduleQualityMachineCandidate machine,
                                                        C context, TaskChainPolicy<C, T> policy) {
        this.validateTaskAndContext(task, context, policy);
        this.validateMachine(machine, policy);
        task.setMachineCode(machine.getMachineCode());
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        ScheduleTaskLinkedList<T> chain = context.getTaskChainGroup()
                .getOrCreate(machine.getMachineCode(), this.toLocalDate(context, policy), shiftOrder);
        ScheduleTaskNode<T> node = this.toNode(task, machine.getMachineCode(), shiftOrder, context, policy);
        ScheduleChainChangeResult<T> result = chain.append(node,
                this.operationContext(context, settings.getAutoAppendOperation()));
        this.finishChange(context, chain, node, settings.getAutoAppendOperation(), policy);
        return result;
    }

    public ScheduleChainChangeResult<T> prependAutoTask(T task, ScheduleQualityMachineCandidate machine,
                                                         C context, TaskChainPolicy<C, T> policy) {
        this.validateTaskAndContext(task, context, policy);
        this.validateMachine(machine, policy);
        task.setMachineCode(machine.getMachineCode());
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        ScheduleTaskLinkedList<T> chain = context.getTaskChainGroup()
                .getOrCreate(machine.getMachineCode(), this.toLocalDate(context, policy), shiftOrder);
        ScheduleTaskNode<T> node = this.toNode(task, machine.getMachineCode(), shiftOrder, context, policy);
        ScheduleChainChangeResult<T> result = chain.prepend(node,
                this.operationContext(context, settings.getAutoPrependOperation()));
        this.finishChange(context, chain, node, settings.getAutoPrependOperation(), policy);
        return result;
    }

    public ScheduleChainChangeResult<T> insertManualTask(T task, TaskChainInsertPosition position,
                                                          C context, TaskChainPolicy<C, T> policy) {
        this.validateTaskAndContext(task, context, policy);
        if (position == null || this.isBlank(position.getMachineCode()) || position.getShiftOrder() == null) {
            throw policy.error(TaskChainErrorType.PARAM_EMPTY, null);
        }
        task.setMachineCode(position.getMachineCode());
        ScheduleTaskLinkedList<T> chain = context.getTaskChainGroup().getOrCreate(position.getMachineCode(),
                this.toLocalDate(context, policy), position.getShiftOrder());
        ScheduleTaskNode<T> anchor = this.findNode(position.getAnchorTaskId(), context);
        if (anchor == null || anchor.getOwnerList() != chain) {
            anchor = chain.findByTaskId(position.getAnchorTaskId());
        }
        ScheduleTaskNode<T> node = this.toNode(task, position.getMachineCode(), position.getShiftOrder(),
                context, policy);
        ScheduleChainChangeResult<T> result = chain.insertAfter(anchor, node,
                this.operationContext(context, settings.getManualInsertOperation()));
        this.finishChange(context, chain, node, settings.getManualInsertOperation(), policy);
        return result;
    }

    public ScheduleChainChangeResult<T> removeTask(String taskId, C context, TaskChainPolicy<C, T> policy) {
        this.validateTaskId(taskId, policy);
        this.validateContextChain(context, policy);
        ScheduleTaskNode<T> node = this.findNode(taskId, context);
        ScheduleTaskLinkedList<T> chain = this.ownerChain(node);
        if (node == null || chain == null) {
            throw policy.error(TaskChainErrorType.TASK_NOT_FOUND, taskId);
        }
        Integer shiftOrder = node.getShiftOrder();
        String machineCode = node.getMachineCode();
        ScheduleChainChangeResult<T> result = chain.remove(node,
                this.operationContext(context, settings.getManualDeleteOperation()));
        context.removeTaskNode(taskId);
        policy.recalculateChainTimes(context, chain, machineCode, shiftOrder);
        return result;
    }

    public ScheduleChainChangeResult<T> transferMachine(String taskId, String targetMachineCode,
                                                         TaskChainTransferPosition position, C context,
                                                         TaskChainPolicy<C, T> policy) {
        this.validateTaskId(taskId, policy);
        if (this.isBlank(targetMachineCode)) {
            throw policy.error(TaskChainErrorType.MACHINE_EMPTY, null);
        }
        if (position == null || position.getShiftOrder() == null) {
            throw policy.error(TaskChainErrorType.SHIFT_INVALID, null);
        }
        this.validateContextChain(context, policy);
        ScheduleTaskNode<T> sourceNode = this.findNode(taskId, context);
        ScheduleTaskLinkedList<T> sourceChain = this.ownerChain(sourceNode);
        if (sourceNode == null || sourceChain == null) {
            throw policy.error(TaskChainErrorType.TASK_NOT_FOUND, taskId);
        }
        ScheduleTaskLinkedList<T> targetChain = context.getTaskChainGroup().getOrCreate(targetMachineCode,
                this.toLocalDate(context, policy), position.getShiftOrder());
        Integer sourceShiftOrder = sourceNode.getShiftOrder();
        String sourceMachineCode = sourceNode.getMachineCode();
        sourceNode.setMachineCode(targetMachineCode);
        sourceNode.setShiftOrder(position.getShiftOrder());
        sourceNode.setShiftCode(settings.getShiftCodePrefix() + position.getShiftOrder());
        ScheduleTaskNode<T> anchor = targetChain.findByTaskId(position.getAnchorTaskId());
        ScheduleChainChangeResult<T> result = sourceChain.transferTo(sourceNode, targetChain, anchor,
                this.operationContext(context, settings.getManualTransferOperation()));
        context.registerTaskNode(taskId, sourceNode);
        policy.recalculateChainTimes(context, sourceChain, sourceMachineCode, sourceShiftOrder);
        policy.recalculateChainTimes(context, targetChain, targetMachineCode, position.getShiftOrder());
        return result;
    }

    public ScheduleChainChangeResult<T> changeQty(String taskId, BigDecimal newPlanQty, Integer shiftOrder,
                                                   C context, TaskChainPolicy<C, T> policy) {
        this.validateTaskId(taskId, policy);
        if (newPlanQty == null) {
            throw policy.error(TaskChainErrorType.PARAM_EMPTY, null);
        }
        if (shiftOrder == null) {
            throw policy.error(TaskChainErrorType.SHIFT_INVALID, null);
        }
        this.validateContextChain(context, policy);
        ScheduleTaskNode<T> node = this.findNode(taskId, context);
        ScheduleTaskLinkedList<T> chain = this.ownerChain(node);
        if (node == null || chain == null) {
            throw policy.error(TaskChainErrorType.TASK_NOT_FOUND, taskId);
        }
        node.setPlanQty(newPlanQty);
        if (node.getTask() != null) {
            node.getTask().setPlanQty(newPlanQty);
        }
        ScheduleChainChangeResult<T> result = chain.resequence(
                this.operationContext(context, settings.getChangeQtyOperation()));
        policy.recalculateChainTimes(context, chain, node.getMachineCode(), shiftOrder);
        policy.traceChainState(context, chain, settings.getChangeQtyOperation(),
                node.getMachineCode(), shiftOrder, taskId);
        return result;
    }

    private void finishChange(C context, ScheduleTaskLinkedList<T> chain, ScheduleTaskNode<T> node,
                              String operation, TaskChainPolicy<C, T> policy) {
        context.registerTaskNode(node.getTaskId(), node);
        policy.recalculateChainTimes(context, chain, node.getMachineCode(), node.getShiftOrder());
        policy.traceChainState(context, chain, operation, node.getMachineCode(),
                node.getShiftOrder(), node.getTaskId());
    }

    private ScheduleTaskNode<T> toNode(T task, String machineCode, Integer shiftOrder, C context,
                                       TaskChainPolicy<C, T> policy) {
        return new ScheduleTaskNode<>(task.getBusinessKey(), task, machineCode, this.toLocalDate(context, policy),
                settings.getShiftCodePrefix() + shiftOrder, shiftOrder, task.getPlanQty());
    }

    private ScheduleTaskNode<T> findNode(String taskId, C context) {
        ScheduleTaskNode<T> node = context.getTaskNode(taskId);
        if (node != null) {
            return node;
        }
        for (ScheduleTaskLinkedList<T> chain : context.getTaskChainGroup().values()) {
            ScheduleTaskNode<T> found = chain.findByTaskId(taskId);
            if (found != null) {
                context.registerTaskNode(taskId, found);
                return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ScheduleTaskLinkedList<T> ownerChain(ScheduleTaskNode<T> node) {
        return node != null && node.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<T>) node.getOwnerList() : null;
    }

    private ScheduleOperationContext operationContext(C context, String reason) {
        return new ScheduleOperationContext(context.getOperator(), reason, context.getTraceId());
    }

    private LocalDate toLocalDate(C context, TaskChainPolicy<C, T> policy) {
        if (context.getScheduleDate() == null) {
            throw policy.error(TaskChainErrorType.SCHEDULE_DATE_EMPTY, null);
        }
        return context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void validateTaskAndContext(T task, C context, TaskChainPolicy<C, T> policy) {
        if (task == null) {
            throw policy.error(TaskChainErrorType.TASK_NOT_FOUND, null);
        }
        if (context == null) {
            throw policy.error(TaskChainErrorType.CONTEXT_EMPTY, null);
        }
    }

    private void validateMachine(ScheduleQualityMachineCandidate machine, TaskChainPolicy<C, T> policy) {
        if (machine == null || this.isBlank(machine.getMachineCode())) {
            throw policy.error(TaskChainErrorType.MACHINE_EMPTY, null);
        }
    }

    private void validateTaskId(String taskId, TaskChainPolicy<C, T> policy) {
        if (this.isBlank(taskId)) {
            throw policy.error(TaskChainErrorType.TASK_NOT_FOUND, null);
        }
    }

    private void validateContextChain(C context, TaskChainPolicy<C, T> policy) {
        if (context == null || context.getTaskChainGroup() == null) {
            throw policy.error(TaskChainErrorType.CONTEXT_EMPTY, null);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
