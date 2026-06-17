package com.zlt.aps.common.engine.schedule;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 通用排程双向链表节点。
 *
 * <p>该类保存任务对象、前驱节点、后继节点、机台、日期、班次、顺序、计划量和预计时间。
 * 它属于通用排程能力，不引用胎面或胎侧专用类型，允许后续胎侧复用。</p>
 *
 * @param <T> 节点承载的业务任务对象类型
 */
@Data
public class ScheduleTaskNode<T> implements IScheduleTaskNode<T> {

    /** 业务任务标识，用于链表内查找和日志输出 */
    private String taskId;

    /** 节点承载的业务任务对象 */
    private T task;

    /** 前驱节点 */
    private ScheduleTaskNode<T> previousNode;

    /** 后继节点 */
    private ScheduleTaskNode<T> nextNode;

    /** 当前节点所属链表 */
    private ScheduleTaskLinkedList<T> ownerList;

    /** 机台编码 */
    private String machineCode;

    /** 排程日期 */
    private LocalDate scheduleDate;

    /** 班次编码 */
    private String shiftCode;

    /** 班次顺序 */
    private Integer shiftOrder;

    /** 任务顺序 */
    private Integer sequence;

    /** 计划量 */
    private BigDecimal planQty;

    /** 预计开始时间 */
    private Date startTime;

    /** 预计结束时间 */
    private Date endTime;

    /**
     * 创建通用任务节点。
     *
     * @param taskId       业务任务标识
     * @param task         业务任务对象
     * @param machineCode  机台编码
     * @param scheduleDate 排程日期
     * @param shiftCode    班次编码
     * @param shiftOrder   班次顺序
     * @param planQty      计划量
     */
    public ScheduleTaskNode(String taskId, T task, String machineCode, LocalDate scheduleDate, String shiftCode,
                            Integer shiftOrder, BigDecimal planQty) {
        this.taskId = taskId;
        this.task = task;
        this.machineCode = machineCode;
        this.scheduleDate = scheduleDate;
        this.shiftCode = shiftCode;
        this.shiftOrder = shiftOrder;
        this.planQty = planQty;
    }

    /**
     * 将当前节点连接到指定节点之后。
     *
     * <p>该方法只处理指针关系，不重新编号，不写日志。调用方必须保证前驱节点所属链表合法。</p>
     *
     * @param previousNode 前驱节点
     */
    public void linkAfter(ScheduleTaskNode<T> previousNode) {
        if (previousNode == null) {
            return;
        }
        ScheduleTaskNode<T> oldNext = previousNode.getNextNode();
        this.previousNode = previousNode;
        this.nextNode = oldNext;
        previousNode.setNextNode(this);
        if (oldNext != null) {
            oldNext.setPreviousNode(this);
        }
    }

    /**
     * 从当前链表中摘除节点。
     *
     * <p>该方法会修改前驱和后继节点指针，并清空当前节点的前驱、后继和所属链表；
     * 不重新编号，不触发局部重算。</p>
     */
    public void unlink() {
        if (previousNode != null) {
            previousNode.setNextNode(nextNode);
        }
        if (nextNode != null) {
            nextNode.setPreviousNode(previousNode);
        }
        previousNode = null;
        nextNode = null;
        ownerList = null;
    }
}
