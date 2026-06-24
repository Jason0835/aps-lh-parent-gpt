package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 胎圈排程任务链节点。
 *
 * <p>任务链是一个有序的LinkedList，用于解决班次排产顺序和赋值问题。</p>
 * <p>每个节点代表一个机台在一个班次的生产任务。</p>
 */
@Data
public class TqTaskNode {

    /**
     * 班次索引（1~6）
     */
    private int classIndex;

    /**
     * 机台编号
     */
    private String machineCode;

    /**
     * 胎圈编码
     */
    private String beadCode;

    /**
     * 排产记录ID（关联TqScheduleResultVo）
     */
    private Long scheduleId;

    /**
     * 本班计划量
     */
    private double planQty;

    /**
     * 本班开始预计库存
     */
    private double startStockQty;

    /**
     * 本班结束预计库存 = 开始库存 + 本班产出 - 本班消耗
     */
    private double endStockQty;

    /**
     * 库存保证班数（本班结束后）
     */
    private double guaranteeShifts;

    /**
     * 生产顺序（本班次内的生产顺序）
     */
    private int produceOrder;

    /**
     * 规格切换时长（小时），0表示无切换
     */
    private double switchTime;

    /**
     * 本班成型消耗量（胎圈消耗 = 成型计划 × 系数）
     */
    private double cxConsumeQty;

    /**
     * 本班实际有效生产时长（小时）= 班次时长 - 切换时长
     */
    private double effectiveHours;
}
