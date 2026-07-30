package com.zlt.aps.tq.engine.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTaskNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎圈任务链排程服务。
 *
 * <p>Phase 5 重构新增：对齐胎侧 {@code TcTaskChainScheduleService}，统一处理自动排程追加、
 * 人工插单、删除、转机台和调量对运行态任务链的修改。</p>
 *
 * <p>与胎侧的差异：</p>
 * <ul>
 *   <li>胎侧任务对象为 {@code TcTaskDraft}（含胶料/口型/工装等复杂字段），胎圈任务对象为 {@code TqTaskNode}（仅含胎圈/机台/班次）</li>
 *   <li>胎侧按"机台+日期+班次顺序"分组链表，胎圈按"机台+日期"分组链表（胎圈一个机台一个链，链内按班次顺序串联）</li>
 *   <li>胎侧有时间窗口和机台速度可重算开始/结束时间，胎圈无班次时间窗口配置，时间重算退化为只重排顺序</li>
 *   <li>胎侧前置任务快照 {@code TcTaskPredecessor} 用于跨日连续性判断，胎圈当前不引入跨日快照</li>
 * </ul>
 *
 * <p>该服务只维护内存态链表，不写数据库。所有修改方法返回 {@link ScheduleChainChangeResult}，
 * 便于上层日志、解释快照和后续局部重算使用。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class TqTaskChainScheduleService {

    /** 自动追加任务链操作编码 */
    public static final String CHAIN_OPERATION_AUTO_APPEND = "AUTO_APPEND";

    /** 自动前置任务链操作编码 */
    public static final String CHAIN_OPERATION_AUTO_PREPEND = "AUTO_PREPEND";

    /** 人工插单任务链操作编码 */
    public static final String CHAIN_OPERATION_MANUAL_INSERT = "MANUAL_INSERT";

    /** 人工删除任务链操作编码 */
    public static final String CHAIN_OPERATION_MANUAL_DELETE = "MANUAL_DELETE";

    /** 人工转机任务链操作编码 */
    public static final String CHAIN_OPERATION_MANUAL_TRANSFER = "MANUAL_TRANSFER";

    /** 人工调量任务链操作编码 */
    public static final String CHAIN_OPERATION_CHANGE_QTY = "CHANGE_QTY";

    /** 胎圈宽表支持的最大班次序号 */
    public static final int TQ_MAX_SHIFT_ORDER = 6;

    /**
     * 自动排程追加任务到机台班次链尾。
     *
     * <p>使用场景：S3 机台分配阶段，候选机台筛选完成后，将选中机台对应的任务节点追加到链尾。
     * 该方法不修改排程记录的机台字段（由调用方 {@code TqMachineAssignHandler} 在调用前完成），
     * 只负责任务链结构维护和重新编号。</p>
     *
     * @param task         待排任务节点（承载胎圈/机台/班次/计划量等数据）
     * @param machineCode  选中候选机台编码
     * @param context      胎圈排程上下文
     * @return 链表变更结果
     * @throws ServiceException 任务或上下文为空、机台编码为空时抛出
     */
    public ScheduleChainChangeResult<TqTaskNode> appendAutoTask(TqTaskNode task, String machineCode,
                                                                 TqScheduleContext context) {
        validateTaskAndContext(task, context);
        if (StrUtil.isBlank(machineCode)) {
            throw new ServiceException("胎圈任务链追加失败：机台编码为空");
        }
        Integer shiftOrder = resolveShiftOrder(task);
        ScheduleTaskLinkedList<TqTaskNode> chain = context.getTaskChainGroup()
                .getOrCreate(machineCode, toLocalDate(context));
        ScheduleTaskNode<TqTaskNode> node = toNode(task, machineCode, shiftOrder, context);
        ScheduleChainChangeResult<TqTaskNode> result = chain.append(node,
                operationContext(context, CHAIN_OPERATION_AUTO_APPEND));
        context.registerTaskNode(node.getTaskId(), node);
        this.resequenceAndLog(context, chain, machineCode, CHAIN_OPERATION_AUTO_APPEND, node.getTaskId());
        return result;
    }

    /**
     * 自动排程前插任务到机台班次链头。
     *
     * <p>使用场景：顺延量新建任务时抢占目标机台的第一优先顺序，避免后续普通任务先占用产能。</p>
     *
     * @param task        待排任务节点
     * @param machineCode 选中候选机台编码
     * @param context     胎圈排程上下文
     * @return 链表变更结果
     * @throws ServiceException 任务或上下文为空、机台编码为空时抛出
     */
    public ScheduleChainChangeResult<TqTaskNode> prependAutoTask(TqTaskNode task, String machineCode,
                                                                  TqScheduleContext context) {
        validateTaskAndContext(task, context);
        if (StrUtil.isBlank(machineCode)) {
            throw new ServiceException("胎圈任务链前插失败：机台编码为空");
        }
        Integer shiftOrder = resolveShiftOrder(task);
        ScheduleTaskLinkedList<TqTaskNode> chain = context.getTaskChainGroup()
                .getOrCreate(machineCode, toLocalDate(context));
        ScheduleTaskNode<TqTaskNode> node = toNode(task, machineCode, shiftOrder, context);
        ScheduleChainChangeResult<TqTaskNode> result = chain.prepend(node,
                operationContext(context, CHAIN_OPERATION_AUTO_PREPEND));
        context.registerTaskNode(node.getTaskId(), node);
        this.resequenceAndLog(context, chain, machineCode, CHAIN_OPERATION_AUTO_PREPEND, node.getTaskId());
        return result;
    }

    /**
     * 人工插单：将任务插入到指定锚点节点之后。
     *
     * <p>锚点节点为空时按追加链尾处理。锚点节点必须属于目标链表，
     * 否则按追加链尾处理以避免跨链污染。</p>
     *
     * @param task            插单任务节点
     * @param machineCode     目标机台编码
     * @param anchorTaskId    锚点任务ID（对应 {@code TqTaskNode.businessKey}），为空时追加链尾
     * @param context         胎圈排程上下文
     * @return 链表变更结果
     * @throws ServiceException 任务或上下文为空、机台编码为空时抛出
     */
    public ScheduleChainChangeResult<TqTaskNode> insertManualTask(TqTaskNode task, String machineCode,
                                                                   String anchorTaskId, TqScheduleContext context) {
        validateTaskAndContext(task, context);
        if (StrUtil.isBlank(machineCode)) {
            throw new ServiceException("胎圈任务链插单失败：机台编码为空");
        }
        Integer shiftOrder = resolveShiftOrder(task);
        ScheduleTaskLinkedList<TqTaskNode> chain = context.getTaskChainGroup()
                .getOrCreate(machineCode, toLocalDate(context));
        ScheduleTaskNode<TqTaskNode> anchor = findNode(anchorTaskId, context);
        if (anchor == null || anchor.getOwnerList() != chain) {
            anchor = chain.findByTaskId(anchorTaskId);
        }
        ScheduleTaskNode<TqTaskNode> node = toNode(task, machineCode, shiftOrder, context);
        ScheduleChainChangeResult<TqTaskNode> result = chain.insertAfter(anchor, node,
                operationContext(context, CHAIN_OPERATION_MANUAL_INSERT));
        context.registerTaskNode(node.getTaskId(), node);
        this.resequenceAndLog(context, chain, machineCode, CHAIN_OPERATION_MANUAL_INSERT, node.getTaskId());
        return result;
    }

    /**
     * 删除任务：从所属链表摘除目标节点并重排后续顺序。
     *
     * <p>查找范围为上下文内已加载的机台任务链集合。找不到目标节点时抛出异常。</p>
     *
     * @param taskId  任务标识（对应 {@code TqTaskNode.businessKey}）
     * @param context 胎圈排程上下文
     * @return 链表变更结果，包含被删除节点和受影响节点
     * @throws ServiceException 任务ID为空、上下文为空或未找到目标节点时抛出
     */
    public ScheduleChainChangeResult<TqTaskNode> removeTask(String taskId, TqScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException("胎圈任务链删除失败：任务ID为空");
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException("胎圈任务链删除失败：上下文为空");
        }
        ScheduleTaskNode<TqTaskNode> indexedNode = findNode(taskId, context);
        if (indexedNode != null && indexedNode.getOwnerList() instanceof ScheduleTaskLinkedList) {
            ScheduleTaskLinkedList<TqTaskNode> ownerChain = (ScheduleTaskLinkedList<TqTaskNode>) indexedNode.getOwnerList();
            String machineCode = indexedNode.getMachineCode();
            ScheduleChainChangeResult<TqTaskNode> result = ownerChain.remove(indexedNode,
                    operationContext(context, CHAIN_OPERATION_MANUAL_DELETE));
            context.removeTaskNode(taskId);
            this.resequenceAndLog(context, ownerChain, machineCode, CHAIN_OPERATION_MANUAL_DELETE, taskId);
            return result;
        }
        throw new ServiceException("胎圈任务链删除失败：未找到任务节点:" + taskId);
    }

    /**
     * 转机台：将任务从原机台链表摘除，插入目标机台链表的指定位置。
     *
     * <p>原链和目标链分别触发重新编号。当前不处理发布状态回退，
     * 由上层操作门面统一处理。</p>
     *
     * @param taskId            任务标识
     * @param targetMachineCode 目标机台编码
     * @param anchorTaskId      目标锚点任务ID，为空时追加到目标链尾
     * @param context           胎圈排程上下文
     * @return 链表变更结果，包含原链和目标链的受影响节点
     * @throws ServiceException 参数缺失、任务未找到时抛出
     */
    public ScheduleChainChangeResult<TqTaskNode> transferMachine(String taskId, String targetMachineCode,
                                                                  String anchorTaskId, TqScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException("胎圈任务链转机台失败：任务ID为空");
        }
        if (StrUtil.isBlank(targetMachineCode)) {
            throw new ServiceException("胎圈任务链转机台失败：目标机台编码为空");
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException("胎圈任务链转机台失败：上下文为空");
        }
        LocalDate localDate = toLocalDate(context);
        ScheduleOperationContext opCtx = operationContext(context, CHAIN_OPERATION_MANUAL_TRANSFER);

        // 在原链中查找目标节点
        ScheduleTaskNode<TqTaskNode> sourceNode = findNode(taskId, context);
        ScheduleTaskLinkedList<TqTaskNode> sourceChain = sourceNode != null
                && sourceNode.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<TqTaskNode>) sourceNode.getOwnerList() : null;
        if (sourceNode == null || sourceChain == null) {
            throw new ServiceException("胎圈任务链转机台失败：未找到任务节点:" + taskId);
        }

        // 获取目标链表
        ScheduleTaskLinkedList<TqTaskNode> targetChain = context.getTaskChainGroup()
                .getOrCreate(targetMachineCode, localDate);

        // 更新节点的机台定位
        String sourceMachineCode = sourceNode.getMachineCode();
        sourceNode.setMachineCode(targetMachineCode);
        ScheduleTaskNode<TqTaskNode> anchorNode = targetChain.findByTaskId(anchorTaskId);

        // 执行跨链转移
        ScheduleChainChangeResult<TqTaskNode> result = sourceChain.transferTo(sourceNode, targetChain, anchorNode, opCtx);
        context.registerTaskNode(taskId, sourceNode);
        this.resequenceAndLog(context, sourceChain, sourceMachineCode, CHAIN_OPERATION_MANUAL_TRANSFER, taskId);
        this.resequenceAndLog(context, targetChain, targetMachineCode, CHAIN_OPERATION_MANUAL_TRANSFER, taskId);
        return result;
    }

    /**
     * 调整计划量：更新节点和任务节点的计划量，触发重新编号。
     *
     * <p>当前版本只更新计划量数值，不处理跨班归属变化，
     * 由上层操作门面在必要时触发局部重算。</p>
     *
     * @param taskId      任务标识
     * @param newPlanQty  新计划量
     * @param context     胎圈排程上下文
     * @return 链表变更结果
     * @throws ServiceException 参数缺失、任务未找到时抛出
     */
    public ScheduleChainChangeResult<TqTaskNode> changeQty(String taskId, BigDecimal newPlanQty,
                                                            TqScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            throw new ServiceException("胎圈任务链调量失败：任务ID为空");
        }
        if (newPlanQty == null) {
            throw new ServiceException("胎圈任务链调量失败：新计划量为空");
        }
        if (context == null || context.getTaskChainGroup() == null) {
            throw new ServiceException("胎圈任务链调量失败：上下文为空");
        }

        ScheduleTaskNode<TqTaskNode> targetNode = findNode(taskId, context);
        ScheduleTaskLinkedList<TqTaskNode> targetChain = targetNode != null
                && targetNode.getOwnerList() instanceof ScheduleTaskLinkedList
                ? (ScheduleTaskLinkedList<TqTaskNode>) targetNode.getOwnerList() : null;
        if (targetNode == null || targetChain == null) {
            throw new ServiceException("胎圈任务链调量失败：未找到任务节点:" + taskId);
        }

        // 更新节点计划量和任务节点计划量
        targetNode.setPlanQty(newPlanQty);
        if (targetNode.getTask() != null) {
            targetNode.getTask().setPlanQty(newPlanQty.doubleValue());
        }

        // 触发重新编号（胎圈无班次时间窗口，只重排顺序，不重算开始/结束时间）
        ScheduleChainChangeResult<TqTaskNode> result = targetChain.resequence(
                operationContext(context, CHAIN_OPERATION_CHANGE_QTY));
        this.logChainState(context, targetChain, CHAIN_OPERATION_CHANGE_QTY,
                targetNode.getMachineCode(), taskId);
        return result;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验任务和上下文非空。
     */
    private void validateTaskAndContext(TqTaskNode task, TqScheduleContext context) {
        if (task == null) {
            throw new ServiceException("胎圈任务链操作失败：任务节点为空");
        }
        if (context == null) {
            throw new ServiceException("胎圈任务链操作失败：上下文为空");
        }
    }

    /**
     * 解析任务节点的班次顺序。
     */
    private Integer resolveShiftOrder(TqTaskNode task) {
        return task.getClassIndex();
    }

    /**
     * 将胎圈任务节点转换为通用排程链表节点。
     *
     * <p>业务键规则：beadCode|machineCode|classIndex，确保同一规格同机台同班次任务键唯一。</p>
     */
    private ScheduleTaskNode<TqTaskNode> toNode(TqTaskNode task, String machineCode, Integer shiftOrder,
                                                 TqScheduleContext context) {
        String businessKey = buildBusinessKey(task, machineCode, shiftOrder);
        BigDecimal planQty = BigDecimal.valueOf(task.getPlanQty());
        LocalDate localDate = toLocalDate(context);
        String shiftCode = "CLASS" + shiftOrder;
        ScheduleTaskNode<TqTaskNode> node = new ScheduleTaskNode<>(businessKey, task, machineCode,
                localDate, shiftCode, shiftOrder, planQty);
        node.setSequence(task.getProduceOrder());
        return node;
    }

    /**
     * 构建任务业务键：beadCode|machineCode|classIndex。
     */
    private String buildBusinessKey(TqTaskNode task, String machineCode, Integer shiftOrder) {
        return safe(task.getBeadCode()) + "|" + safe(machineCode) + "|" + safe(shiftOrder);
    }

    /**
     * 空值安全的字符串转换。
     */
    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 将排程日期转换为 LocalDate。
     *
     * <p>胎圈 {@code TqScheduleContext.scheduleDate} 为 {@code yyyy-MM-dd} 字符串格式，
     * 这里解析为 {@link LocalDate}。空值时回退到当前日期，避免任务链键空指针。</p>
     */
    private LocalDate toLocalDate(TqScheduleContext context) {
        String scheduleDate = context.getScheduleDate();
        if (StrUtil.isBlank(scheduleDate)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(scheduleDate);
        } catch (Exception e) {
            log.warn("[TQ_TASK_CHAIN] 排程日期格式异常，回退到当前日期: scheduleDate={}", scheduleDate, e);
            return LocalDate.now();
        }
    }

    /**
     * 构建操作上下文。
     */
    private ScheduleOperationContext operationContext(TqScheduleContext context, String operation) {
        String operator = context.getOperator() == null ? "SYSTEM" : context.getOperator();
        return new ScheduleOperationContext(operator, "胎圈任务链操作:" + operation, context.getTraceId());
    }

    /**
     * 在上下文节点索引和已加载链表中查找目标节点。
     */
    private ScheduleTaskNode<TqTaskNode> findNode(String taskId, TqScheduleContext context) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        ScheduleTaskNode<TqTaskNode> indexed = context.getTaskNode(taskId);
        if (indexed != null) {
            return indexed;
        }
        // 兜底：遍历所有链表
        for (ScheduleTaskLinkedList<TqTaskNode> chain : context.getTaskChainGroup().values()) {
            ScheduleTaskNode<TqTaskNode> node = chain.findByTaskId(taskId);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * 触发重新编号并打印日志。
     */
    private void resequenceAndLog(TqScheduleContext context, ScheduleTaskLinkedList<TqTaskNode> chain,
                                 String machineCode, String operation, String changedTaskId) {
        if (chain == null) {
            return;
        }
        this.logChainState(context, chain, operation, machineCode, changedTaskId);
    }

    /**
     * 打印任务链变更后的顺序摘要。
     */
    private void logChainState(TqScheduleContext context, ScheduleTaskLinkedList<TqTaskNode> chain,
                               String operation, String machineCode, String changedTaskId) {
        String chainOrder = chain == null ? "" : chain.toList().stream()
                .map(ScheduleTaskNode::getTaskId)
                .collect(Collectors.joining(","));
        log.info("[TQ_TASK_CHAIN] batchNo={}, traceId={}, scheduleDate={}, operation={}, machineCode={}, changedTaskId={}, chainSize={}, chainOrder={}",
                context == null ? null : context.getBatchNo(),
                context == null ? null : context.getTraceId(),
                context == null ? null : context.getScheduleDate(),
                operation, machineCode, changedTaskId, chain == null ? 0 : chain.getSize(), chainOrder);
    }

    /**
     * 兼容旧 {@link TqScheduleResultVo} 的任务节点构建。
     *
     * <p>用于将排程记录中按班次展开为任务节点并追加到任务链。
     * 该方法不修改排程记录，只构建任务节点并调用 {@link #appendAutoTask}。</p>
     *
     * @param scheduleVo 排程记录
     * @param classIdx   班次索引（1~6）
     * @param planQty    本班计划量
     * @param machineCode 机台编码
     * @param context    排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TqTaskNode> appendScheduleVo(TqScheduleResultVo scheduleVo, int classIdx,
                                                                   double planQty, String machineCode,
                                                                   TqScheduleContext context) {
        TqTaskNode node = new TqTaskNode();
        node.setClassIndex(classIdx);
        node.setMachineCode(machineCode);
        node.setBeadCode(scheduleVo.getBeadCode());
        node.setScheduleId(scheduleVo.getId());
        node.setPlanQty(planQty);
        return appendAutoTask(node, machineCode, context);
    }

    /**
     * 批量加载已排程结果到任务链。
     *
     * <p>用于自动排程构建阶段，将 {@code context.scheduleList} 中已分配机台的排程结果按
     * 机台分组、班次顺序串联加载到 {@code taskChainGroup}。</p>
     *
     * <p>调用前应清空 {@code taskChainGroup} 和 {@code taskNodeIndex}，避免重复加载。</p>
     *
     * @param context 排程上下文
     */
    public void loadFromScheduleList(TqScheduleContext context) {
        if (context == null || context.getTaskChainGroup() == null) {
            return;
        }
        List<TqScheduleResultVo> scheduleList = context.getScheduleList();
        if (scheduleList == null || scheduleList.isEmpty()) {
            return;
        }
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            if (scheduleVo == null || StrUtil.isBlank(scheduleVo.getMachineCode())) {
                continue;
            }
            String machineCode = scheduleVo.getMachineCode();
            for (int classIdx = 1; classIdx <= TQ_MAX_SHIFT_ORDER; classIdx++) {
                double planQty = getClassPlanQty(scheduleVo, classIdx);
                if (planQty <= 0) {
                    continue;
                }
                appendScheduleVo(scheduleVo, classIdx, planQty, machineCode, context);
            }
        }
        log.info("[TQ_TASK_CHAIN] batchNo={}, 任务链批量加载完成, 机台数={}",
                context.getBatchNo(), context.getTaskChainGroup().values().size());
    }

    /**
     * 读取排程记录指定班次的计划量。
     *
     * <p>与 {@link TqMachineAssignHandler#getClassPlanQty} 对齐，
     * 避免循环依赖，此处复制必要逻辑。</p>
     *
     * @param scheduleVo 排程记录
     * @param classIdx   班次索引（1~6）
     * @return 计划量；班次索引非法或计划量为空时返回 0
     */
    private double getClassPlanQty(TqScheduleResultVo scheduleVo, int classIdx) {
        switch (classIdx) {
            case 1: return scheduleVo.getClass1PlanQty() == null ? 0D : scheduleVo.getClass1PlanQty();
            case 2: return scheduleVo.getClass2PlanQty() == null ? 0D : scheduleVo.getClass2PlanQty();
            case 3: return scheduleVo.getClass3PlanQty() == null ? 0D : scheduleVo.getClass3PlanQty();
            case 4: return scheduleVo.getClass4PlanQty() == null ? 0D : scheduleVo.getClass4PlanQty();
            case 5: return scheduleVo.getClass5PlanQty() == null ? 0D : scheduleVo.getClass5PlanQty();
            case 6: return scheduleVo.getClass6PlanQty() == null ? 0D : scheduleVo.getClass6PlanQty();
            default: return 0D;
        }
    }
}
