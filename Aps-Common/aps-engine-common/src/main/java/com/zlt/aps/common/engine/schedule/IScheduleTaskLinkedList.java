package com.zlt.aps.common.engine.schedule;

/**
 * 通用排程双向链表操作接口。
 *
 * <p>该接口承载自动排程、插单、删除、转机台和调量等场景的基础任务链修改能力。
 * 所有修改方法必须返回结构化变更结果，便于后续日志、解释快照和局部重算使用。</p>
 *
 * @param <T> 链表节点承载的业务任务对象类型
 */
public interface IScheduleTaskLinkedList<T> {

    /**
     * 将节点追加到链尾。
     *
     * @param node    待追加节点，不能已经属于其他链表
     * @param context 操作上下文，包含操作人、原因和追踪号
     * @return 链表变更结果，包含受影响节点和新顺序
     */
    ScheduleChainChangeResult<T> append(ScheduleTaskNode<T> node, ScheduleOperationContext context);

    /**
     * 将新节点插入指定节点之后。
     *
     * @param anchorNode 锚点节点；为空时按追加链尾处理
     * @param newNode    待插入节点，不能已经属于其他链表
     * @param context    操作上下文，包含操作人、原因和追踪号
     * @return 链表变更结果，包含受影响节点和新顺序
     */
    ScheduleChainChangeResult<T> insertAfter(ScheduleTaskNode<T> anchorNode, ScheduleTaskNode<T> newNode,
                                             ScheduleOperationContext context);

    /**
     * 从当前链表摘除指定节点。
     *
     * @param node    待删除节点，必须属于当前链表
     * @param context 操作上下文，包含操作人、原因和追踪号
     * @return 链表变更结果，包含删除节点、受影响节点和新顺序
     */
    ScheduleChainChangeResult<T> remove(ScheduleTaskNode<T> node, ScheduleOperationContext context);

    /**
     * 将节点从当前链表转移到目标链表。
     *
     * @param node         待转移节点，必须属于当前链表
     * @param targetList   目标链表，不能为空
     * @param targetAnchor 目标锚点节点；为空时追加到目标链尾
     * @param context      操作上下文，包含操作人、原因和追踪号
     * @return 链表变更结果，包含源链和目标链受影响节点
     */
    ScheduleChainChangeResult<T> transferTo(ScheduleTaskNode<T> node, IScheduleTaskLinkedList<T> targetList,
                                            ScheduleTaskNode<T> targetAnchor, ScheduleOperationContext context);

    /**
     * 从头节点开始重新编号。
     *
     * @param context 操作上下文，包含操作人、原因和追踪号
     * @return 链表变更结果，包含全部重新编号节点
     */
    ScheduleChainChangeResult<T> resequence(ScheduleOperationContext context);
}
