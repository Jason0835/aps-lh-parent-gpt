package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

import java.util.Date;

/**
 * 钢丝圈排程滚动更新变更明细
 *
 * <p>记录单个排程记录在滚动更新中的变更情况。</p>
 * <p>MVP阶段仅记录主表日志，明细表后续补充。</p>
 *
 * @author APS
 */
@Data
public class GsqRollingChangeDetail {

    /** 排程记录ID */
    private Long scheduleId;

    /** 班次索引 */
    private int classIndex;

    /** 机台编号 */
    private String machineCode;

    /** 钢丝圈编码 */
    private String steelRingCode;

    /** 变更类型：1-新增，2-删除，3-更新（时间/顺序/数量） */
    private String changeType;

    /** 原生产顺序 */
    private Integer beforeOrder;

    /** 新生产顺序 */
    private Integer afterOrder;

    /** 原预计开始时间 */
    private Date beforeStartTime;

    /** 新预计开始时间 */
    private Date afterStartTime;

    /** 原预计结束时间 */
    private Date beforeEndTime;

    /** 新预计结束时间 */
    private Date afterEndTime;

    /** 原计划量 */
    private Double beforePlanQty;

    /** 新计划量 */
    private Double afterPlanQty;

    /** 原任务状态 */
    private String beforeStatus;

    /** 新任务状态 */
    private String afterStatus;

    /** 变更原因 */
    private String changeReason;
}
