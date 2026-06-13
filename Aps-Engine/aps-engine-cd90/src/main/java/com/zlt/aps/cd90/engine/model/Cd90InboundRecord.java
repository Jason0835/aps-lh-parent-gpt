package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 班次开始前已形成或预计形成的直裁入库记录。
 */
@Data
@Builder
public class Cd90InboundRecord {

    /** 工单或任务唯一标识，用于实际与计划互斥。 */
    private String taskKey;
    /** 是否为MES实际入库。 */
    private boolean actual;
    /** 帘布代码。 */
    private String clothCode;
    /** 分配库排编码。 */
    private String laneCode;
    /** 入库车辆数。 */
    private int vehicleCount;
}
