package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎侧任务链排程服务。
 *
 * <p>统一处理自动排程和人工操作对运行态任务链的修改。当前为骨架实现，只实现自动追加和
 * 人工插单的基础链表操作；删除、转机台和调量待业务口径确认后补充完整查找与重算逻辑。</p>
 */
@Slf4j
@Service
public class TcTaskChainScheduleService {

    /**
     * 自动排程追加任务。
     *
     * @param task    待排任务草稿
     * @param machine 选中候选机台
     * @param context 胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> appendAutoTask(TcTaskDraft task, TcMachineCandidate machine,
                                                                 TcScheduleContext context) {
        validateTaskAndContext(task, context);
        if (machine == null || StrUtil.isBlank(machine.getMachineCode())) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        task.setMachineCode(machine.getMachineCode());
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(machine.getMachineCode(), toLocalDate(context), shiftOrder);
        ScheduleTaskNode<TcTaskDraft> node = toNode(task, machine.getMachineCode(), shiftOrder, context);
        ScheduleChainChangeResult<TcTaskDraft> result = chain.append(node,
                operationContext(context, TcScheduleConstants.CHAIN_OPERATION_AUTO_APPEND));
        context.registerTaskNode(node.getTaskId(), node);
        this.recalculateChainTimes(context, chain, machine.getMachineCode(), shiftOrder);
        this.logChainState(context, chain, TcScheduleConstants.CHAIN_OPERATION_AUTO_APPEND,
                machine.getMachineCode(), shiftOrder, node.getTaskId());
        return result;
    }

    /**
     * 自动排程前插任务。
     *
     * <p>用于顺延量新建任务时抢占目标机台目标班次的第一优先顺序，避免后续普通任务先占用产能。</p>
     *
     * @param task    待排任务草稿
     * @param machine 选中候选机台
     * @param context 胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> prependAutoTask(TcTaskDraft task, TcMachineCandidate machine,
                                                                  TcScheduleContext context) {
        validateTaskAndContext(task, context);
        if (machine == null || StrUtil.isBlank(machine.getMachineCode())) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        task.setMachineCode(machine.getMachineCode());
        Integer shiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(machine.getMachineCode(), toLocalDate(context), shiftOrder);
        ScheduleTaskNode<TcTaskDraft> node = toNode(task, machine.getMachineCode(), shiftOrder, context);
        ScheduleChainChangeResult<TcTaskDraft> result = chain.prepend(node,
                operationContext(context, TcScheduleConstants.CHAIN_OPERATION_AUTO_PREPEND));
        context.registerTaskNode(node.getTaskId(), node);
        this.recalculateChainTimes(context, chain, machine.getMachineCode(), shiftOrder);
        this.logChainState(context, chain, TcScheduleConstants.CHAIN_OPERATION_AUTO_PREPEND,
                machine.getMachineCode(), shiftOrder, node.getTaskId());
        return result;
    }

    /**
     * 人工插单。
     *
     * @param task     插单任务草稿
     * @param position 插入位置
     * @param context  胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> insertManualTask(TcTaskDraft task, TcInsertPosition position,
                                                                   TcScheduleContext context) {
        validateTaskAndContext(task, context);
        if (position == null || StrUtil.isBlank(position.getMachineCode()) || position.getShiftOrder() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_PARAM_EMPTY.getDefaultMessage());
        }
        task.setMachineCode(position.getMachineCode());
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(position.getMachineCode(), toLocalDate(context), position.getShiftOrder());
        ScheduleTaskNode<TcTaskDraft> anchor = findNode(position.getAnchorTaskId(), context);
        if (anchor == null || anchor.getOwnerList() != chain) {
            anchor = chain.findByTaskId(position.getAnchorTaskId());
        }
        ScheduleTaskNode<TcTaskDraft> node = toNode(task, position.getMachineCode(), position.getShiftOrder(), context);
        ScheduleChainChangeResult<TcTaskDraft> result = chain.insertAfter(anchor, node,
                operationContext(context, TcScheduleConstants.CHAIN_OPERATION_MANUAL_INSERT));
        context.registerTaskNode(node.getTaskId(), node);
        this.recalculateChainTimes(context, chain, position.getMachineCode(), position.getShiftOrder());
        this.logChainState(context, chain, TcScheduleConstants.CHAIN_OPERATION_MANUAL_INSERT,
                position.getMachineCode(), position.getShiftOrder(), node.getTaskId());
        return result;
    }

    /**
     * 删除任务。
     *
     * <p>按任务ID在全部已加载任务链中查找目标节点，找到后从所属链表摘除并重排后续顺序。
     * 查找范围为上下文内已加载的机台班次任务链集合。</p>
     *
     * @param taskId  任务标识（对应TcTaskDraft.businessKey）
     * @param context 胎侧排程上下文
     * @return 链表变更结果，包含被删除节点和受影响节点
     * @throws ServiceException 任务ID为空或未找到目标节点时抛出
     */
    public ScheduleChainChangeResult<TcTaskDraft> removeTask(String taskId, TcScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        ScheduleTaskNode<TcTaskDraft> indexedNode = findNode(taskId, context);
        if (indexedNode != null && indexedNode.getOwnerList() instanceof ScheduleTaskLinkedList) {
            ScheduleTaskLinkedList<TcTaskDraft> ownerChain = (ScheduleTaskLinkedList<TcTaskDraft>) indexedNode.getOwnerList();
            Integer shiftOrder = indexedNode.getShiftOrder();
            String machineCode = indexedNode.getMachineCode();
            ScheduleChainChangeResult<TcTaskDraft> result = ownerChain.remove(indexedNode,
                    operationContext(context, TcScheduleConstants.CHAIN_OPERATION_MANUAL_DELETE));
            context.removeTaskNode(taskId);
            this.recalculateChainTimes(context, ownerChain, machineCode, shiftOrder);
            return result;
        }
        throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage() + ":" + taskId);
    }

    /**
     * 转机台。
     *
     * <p>从原机台任务链中摘除目标节点，插入目标机台指定班次任务链的指定位置或链尾。
     * 原链和目标链分别触发重新编号。当前不处理发布状态回退，由上层操作门面统一处理。</p>
     *
     * @param taskId            任务标识（对应TcTaskDraft.businessKey）
     * @param targetMachineCode 目标机台编码
     * @param position          目标位置，包含目标班次顺序和锚点任务ID
     * @param context           胎侧排程上下文
     * @return 链表变更结果，包含原链和目标链的受影响节点
     * @throws ServiceException 参数缺失、任务未找到或目标链表不存在时抛出
     */
    public ScheduleChainChangeResult<TcTaskDraft> transferMachine(String taskId, String targetMachineCode,
                                                                   TcTransferPosition position, TcScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (StrUtil.isBlank(targetMachineCode)) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        if (position == null || position.getShiftOrder() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SHIFT_INVALID.getDefaultMessage());
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        LocalDate localDate = toLocalDate(context);
        ScheduleOperationContext opCtx = operationContext(context,
                TcScheduleConstants.CHAIN_OPERATION_MANUAL_TRANSFER);

        // 在原链中查找目标节点
        ScheduleTaskNode<TcTaskDraft> sourceNode = findNode(taskId, context);
        ScheduleTaskLinkedList<TcTaskDraft> sourceChain = sourceNode != null
                && sourceNode.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<TcTaskDraft>) sourceNode.getOwnerList() : null;
        if (sourceNode == null || sourceChain == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage() + ":" + taskId);
        }

        // 获取目标链表
        ScheduleTaskLinkedList<TcTaskDraft> targetChain = context.getTaskChainGroup()
                .getOrCreate(targetMachineCode, localDate, position.getShiftOrder());

        // 更新节点的机台和班次定位，确保转移后目标链时间按目标班次重算。
        Integer sourceShiftOrder = sourceNode.getShiftOrder();
        String sourceMachineCode = sourceNode.getMachineCode();
        sourceNode.setMachineCode(targetMachineCode);
        sourceNode.setShiftOrder(position.getShiftOrder());
        sourceNode.setShiftCode("CLASS" + position.getShiftOrder());
        ScheduleTaskNode<TcTaskDraft> anchorNode = targetChain.findByTaskId(position.getAnchorTaskId());

        // 执行跨链转移
        ScheduleChainChangeResult<TcTaskDraft> result = sourceChain.transferTo(sourceNode, targetChain, anchorNode, opCtx);
        context.registerTaskNode(taskId, sourceNode);
        this.recalculateChainTimes(context, sourceChain, sourceMachineCode, sourceShiftOrder);
        this.recalculateChainTimes(context, targetChain, targetMachineCode, position.getShiftOrder());
        return result;
    }

    /**
     * 调整计划量。
     *
     * <p>按任务ID查找目标节点，更新节点和草稿的计划量，然后对所属链表触发重新编号。
     * 当前版本只更新计划量数值，不处理跨班归属变化，由上层操作门面在必要时触发局部重算。</p>
     *
     * @param taskId     任务标识（对应TcTaskDraft.businessKey）
     * @param newPlanQty 新计划量
     * @param shiftOrder 班次顺序，用于日志和操作上下文
     * @param context    胎侧排程上下文
     * @return 链表变更结果
     * @throws ServiceException 参数缺失、任务未找到或新计划量为空时抛出
     */
    public ScheduleChainChangeResult<TcTaskDraft> changeQty(String taskId, BigDecimal newPlanQty, Integer shiftOrder,
                                                             TcScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (newPlanQty == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_PARAM_EMPTY.getDefaultMessage());
        }
        if (shiftOrder == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SHIFT_INVALID.getDefaultMessage());
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }

        // 遍历所有已加载链表查找目标节点
        ScheduleTaskNode<TcTaskDraft> targetNode = findNode(taskId, context);
        ScheduleTaskLinkedList<TcTaskDraft> targetChain = targetNode != null
                && targetNode.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<TcTaskDraft>) targetNode.getOwnerList() : null;
        if (targetNode == null || targetChain == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage() + ":" + taskId);
        }

        // 更新节点计划量和草稿计划量
        targetNode.setPlanQty(newPlanQty);
        if (targetNode.getTask() != null) {
            targetNode.getTask().setPlanQty(newPlanQty);
        }

        // 触发重新编号和时间重算
        ScheduleChainChangeResult<TcTaskDraft> result = targetChain.resequence(
                operationContext(context, TcScheduleConstants.CHAIN_OPERATION_CHANGE_QTY));
        this.recalculateChainTimes(context, targetChain, targetNode.getMachineCode(), shiftOrder);
        this.logChainState(context, targetChain, TcScheduleConstants.CHAIN_OPERATION_CHANGE_QTY,
                targetNode.getMachineCode(), shiftOrder, taskId);
        return result;
    }

    /**
     * 打印任务链变更后的顺序摘要，说明最终班次顺位如何形成。
     *
     * @param context 排程上下文
     * @param chain 当前机台班次任务链
     * @param operation 操作类型
     * @param machineCode 机台编码
     * @param shiftOrder 班次顺序
     * @param changedTaskId 本次变化的任务业务键
     */
    private void logChainState(TcScheduleContext context, ScheduleTaskLinkedList<TcTaskDraft> chain, String operation,
                               String machineCode, Integer shiftOrder, String changedTaskId) {
        String chainOrder = chain == null ? "" : chain.toList().stream()
                .map(ScheduleTaskNode::getTaskId)
                .collect(Collectors.joining(","));
        log.info("[TC_TASK_CHAIN] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, operation={}, machineCode={}, shiftOrder={}, changedTaskId={}, chainSize={}, chainOrder={}",
                context == null ? null : context.getBatchNo(), context == null ? null : context.getTraceId(),
                context == null ? null : context.getFactoryCode(),
                context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate()),
                operation, machineCode, shiftOrder, changedTaskId, chain == null ? 0 : chain.getSize(), chainOrder);
    }

    /**
     * 重算指定机台班次任务链内所有节点的预计开始和结束时间。
     *
     * <p>同一链表内任务按当前顺序连续排布，首个任务从班次计划开始时间开始；
     * 后续任务从前一任务结束时间开始。缺少班次窗口或生产速度时不抛异常，
     * 只清空对应节点时间，避免阻断既有排程主流程。</p>
     *
     * @param context 排程上下文
     * @param chain 当前机台班次任务链
     * @param machineCode 机台编码
     * @param shiftOrder 班次顺序
     */
    private void recalculateChainTimes(TcScheduleContext context, ScheduleTaskLinkedList<TcTaskDraft> chain,
                                       String machineCode, Integer shiftOrder) {
        if (context == null || chain == null) {
            return;
        }
        Date cursorTime = this.resolveShiftStartTime(context, shiftOrder);
        if (cursorTime == null) {
            this.clearChainTimes(chain);
            log.warn("[TC_TASK_TIME] batchNo={}, traceId={}, machineCode={}, shiftOrder={}, reason=SHIFT_WINDOW_MISSING",
                    context.getBatchNo(), context.getTraceId(), machineCode, shiftOrder);
            return;
        }
        TcTaskDraft previousTask = null;
        TcTaskPredecessor externalPredecessor = this.resolvePreviousShiftPredecessor(context, machineCode, shiftOrder);
        for (ScheduleTaskNode<TcTaskDraft> node : chain.toList()) {
            TcTaskDraft currentTask = node.getTask();
            BigDecimal planQty = this.nvl(node.getPlanQty());
            if (planQty.compareTo(BigDecimal.ZERO) <= 0) {
                node.setStartTime(null);
                node.setEndTime(null);
                if (currentTask != null) {
                    currentTask.setPreviousSpecSwitchHours(BigDecimal.ZERO);
                    currentTask.setPreviousGlueSwitchHours(BigDecimal.ZERO);
                    currentTask.setPreviousGlueSwitchCapacityDeduct(BigDecimal.ZERO);
                }
                continue;
            }
            BigDecimal machineSpeed = this.resolveNodeMachineSpeed(node);
            if (machineSpeed.compareTo(BigDecimal.ZERO) <= 0 || cursorTime == null) {
                node.setStartTime(null);
                node.setEndTime(null);
                cursorTime = null;
                if (currentTask != null) {
                    currentTask.setPreviousSpecSwitchHours(BigDecimal.ZERO);
                    currentTask.setPreviousGlueSwitchHours(BigDecimal.ZERO);
                    currentTask.setPreviousGlueSwitchCapacityDeduct(BigDecimal.ZERO);
                }
                log.warn("[TC_TASK_TIME] batchNo={}, traceId={}, machineCode={}, shiftOrder={}, taskId={}, reason=MACHINE_SPEED_MISSING",
                        context.getBatchNo(), context.getTraceId(), machineCode, shiftOrder, node.getTaskId());
                continue;
            }
            BigDecimal specSwitchHours = this.resolveSpecSwitchHours(context, previousTask, externalPredecessor,
                    currentTask);
            String previousGlueCode = previousTask == null
                    ? (externalPredecessor == null ? null : externalPredecessor.getGlueCode())
                    : previousTask.getGlueCode();
            BigDecimal glueSwitchCapacityDeduct = this.resolveGlueSwitchCapacityDeduct(context, previousTask,
                    externalPredecessor, currentTask);
            BigDecimal glueSwitchHours = this.convertCapacityDeductToHours(glueSwitchCapacityDeduct, machineSpeed);
            if (currentTask != null) {
                currentTask.setPreviousSpecSwitchHours(specSwitchHours);
                currentTask.setPreviousGlueSwitchHours(glueSwitchHours);
                currentTask.setPreviousGlueSwitchCapacityDeduct(glueSwitchCapacityDeduct);
            }
            long switchSeconds = specSwitchHours.add(glueSwitchHours)
                    .multiply(BigDecimal.valueOf(TcScheduleConstants.SECONDS_PER_HOUR))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            Date startTime = new Date(cursorTime.getTime() + switchSeconds * 1000L);
            long durationSeconds = this.calculateDurationSeconds(planQty, machineSpeed);
            Date endTime = new Date(startTime.getTime() + durationSeconds * 1000L);
            node.setStartTime(startTime);
            node.setEndTime(endTime);
            cursorTime = endTime;
            previousTask = currentTask;
            externalPredecessor = null;
            log.info("[TC_TASK_SWITCH] batchNo={}, traceId={}, machineCode={}, shiftOrder={}, taskId={}, previousGlueCode={}, currentGlueCode={}, specSwitchHours={}, glueSwitchHours={}, glueSwitchCapacityDeduct={}, switchCapacityDeduct={}",
                    context.getBatchNo(), context.getTraceId(), machineCode, shiftOrder, node.getTaskId(),
                    previousGlueCode, currentTask == null ? null : currentTask.getGlueCode(),
                    specSwitchHours, glueSwitchHours, glueSwitchCapacityDeduct,
                    specSwitchHours.multiply(machineSpeed).add(glueSwitchCapacityDeduct));
        }
    }

    /**
     * 解析班次计划开始时间。
     *
     * @param context 排程上下文
     * @param shiftOrder 班次顺序
     * @return 班次开始时间；缺失或格式非法时返回 null
     */
    private Date resolveShiftStartTime(TcScheduleContext context, Integer shiftOrder) {
        TcShiftTimeWindow window = context.getShiftTimeWindowMap().get(shiftOrder);
        if (window == null || StrUtil.isBlank(window.getPlanStartTime()) || context.getScheduleDate() == null) {
            return null;
        }
        try {
            Date shiftDate = DateUtil.offsetDay(context.getScheduleDate(), (shiftOrder - 1) / 3);
            return DateUtil.parse(DateUtil.formatDate(shiftDate) + " " + window.getPlanStartTime());
        } catch (Exception exception) {
            log.warn("[TC_TASK_TIME] batchNo={}, traceId={}, shiftOrder={}, planStartTime={}, reason=SHIFT_START_PARSE_FAILED",
                    context.getBatchNo(), context.getTraceId(), shiftOrder, window.getPlanStartTime(), exception);
            return null;
        }
    }

    /**
     * 解析当前班首任务之前的有效前置任务。
     *
     * @param context     排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder  当前班次
     * @return 上一班链尾或排程日前置快照
     */
    private TcTaskPredecessor resolvePreviousShiftPredecessor(TcScheduleContext context, String machineCode,
                                                              Integer shiftOrder) {
        for (int previousShiftOrder = shiftOrder - 1; previousShiftOrder >= 1; previousShiftOrder--) {
            ScheduleTaskLinkedList<TcTaskDraft> previousChain = context.getTaskChain(machineCode, previousShiftOrder);
            if (previousChain == null || previousChain.toList().isEmpty()) {
                continue;
            }
            List<ScheduleTaskNode<TcTaskDraft>> nodes = previousChain.toList();
            TcTaskDraft tailTask = nodes.get(nodes.size() - 1).getTask();
            if (tailTask == null) {
                continue;
            }
            TcTaskPredecessor predecessor = new TcTaskPredecessor();
            predecessor.setSidewallCode(tailTask.getSidewallCode());
            predecessor.setGlueCode(tailTask.getGlueCode());
            predecessor.setShiftOrder(previousShiftOrder);
            predecessor.setBusinessKey(tailTask.getBusinessKey());
            return predecessor;
        }
        return context.getMachinePredecessorMap().get(machineCode);
    }

    /**
     * 计算规格切换小时数。
     *
     * @param context             排程上下文
     * @param previousTask        当前链内前置任务
     * @param externalPredecessor 上一班或排程日前置任务
     * @param currentTask         当前任务
     * @return 规格切换小时数
     */
    private BigDecimal resolveSpecSwitchHours(TcScheduleContext context, TcTaskDraft previousTask,
                                              TcTaskPredecessor externalPredecessor, TcTaskDraft currentTask) {
        String previousSidewallCode = previousTask == null
                ? (externalPredecessor == null ? null : externalPredecessor.getSidewallCode())
                : previousTask.getSidewallCode();
        if (currentTask == null || previousSidewallCode == null
                || Objects.equals(previousSidewallCode, currentTask.getSidewallCode())) {
            return BigDecimal.ZERO;
        }
        return this.resolveSwitchParamHours(context, TcScheduleConstants.PARAM_SPEC_CHANGE_MINUTES);
    }

    /**
     * 计算主胶料切换固定产能扣减量。
     *
     * @param context             排程上下文
     * @param previousTask        当前链内前置任务
     * @param externalPredecessor 上一班或排程日前置任务
     * @param currentTask         当前任务
     * @return 主胶料切换固定产能扣减量
     */
    private BigDecimal resolveGlueSwitchCapacityDeduct(TcScheduleContext context, TcTaskDraft previousTask,
                                                       TcTaskPredecessor externalPredecessor,
                                                       TcTaskDraft currentTask) {
        String previousGlueCode = previousTask == null
                ? (externalPredecessor == null ? null : externalPredecessor.getGlueCode())
                : previousTask.getGlueCode();
        if (currentTask == null || StrUtil.isBlank(previousGlueCode) || StrUtil.isBlank(currentTask.getGlueCode())
                || Objects.equals(previousGlueCode.trim(), currentTask.getGlueCode().trim())) {
            return BigDecimal.ZERO;
        }
        TcParamValue paramValue = context.getParamMap().get(TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT);
        String value = paramValue == null
                ? TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT
                : StrUtil.blankToDefault(paramValue.getEffectiveValue(),
                TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT);
        try {
            BigDecimal deduct = new BigDecimal(value);
            if (deduct.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[TC_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=NEGATIVE_VALUE",
                        context.getBatchNo(), context.getTraceId(),
                        TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT, value);
                return BigDecimal.ZERO;
            }
            return deduct;
        } catch (NumberFormatException exception) {
            log.warn("[TC_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, defaultValue={}, reason=INVALID_NUMBER",
                    context.getBatchNo(), context.getTraceId(),
                    TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT, value,
                    TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT, exception);
            return new BigDecimal(TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT);
        }
    }

    /**
     * 将固定产能扣减量按当前任务速度折算为切换小时数。
     *
     * @param capacityDeduct 固定产能扣减量
     * @param machineSpeed 当前任务机台速度
     * @return 切换小时数；速度无效时返回0
     */
    private BigDecimal convertCapacityDeductToHours(BigDecimal capacityDeduct, BigDecimal machineSpeed) {
        if (this.nvl(capacityDeduct).compareTo(BigDecimal.ZERO) <= 0
                || this.nvl(machineSpeed).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return capacityDeduct.divide(machineSpeed, TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                RoundingMode.HALF_UP);
    }

    /**
     * 将切换分钟数参数转换为小时数。
     *
     * @param context   排程上下文
     * @param paramCode 参数编码
     * @return 非负切换小时数，参数缺失或非法时返回0
     */
    private BigDecimal resolveSwitchParamHours(TcScheduleContext context, String paramCode) {
        TcParamValue paramValue = context.getParamMap().get(paramCode);
        String value = paramValue == null ? "0" : StrUtil.blankToDefault(paramValue.getEffectiveValue(), "0");
        try {
            BigDecimal switchMinutes = new BigDecimal(value).max(BigDecimal.ZERO);
            if (switchMinutes.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return switchMinutes.divide(BigDecimal.valueOf(TcScheduleConstants.MINUTES_PER_HOUR),
                    TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            log.warn("[TC_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=INVALID_NUMBER",
                    context.getBatchNo(), context.getTraceId(), paramCode, value, exception);
            return BigDecimal.ZERO;
        }
    }
    /**
     * 解析节点使用的机台速度。
     *
     * @param node 任务链节点
     * @return 机台速度，缺失时返回 0
     */
    private BigDecimal resolveNodeMachineSpeed(ScheduleTaskNode<TcTaskDraft> node) {
        if (node == null || node.getTask() == null) {
            return BigDecimal.ZERO;
        }
        return this.nvl(node.getTask().getMachineSpeed());
    }

    /**
     * 按计划量和生产速度计算生产时长秒数。
     *
     * @param planQty 计划量，单位米
     * @param machineSpeed 生产速度，单位米/小时
     * @return 向上取整后的生产秒数
     */
    private long calculateDurationSeconds(BigDecimal planQty, BigDecimal machineSpeed) {
        long durationSeconds = planQty.multiply(BigDecimal.valueOf(TcScheduleConstants.SECONDS_PER_HOUR))
                .divide(machineSpeed, 0, RoundingMode.CEILING)
                .longValue();
        if (durationSeconds < 1L) {
            return 1L;
        }
        return durationSeconds;
    }

    /**
     * 清空整条任务链的预计起止时间。
     *
     * @param chain 任务链
     */
    private void clearChainTimes(ScheduleTaskLinkedList<TcTaskDraft> chain) {
        for (ScheduleTaskNode<TcTaskDraft> node : chain.toList()) {
            node.setStartTime(null);
            node.setEndTime(null);
        }
    }

    /**
     * 空数值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ScheduleTaskNode<TcTaskDraft> toNode(TcTaskDraft task, String machineCode, Integer shiftOrder,
                                                TcScheduleContext context) {
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
    private ScheduleTaskNode<TcTaskDraft> findNode(String taskId, TcScheduleContext context) {
        ScheduleTaskNode<TcTaskDraft> node = context.getTaskNode(taskId);
        if (node != null) {
            return node;
        }
        for (ScheduleTaskLinkedList<TcTaskDraft> chain : context.getTaskChainGroup().values()) {
            ScheduleTaskNode<TcTaskDraft> found = chain.findByTaskId(taskId);
            if (found != null) {
                context.registerTaskNode(taskId, found);
                return found;
            }
        }
        return null;
    }

    private ScheduleOperationContext operationContext(TcScheduleContext context, String reason) {
        return new ScheduleOperationContext(context.getOperator(), reason, context.getTraceId());
    }

    private LocalDate toLocalDate(TcScheduleContext context) {
        if (context.getScheduleDate() == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }
        return DateUtil.toLocalDateTime(context.getScheduleDate()).toLocalDate();
    }

    private void validateTaskAndContext(TcTaskDraft task, TcScheduleContext context) {
        if (task == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_TASK_NOT_FOUND.getDefaultMessage());
        }
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
    }
}
