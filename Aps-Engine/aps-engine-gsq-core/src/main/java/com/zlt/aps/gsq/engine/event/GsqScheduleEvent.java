package com.zlt.aps.gsq.engine.event;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 钢丝圈排程事件。
 *
 * <p>对齐胎圈 TqScheduleEvent，承载钢丝圈排程完成事件的关键信息，
 * 供下游监听器（通知/审计/下游排程）消费。</p>
 *
 * @author APS
 */
@Data
public class GsqScheduleEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    private String factoryCode;

    /** 排程日期。 */
    private Date scheduleDate;

    /** 目标班次。 */
    private Integer targetShiftOrder;

    /** 滚动批次号（运行幂等键）。 */
    private String batchNo;

    /** 状态（SUCCESS/FAILED）。 */
    private String status;

    /** 影响的排程记录数。 */
    private int affectedCount;

    /** 滚动前预计库存。 */
    private double beforeStockQty;

    /** 滚动后预计库存。 */
    private double afterStockQty;
}