package com.zlt.aps.common.engine.schedule;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 排程任务链变更结果。
 *
 * <p>用于描述追加、插入、删除、转机台和重新编号后的结构化结果。日志、解释快照和
 * 局部重算可以读取该对象判断受影响节点范围。</p>
 *
 * @param <T> 链表节点承载的业务任务对象类型
 */
@Data
public class ScheduleChainChangeResult<T> {

    /** 操作类型 */
    private String operationType;

    /** 追踪标识 */
    private String traceId;

    /** 受影响节点 */
    private List<ScheduleTaskNode<T>> affectedNodes = new ArrayList<>();

    /** 被删除或转移前摘除的节点 */
    private List<ScheduleTaskNode<T>> removedNodes = new ArrayList<>();

    /**
     * 创建链表变更结果。
     *
     * @param operationType 操作类型
     * @param context       操作上下文
     */
    public ScheduleChainChangeResult(String operationType, ScheduleOperationContext context) {
        this.operationType = operationType;
        this.traceId = context == null ? null : context.getTraceId();
    }

    /**
     * 追加受影响节点。
     *
     * @param node 受影响节点
     */
    public void addAffectedNode(ScheduleTaskNode<T> node) {
        if (node != null) {
            affectedNodes.add(node);
        }
    }

    /**
     * 批量追加受影响节点。
     *
     * @param nodes 受影响节点集合
     */
    public void addAffectedNodes(List<ScheduleTaskNode<T>> nodes) {
        if (nodes != null) {
            affectedNodes.addAll(nodes);
        }
    }

    /**
     * 追加被删除节点。
     *
     * @param node 被删除或摘除的节点
     */
    public void addRemovedNode(ScheduleTaskNode<T> node) {
        if (node != null) {
            removedNodes.add(node);
        }
    }

    public List<ScheduleTaskNode<T>> getAffectedNodes() {
        return Collections.unmodifiableList(affectedNodes);
    }

    public void setAffectedNodes(List<ScheduleTaskNode<T>> affectedNodes) {
        this.affectedNodes = affectedNodes == null ? new ArrayList<>() : new ArrayList<>(affectedNodes);
    }

    public List<ScheduleTaskNode<T>> getRemovedNodes() {
        return Collections.unmodifiableList(removedNodes);
    }

    public void setRemovedNodes(List<ScheduleTaskNode<T>> removedNodes) {
        this.removedNodes = removedNodes == null ? new ArrayList<>() : new ArrayList<>(removedNodes);
    }
}
