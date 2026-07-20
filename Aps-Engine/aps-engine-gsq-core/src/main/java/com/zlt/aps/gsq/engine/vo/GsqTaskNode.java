package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈排程任务链节点。
 *
 * <p>任务链是一个有序的LinkedList，用于解决班次排产顺序和赋值问题。</p>
 * <p>每个节点代表一个机台在一个班次的生产任务，仅在内存中维护，不持久化。</p>
 *
 * <p>节点字段说明：</p>
 * <ul>
 *   <li>classIndex: 班次索引(1~6)，对应钢丝圈1~6班</li>
 *   <li>machineCode: 机台编号</li>
 *   <li>wireCoilCode: 钢丝盘号（换盘判断依据）</li>
 *   <li>planQty: 本班计划量</li>
 *   <li>startStockQty: 本班开始预计库存</li>
 *   <li>endStockQty: 本班结束预计库存 = 开始库存 + 本班产出 - 本班消耗</li>
 *   <li>tqConsumeQty: 本班胎圈消耗量（胎圈消耗 = 胎圈计划量 × BOM × 系数）</li>
 *   <li>guaranteeShifts: 库存保证班数（本班结束后）</li>
 *   <li>switchTime: 切换时长（小时），取三种切换最大值</li>
 *   <li>switchType: 切换类型 SPEC/INCH/WIRE</li>
 *   <li>effectiveHours: 本班实际有效生产时长（小时）= 班次时长 - 切换时长</li>
 *   <li>freshExpired: 保鲜期是否超期（true=超期）</li>
 * </ul>
 *
 * @author APS
 */
@Data
public class GsqTaskNode {

    /** 班次索引（1~6） */
    private int classIndex;

    /** 机台编号 */
    private String machineCode;

    /** 钢丝圈编码 */
    private String steelRingCode;

    /** 钢丝盘号（换盘判断依据） */
    private String wireCoilCode;

    /** 排产记录ID（关联GsqScheduleResultVo） */
    private Long scheduleId;

    /** 本班计划量 */
    private double planQty;

    /** 本班开始预计库存 */
    private double startStockQty;

    /** 本班结束预计库存 = 开始库存 + 本班产出 - 本班消耗 */
    private double endStockQty;

    /** 本班胎圈消耗量（胎圈消耗 = 胎圈计划量 × BOM × 系数） */
    private double tqConsumeQty;

    /** 库存保证班数（本班结束后） */
    private double guaranteeShifts;

    /** 生产顺序（本班次内的生产顺序） */
    private int produceOrder;

    /** 规格切换时长（小时），0表示无切换 */
    private double switchTime;

    /** 切换类型：SPEC=规格切换，INCH=切英寸，WIRE=换盘，取三种最大值 */
    private String switchType;

    /** 本班实际有效生产时长（小时）= 班次时长 - 切换时长 */
    private double effectiveHours;

    /** 保鲜期是否超期（true=超期，产出时间到胎圈消耗时间 > 72h） */
    private boolean freshExpired;
}
