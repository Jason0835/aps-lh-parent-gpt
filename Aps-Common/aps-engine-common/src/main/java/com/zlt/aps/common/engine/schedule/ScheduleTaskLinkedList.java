package com.zlt.aps.common.engine.schedule;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用排程双向链表实现。
 *
 * <p>该类负责追加、插入、删除、跨链转移和重新编号。所有会修改链表的方法都会返回
 * 结构化变更结果；该类不写数据库，不直接触发局部重算。</p>
 *
 * @param <T> 链表节点承载的业务任务对象类型
 */
@Getter
public class ScheduleTaskLinkedList<T> implements IScheduleTaskLinkedList<T> {

    /** 头节点 */
    private ScheduleTaskNode<T> head;

    /** 尾节点 */
    private ScheduleTaskNode<T> tail;

    /** 链表节点数量 */
    private int size;

    @Override
    public ScheduleChainChangeResult<T> append(ScheduleTaskNode<T> node, ScheduleOperationContext context) {
        validateNewNode(node);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            node.linkAfter(tail);
            tail = node;
        }
        node.setOwnerList(this);
        size++;
        ScheduleChainChangeResult<T> result = resequence(context);
        result.setOperationType("APPEND");
        return result;
    }

    @Override
    public ScheduleChainChangeResult<T> prepend(ScheduleTaskNode<T> node, ScheduleOperationContext context) {
        validateNewNode(node);
        if (head == null) {
            head = node;
            tail = node;
        } else {
            node.setNextNode(head);
            head.setPreviousNode(node);
            head = node;
        }
        node.setOwnerList(this);
        size++;
        ScheduleChainChangeResult<T> result = resequence(context);
        result.setOperationType("PREPEND");
        return result;
    }
    @Override
    public ScheduleChainChangeResult<T> insertAfter(ScheduleTaskNode<T> anchorNode, ScheduleTaskNode<T> newNode,
                                                    ScheduleOperationContext context) {
        if (anchorNode == null) {
            return append(newNode, context);
        }
        validateNodeInCurrentList(anchorNode);
        validateNewNode(newNode);
        ScheduleTaskNode<T> oldNext = anchorNode.getNextNode();
        newNode.linkAfter(anchorNode);
        if (oldNext == null) {
            tail = newNode;
        }
        newNode.setOwnerList(this);
        size++;
        ScheduleChainChangeResult<T> result = resequence(context);
        result.setOperationType("INSERT_AFTER");
        return result;
    }

    @Override
    public ScheduleChainChangeResult<T> remove(ScheduleTaskNode<T> node, ScheduleOperationContext context) {
        validateNodeInCurrentList(node);
        if (node == head) {
            head = node.getNextNode();
        }
        if (node == tail) {
            tail = node.getPreviousNode();
        }
        node.unlink();
        size--;
        ScheduleChainChangeResult<T> result = resequence(context);
        result.setOperationType("REMOVE");
        result.addRemovedNode(node);
        return result;
    }

    @Override
    public ScheduleChainChangeResult<T> transferTo(ScheduleTaskNode<T> node, IScheduleTaskLinkedList<T> targetList,
                                                   ScheduleTaskNode<T> targetAnchor, ScheduleOperationContext context) {
        if (!(targetList instanceof ScheduleTaskLinkedList)) {
            throw new IllegalArgumentException("目标链表类型不支持转移操作");
        }
        ScheduleTaskLinkedList<T> target = (ScheduleTaskLinkedList<T>) targetList;
        ScheduleChainChangeResult<T> sourceResult = remove(node, context);
        ScheduleChainChangeResult<T> targetResult = target.insertAfter(targetAnchor, node, context);
        ScheduleChainChangeResult<T> result = new ScheduleChainChangeResult<>("TRANSFER", context);
        result.addRemovedNode(node);
        result.addAffectedNodes(sourceResult.getAffectedNodes());
        result.addAffectedNodes(targetResult.getAffectedNodes());
        return result;
    }

    @Override
    public ScheduleChainChangeResult<T> resequence(ScheduleOperationContext context) {
        ScheduleChainChangeResult<T> result = new ScheduleChainChangeResult<>("RESEQUENCE", context);
        int sequence = 1;
        ScheduleTaskNode<T> current = head;
        while (current != null) {
            current.setSequence(sequence++);
            current.setOwnerList(this);
            result.addAffectedNode(current);
            current = current.getNextNode();
        }
        return result;
    }

    /**
     * 按头到尾返回链表快照。
     *
     * <p>返回集合是新的不可变集合，调用方不能通过集合增删影响链表结构；节点对象本身仍是运行态对象。</p>
     *
     * @return 链表节点快照
     */
    public List<ScheduleTaskNode<T>> toList() {
        List<ScheduleTaskNode<T>> nodes = new ArrayList<>();
        ScheduleTaskNode<T> current = head;
        while (current != null) {
            nodes.add(current);
            current = current.getNextNode();
        }
        return Collections.unmodifiableList(nodes);
    }

    /**
     * 按任务标识查找节点。
     *
     * @param taskId 业务任务标识
     * @return 匹配节点；不存在时返回空
     */
    public ScheduleTaskNode<T> findByTaskId(String taskId) {
        ScheduleTaskNode<T> current = head;
        while (current != null) {
            if (taskId != null && taskId.equals(current.getTaskId())) {
                return current;
            }
            current = current.getNextNode();
        }
        return null;
    }

    /**
     * 校验新节点可以入链。
     *
     * @param node 待入链节点
     */
    private void validateNewNode(ScheduleTaskNode<T> node) {
        if (node == null) {
            throw new IllegalArgumentException("任务节点不能为空");
        }
        if (node.getOwnerList() != null || node.getPreviousNode() != null || node.getNextNode() != null) {
            throw new IllegalStateException("任务节点已属于其他任务链");
        }
    }

    /**
     * 校验节点属于当前链表。
     *
     * @param node 待校验节点
     */
    private void validateNodeInCurrentList(ScheduleTaskNode<T> node) {
        if (node == null || node.getOwnerList() != this) {
            throw new IllegalArgumentException("任务节点不在当前任务链中");
        }
    }
}
