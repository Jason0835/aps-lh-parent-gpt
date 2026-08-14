package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 班次开始前已形成或预计形成的斜裁入库记录。
 */
@Data
@Builder
public class Cd15InboundRecord {

    /** 工单或任务唯一标识，用于实际与计划互斥。 */
    private String taskKey;
    /** 是否为MES实际入库。 */
    private boolean actual;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 分配库排编码。 */
    private String laneCode;
    /** 库排绑定机台编码。 */
    private String machineCode;
    /** 入库车辆数。 */
    private int vehicleCount;
    /** 精确入库数量，单位米；为空时资源计算仍可按车辆数处理。 */
    private BigDecimal inboundQuantity;
    /** 实际或预计入库时间，用于判断能否进入当前班次资源快照。 */
    private LocalDateTime inboundTime;
}
