package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.*;
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
 * 胎面任务链排程服务。
 *
 * <p>统一处理自动排程和人工操作对运行态任务链的修改。当前为骨架实现，只实现自动追加和
 * 人工插单的基础链表操作；删除、转机台和调量待业务口径确认后补充完整查找与重算逻辑。</p>
 */
@Slf4j
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
        ScheduleChainChangeResult<TmTaskDraft> result = chain.append(node,
                operationContext(context, TmScheduleConstants.CHAIN_OPERATION_AUTO_APPEND));
        context.registerTaskNode(node.getTaskId(), node);
        this.recalculateChainTimes(context, chain, machine.getMachineCode(), shiftOrder);
        this.logChainState(context, chain, TmScheduleConstants.CHAIN_OPERATION_AUTO_APPEND,
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
     * @param context 胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> prependAutoTask(TmTaskDraft task, TmMachineCandidate machine,
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
        ScheduleChainChangeResult<TmTaskDraft> result = chain.prepend(node,
                operationContext(context, TmScheduleConstants.CHAIN_OPERATION_AUTO_PREPEND));
        context.registerTaskNode(node.getTaskId(), node);
        this.recalculateChainTimes(context, chain, machine.getMachineCode(), shiftOrder);
        this.logChainState(context, chain, TmScheduleConstants.CHAIN_OPERATION_AUTO_PREPEND,
                machine.getMachineCode(), shiftOrder, node.getTaskId());
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
        ScheduleChainChangeResult<TmTaskDraft> result = chain.insertAfter(anchor, node,
                operationContext(context, TmScheduleConstants.CHAIN_OPERATION_MANUAL_INSERT));
        context.registerTaskNode(node.getTaskId(), node);
        this.recalculateChainTimes(context, chain, position.getMachineCode(), position.getShiftOrder());
        this.logChainState(context, chain, TmScheduleConstants.CHAIN_OPERATION_MANUAL_INSERT,
                position.getMachineCode(), position.getShiftOrder(), node.getTaskId());
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
            Integer shiftOrder = indexedNode.getShiftOrder();
            String machineCode = indexedNode.getMachineCode();
            ScheduleChainChangeResult<TmTaskDraft> result = ownerChain.remove(indexedNode,
                    operationContext(context, TmScheduleConstants.CHAIN_OPERATION_MANUAL_DELETE));
            context.removeTaskNode(taskId);
            this.recalculateChainTimes(context, ownerChain, machineCode, shiftOrder);
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
        ScheduleOperationContext opCtx = operationContext(context,
                TmScheduleConstants.CHAIN_OPERATION_MANUAL_TRANSFER);

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

        // 更新节点的机台和班次定位，确保转移后目标链时间按目标班次重算。
        Integer sourceShiftOrder = sourceNode.getShiftOrder();
        String sourceMachineCode = sourceNode.getMachineCode();
        sourceNode.setMachineCode(targetMachineCode);
        sourceNode.setShiftOrder(position.getShiftOrder());
        sourceNode.setShiftCode("CLASS" + position.getShiftOrder());
        ScheduleTaskNode<TmTaskDraft> anchorNode = targetChain.findByTaskId(position.getAnchorTaskId());

        // 执行跨链转移
        ScheduleChainChangeResult<TmTaskDraft> result = sourceChain.transferTo(sourceNode, targetChain, anchorNode, opCtx);
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

        // 触发重新编号和时间重算
        ScheduleChainChangeResult<TmTaskDraft> result = targetChain.resequence(
                operationContext(context, TmScheduleConstants.CHAIN_OPERATION_CHANGE_QTY));
        this.recalculateChainTimes(context, targetChain, targetNode.getMachineCode(), shiftOrder);
        this.logChainState(context, targetChain, TmScheduleConstants.CHAIN_OPERATION_CHANGE_QTY,
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
    private void logChainState(TmScheduleContext context, ScheduleTaskLinkedList<TmTaskDraft> chain, String operation,
                               String machineCode, Integer shiftOrder, String changedTaskId) {
        String chainOrder = chain == null ? "" : chain.toList().stream()
                .map(ScheduleTaskNode::getTaskId)
                .collect(Collectors.joining(","));
        log.info("[TM_TASK_CHAIN] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, operation={}, machineCode={}, shiftOrder={}, changedTaskId={}, chainSize={}, chainOrder={}",
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
    private void recalculateChainTimes(TmScheduleContext context, ScheduleTaskLinkedList<TmTaskDraft> chain,
                                       String machineCode, Integer shiftOrder) {
        if (context == null || chain == null) {
            return;
        }
        Date cursorTime = this.resolveShiftStartTime(context, shiftOrder);
        if (cursorTime == null) {
            this.clearChainTimes(chain);
            log.warn("[TM_TASK_TIME] batchNo={}, traceId={}, machineCode={}, shiftOrder={}, reason=SHIFT_WINDOW_MISSING",
                    context.getBatchNo(), context.getTraceId(), machineCode, shiftOrder);
            return;
        }
        TmTaskDraft previousTask = null;
        TmTaskPredecessor externalPredecessor = this.resolvePreviousShiftPredecessor(context, machineCode, shiftOrder);
        for (ScheduleTaskNode<TmTaskDraft> node : chain.toList()) {
            TmTaskDraft currentTask = node.getTask();
            BigDecimal planQty = this.nvl(node.getPlanQty());
            if (planQty.compareTo(BigDecimal.ZERO) <= 0) {
                node.setStartTime(null);
                node.setEndTime(null);
                if (currentTask != null) {
                    currentTask.setPreviousSpecSwitchHours(BigDecimal.ZERO);
                    currentTask.setPreviousGlueSwitchHours(BigDecimal.ZERO);
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
                }
                log.warn("[TM_TASK_TIME] batchNo={}, traceId={}, machineCode={}, shiftOrder={}, taskId={}, reason=MACHINE_SPEED_MISSING",
                        context.getBatchNo(), context.getTraceId(), machineCode, shiftOrder, node.getTaskId());
                continue;
            }
            BigDecimal specSwitchHours = this.resolveSpecSwitchHours(context, previousTask, externalPredecessor,
                    currentTask);
            BigDecimal glueSwitchHours = this.resolveGlueSwitchHours(context, previousTask, externalPredecessor,
                    currentTask);
            if (currentTask != null) {
                currentTask.setPreviousSpecSwitchHours(specSwitchHours);
                currentTask.setPreviousGlueSwitchHours(glueSwitchHours);
            }
            long switchSeconds = specSwitchHours.add(glueSwitchHours)
                    .multiply(BigDecimal.valueOf(TmScheduleConstants.SECONDS_PER_HOUR))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            Date startTime = new Date(cursorTime.getTime() + switchSeconds * 1000L);
            long durationSeconds = this.calculateDurationSeconds(planQty, machineSpeed);
            Date endTime = new Date(startTime.getTime() + durationSeconds * 1000L);
            node.setStartTime(startTime);
            node.setEndTime(endTime);
            cursorTime = endTime;
            previousTask = currentTask;
            externalPredecessor = null;
            log.info("[TM_TASK_SWITCH] batchNo={}, traceId={}, machineCode={}, shiftOrder={}, taskId={}, specSwitchHours={}, glueSwitchHours={}, switchCapacityDeduct={}",
                    context.getBatchNo(), context.getTraceId(), machineCode, shiftOrder, node.getTaskId(),
                    specSwitchHours, glueSwitchHours, specSwitchHours.add(glueSwitchHours).multiply(machineSpeed));
        }
    }

    /**
     * 解析班次计划开始时间。
     *
     * @param context 排程上下文
     * @param shiftOrder 班次顺序
     * @return 班次开始时间；缺失或格式非法时返回 null
     */
    private Date resolveShiftStartTime(TmScheduleContext context, Integer shiftOrder) {
        TmShiftTimeWindow window = context.getShiftTimeWindowMap().get(shiftOrder);
        if (window == null || StrUtil.isBlank(window.getPlanStartTime()) || context.getScheduleDate() == null) {
            return null;
        }
        try {
            Date shiftDate = DateUtil.offsetDay(context.getScheduleDate(), (shiftOrder - 1) / 3);
            return DateUtil.parse(DateUtil.formatDate(shiftDate) + " " + window.getPlanStartTime());
        } catch (Exception exception) {
            log.warn("[TM_TASK_TIME] batchNo={}, traceId={}, shiftOrder={}, planStartTime={}, reason=SHIFT_START_PARSE_FAILED",
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
    private TmTaskPredecessor resolvePreviousShiftPredecessor(TmScheduleContext context, String machineCode,
                                                              Integer shiftOrder) {
        for (int previousShiftOrder = shiftOrder - 1; previousShiftOrder >= 1; previousShiftOrder--) {
            ScheduleTaskLinkedList<TmTaskDraft> previousChain = context.getTaskChain(machineCode, previousShiftOrder);
            if (previousChain == null || previousChain.toList().isEmpty()) {
                continue;
            }
            List<ScheduleTaskNode<TmTaskDraft>> nodes = previousChain.toList();
            TmTaskDraft tailTask = nodes.get(nodes.size() - 1).getTask();
            if (tailTask == null) {
                continue;
            }
            TmTaskPredecessor predecessor = new TmTaskPredecessor();
            predecessor.setTreadCode(tailTask.getTreadCode());
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
    private BigDecimal resolveSpecSwitchHours(TmScheduleContext context, TmTaskDraft previousTask,
                                              TmTaskPredecessor externalPredecessor, TmTaskDraft currentTask) {
        String previousTreadCode = previousTask == null
                ? (externalPredecessor == null ? null : externalPredecessor.getTreadCode())
                : previousTask.getTreadCode();
        if (currentTask == null || previousTreadCode == null
                || Objects.equals(previousTreadCode, currentTask.getTreadCode())) {
            return BigDecimal.ZERO;
        }
        return this.resolveSwitchParamHours(context, TmScheduleConstants.PARAM_SPEC_CHANGE_MINUTES);
    }

    /**
     * 计算胶料切换小时数。
     *
     * @param context             排程上下文
     * @param previousTask        当前链内前置任务
     * @param externalPredecessor 上一班或排程日前置任务
     * @param currentTask         当前任务
     * @return 胶料切换小时数
     */
    private BigDecimal resolveGlueSwitchHours(TmScheduleContext context, TmTaskDraft previousTask,
                                              TmTaskPredecessor externalPredecessor, TmTaskDraft currentTask) {
        String previousGlueCode = previousTask == null
                ? (externalPredecessor == null ? null : externalPredecessor.getGlueCode())
                : previousTask.getGlueCode();
        if (currentTask == null || previousGlueCode == null
                || Objects.equals(previousGlueCode, currentTask.getGlueCode())) {
            return BigDecimal.ZERO;
        }
        return this.resolveSwitchParamHours(context, TmScheduleConstants.PARAM_GLUE_CHANGE_MINUTES);
    }

    /**
     * 将切换分钟数参数转换为小时数。
     *
     * @param context   排程上下文
     * @param paramCode 参数编码
     * @return 非负切换小时数，参数缺失或非法时返回0
     */
    private BigDecimal resolveSwitchParamHours(TmScheduleContext context, String paramCode) {
        TmParamValue paramValue = context.getParamMap().get(paramCode);
        String value = paramValue == null ? "0" : StrUtil.blankToDefault(paramValue.getEffectiveValue(), "0");
        try {
            BigDecimal switchMinutes = new BigDecimal(value).max(BigDecimal.ZERO);
            if (switchMinutes.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return switchMinutes.divide(BigDecimal.valueOf(TmScheduleConstants.MINUTES_PER_HOUR),
                    TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            log.warn("[TM_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=INVALID_NUMBER",
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
    private BigDecimal resolveNodeMachineSpeed(ScheduleTaskNode<TmTaskDraft> node) {
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
        long durationSeconds = planQty.multiply(BigDecimal.valueOf(TmScheduleConstants.SECONDS_PER_HOUR))
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
    private void clearChainTimes(ScheduleTaskLinkedList<TmTaskDraft> chain) {
        for (ScheduleTaskNode<TmTaskDraft> node : chain.toList()) {
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
