package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 当前班次资源快照中的单个库排状态。
 */
@Data
@Builder
public class Cd90StorageLaneState {

    /** 库排编码。 */
    private String laneCode;
    /** 当前占用的帘布代码；车数为0时清空。 */
    private String clothCode;
    /** 当前占用车数。 */
    private int vehicleCount;
    /** 最大可容纳车数。 */
    private int maxVehicleCount;
}
