package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

import java.util.Date;

/**
 * 钢丝圈排程滚动更新任务节点
 *
 * <p>用于滚动更新时维护任务链中每个节点的预计开始/结束时间。</p>
 *
 * @author APS
 */
@Data
public class GsqRollingTaskNode {

    /** 排程记录ID（关联 T_GSQ_SCHEDULE_RESULT.ID） */
    private Long scheduleId;

    /** 班次索引（1~6） */
    private int classIndex;

    /** 机台编号 */
    private String machineCode;

    /** 钢丝圈编码 */
    private String steelRingCode;

    /** 本班计划量 */
    private double planQty;

    /** 本班完成量（已完成的，不可调整） */
    private double finishQty;

    /** 生产顺序（本班次内的生产顺序，从1开始） */
    private int produceOrder;

    /** 规格切换时长（小时），0表示无切换 */
    private double switchTime;

    /** 预计开始时间 */
    private Date startTime;

    /** 预计结束时间 */
    private Date endTime;

    /** 任务状态（0-正常，1-已取消，2-已推迟，3-部分完成推迟） */
    private String taskStatus;

    /** 是否为本班次第一个任务（用于确定起始时间） */
    private boolean firstInShift;

    /** 上一个节点的钢丝圈编码（用于计算切换时长） */
    private String prevSteelRingCode;
}
