package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 胎面任务链排程服务。
 *
 * <p>统一处理自动排程和人工操作对运行态任务链的修改。当前为骨架实现，只实现自动追加和
 * 人工插单的基础链表操作；删除、转机台和调量待业务口径确认后补充完整查找与重算逻辑。</p>
 */
@Service
public class TmTaskChainScheduleService {

    /**
     * 自动排程追加任务。
     *
     * @param task    待排任务草稿
     * @param machine 选中候选机台
     * @param context 胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> appendAutoTask(TmTaskDraft task, TmMachineCandidate machine,
                                                                 TmScheduleContext context) {
        validateTaskAndContext(task, context);
        if (machine == null || StrUtil.isBlank(machine.getMachineCode())) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        task.setMachineCode(machine.getMachineCode());
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(machine.getMachineCode(), toLocalDate(context), shiftOrder);
        ScheduleTaskNode<TmTaskDraft> node = toNode(task, machine.getMachineCode(), shiftOrder, context);
        ScheduleChainChangeResult<TmTaskDraft> result = chain.append(node, operationContext(context, "AUTO_APPEND"));
        context.registerTaskNode(node.getTaskId(), node);
        return result;
    }

    /**
     * 人工插单。
     *
     * @param task     插单任务草稿
     * @param position 插入位置
     * @param context  胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> insertManualTask(TmTaskDraft task, TmInsertPosition position,
                                                                   TmScheduleContext context) {
        validateTaskAndContext(task, context);
        if (position == null || StrUtil.isBlank(position.getMachineCode()) || position.getShiftOrder() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_PARAM_EMPTY.getDefaultMessage());
        }
        task.setMachineCode(position.getMachineCode());
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(position.getMachineCode(), toLocalDate(context), position.getShiftOrder());
        ScheduleTaskNode<TmTaskDraft> anchor = findNode(position.getAnchorTaskId(), context);
        if (anchor == null || anchor.getOwnerList() != chain) {
            anchor = chain.findByTaskId(position.getAnchorTaskId());
        }
        ScheduleTaskNode<TmTaskDraft> node = toNode(task, position.getMachineCode(), position.getShiftOrder(), context);
        ScheduleChainChangeResult<TmTaskDraft> result = chain.insertAfter(anchor, node, operationContext(context, "MANUAL_INSERT"));
        context.registerTaskNode(node.getTaskId(), node);
        return result;
    }

    /**
     * 删除任务。
     *
     * <p>按任务ID在全部已加载任务链中查找目标节点，找到后从所属链表摘除并重排后续顺序。
     * 查找范围为上下文内已加载的机台班次任务链集合。</p>
     *
     * @param taskId  任务标识（对应TmTaskDraft.businessKey）
     * @param context 胎面排程上下文
     * @return 链表变更结果，包含被删除节点和受影响节点
     * @throws ServiceException 任务ID为空或未找到目标节点时抛出
     */
    public ScheduleChainChangeResult<TmTaskDraft> removeTask(String taskId, TmScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        ScheduleTaskNode<TmTaskDraft> indexedNode = findNode(taskId, context);
        if (indexedNode != null && indexedNode.getOwnerList() instanceof ScheduleTaskLinkedList) {
            ScheduleTaskLinkedList<TmTaskDraft> ownerChain = (ScheduleTaskLinkedList<TmTaskDraft>) indexedNode.getOwnerList();
            ScheduleChainChangeResult<TmTaskDraft> result = ownerChain.remove(indexedNode, operationContext(context, "MANUAL_DELETE"));
            context.removeTaskNode(taskId);
            return result;
        }
        throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage() + ":" + taskId);
    }

    /**
     * 转机台。
     *
     * <p>从原机台任务链中摘除目标节点，插入目标机台指定班次任务链的指定位置或链尾。
     * 原链和目标链分别触发重新编号。当前不处理发布状态回退，由上层操作门面统一处理。</p>
     *
     * @param taskId            任务标识（对应TmTaskDraft.businessKey）
     * @param targetMachineCode 目标机台编码
     * @param position          目标位置，包含目标班次顺序和锚点任务ID
     * @param context           胎面排程上下文
     * @return 链表变更结果，包含原链和目标链的受影响节点
     * @throws ServiceException 参数缺失、任务未找到或目标链表不存在时抛出
     */
    public ScheduleChainChangeResult<TmTaskDraft> transferMachine(String taskId, String targetMachineCode,
                                                                   TmTransferPosition position, TmScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (StrUtil.isBlank(targetMachineCode)) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        if (position == null || position.getShiftOrder() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SHIFT_INVALID.getDefaultMessage());
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        LocalDate localDate = toLocalDate(context);
        ScheduleOperationContext opCtx = operationContext(context, "MANUAL_TRANSFER");

        // 在原链中查找目标节点
        ScheduleTaskNode<TmTaskDraft> sourceNode = findNode(taskId, context);
        ScheduleTaskLinkedList<TmTaskDraft> sourceChain = sourceNode != null
                && sourceNode.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<TmTaskDraft>) sourceNode.getOwnerList() : null;
        if (sourceNode == null || sourceChain == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage() + ":" + taskId);
        }

        // 获取目标链表
        ScheduleTaskLinkedList<TmTaskDraft> targetChain = context.getTaskChainGroup()
                .getOrCreate(targetMachineCode, localDate, position.getShiftOrder());

        // 更新节点的机台编码
        sourceNode.setMachineCode(targetMachineCode);
        ScheduleTaskNode<TmTaskDraft> anchorNode = targetChain.findByTaskId(position.getAnchorTaskId());

        // 执行跨链转移
        ScheduleChainChangeResult<TmTaskDraft> result = sourceChain.transferTo(sourceNode, targetChain, anchorNode, opCtx);
        context.registerTaskNode(taskId, sourceNode);
        return result;
    }

    /**
     * 调整计划量。
     *
     * <p>按任务ID查找目标节点，更新节点和草稿的计划量，然后对所属链表触发重新编号。
     * 当前版本只更新计划量数值，不处理跨班归属变化，由上层操作门面在必要时触发局部重算。</p>
     *
     * @param taskId     任务标识（对应TmTaskDraft.businessKey）
     * @param newPlanQty 新计划量
     * @param shiftOrder 班次顺序，用于日志和操作上下文
     * @param context    胎面排程上下文
     * @return 链表变更结果
     * @throws ServiceException 参数缺失、任务未找到或新计划量为空时抛出
     */
    public ScheduleChainChangeResult<TmTaskDraft> changeQty(String taskId, BigDecimal newPlanQty, Integer shiftOrder,
                                                             TmScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (newPlanQty == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_PARAM_EMPTY.getDefaultMessage());
        }
        if (shiftOrder == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SHIFT_INVALID.getDefaultMessage());
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }

        // 遍历所有已加载链表查找目标节点
        ScheduleTaskNode<TmTaskDraft> targetNode = findNode(taskId, context);
        ScheduleTaskLinkedList<TmTaskDraft> targetChain = targetNode != null
                && targetNode.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<TmTaskDraft>) targetNode.getOwnerList() : null;
        if (targetNode == null || targetChain == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage() + ":" + taskId);
        }

        // 更新节点计划量和草稿计划量
        targetNode.setPlanQty(newPlanQty);
        if (targetNode.getTask() != null) {
            targetNode.getTask().setPlanQty(newPlanQty);
        }

        // 触发重新编号
        return targetChain.resequence(operationContext(context, "CHANGE_QTY"));
    }

    private ScheduleTaskNode<TmTaskDraft> toNode(TmTaskDraft task, String machineCode, Integer shiftOrder,
                                                TmScheduleContext context) {
        return new ScheduleTaskNode<>(task.getBusinessKey(), task, machineCode, toLocalDate(context),
                "CLASS" + shiftOrder, shiftOrder, task.getPlanQty());
    }

    /**
     * 查找任务链节点，优先使用上下文索引，缺失时回退遍历并补建索引。
     *
     * @param taskId  任务标识
     * @param context 排程上下文
     * @return 任务链节点，不存在时返回 null
     */
    private ScheduleTaskNode<TmTaskDraft> findNode(String taskId, TmScheduleContext context) {
        ScheduleTaskNode<TmTaskDraft> node = context.getTaskNode(taskId);
        if (node != null) {
            return node;
        }
        for (ScheduleTaskLinkedList<TmTaskDraft> chain : context.getTaskChainGroup().values()) {
            ScheduleTaskNode<TmTaskDraft> found = chain.findByTaskId(taskId);
            if (found != null) {
                context.registerTaskNode(taskId, found);
                return found;
            }
        }
        return null;
    }

    private ScheduleOperationContext operationContext(TmScheduleContext context, String reason) {
        return new ScheduleOperationContext(context.getOperator(), reason, context.getTraceId());
    }

    private LocalDate toLocalDate(TmScheduleContext context) {
        if (context.getScheduleDate() == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }
        return DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate();
    }

    private void validateTaskAndContext(TmTaskDraft task, TmScheduleContext context) {
        if (task == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
    }
}
