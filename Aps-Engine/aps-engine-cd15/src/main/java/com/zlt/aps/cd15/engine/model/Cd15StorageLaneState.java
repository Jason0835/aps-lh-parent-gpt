package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 当前班次资源快照中的单个库排状态。
 */
@Data
@Builder
public class Cd15StorageLaneState {

    /** 库排编码。 */
    private String laneCode;
    /** 固定绑定的斜裁机台编码。 */
    private String machineCode;
    /** 当前定向钢带代码；车数为0时仍保留原钢带归属。 */
    private String steelStripCode;
    /** 当前占用车数。 */
    private int vehicleCount;
    /** 最大可容纳车数。 */
    private int maxVehicleCount;
}
