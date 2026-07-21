package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/** 机台任务链末尾的大卷与斜裁规格状态。 */
@Data
@Builder
public class Cd15MachineTailState {
    /** 施工材料稳定键。 */
    private String materialKey;
    /** 链尾斜裁规格。 */
    private String steelStripCode;
    /** 链尾大卷代码。 */
    private String bigRollCode;
    /** 裁断角度。 */
    private String cuttingAngle;
}
