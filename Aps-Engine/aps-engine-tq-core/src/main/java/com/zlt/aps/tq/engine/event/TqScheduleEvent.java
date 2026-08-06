package com.zlt.aps.tq.engine.event;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎圈排程事件。
 *
 * <p>对齐胎面 TmScheduleEvent，承载自动滚动完成事件的关键信息，
 * 供下游监听器（通知/审计/下游排程）消费。</p>
 *
 * @author APS
 */
@Data
public class TqScheduleEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    private String factoryCode;

    /** 排程日期。 */
    private Date scheduleDate;

    /** 目标班次。 */
    private Integer targetShiftOrder;

    /** 运行幂等键。 */
    private String runKey;

    /** 状态（SUCCESS/SKIPPED）。 */
    private String status;

    /** 实际调整胎圈数量。 */
    private Integer adjustedBeadCount;

    /** 实际受影响结果行数量。 */
    private Integer affectedResultCount;

    /** 调整前计划量。 */
    private BigDecimal beforePlanQty;

    /** 调整后计划量。 */
    private BigDecimal afterPlanQty;
}
