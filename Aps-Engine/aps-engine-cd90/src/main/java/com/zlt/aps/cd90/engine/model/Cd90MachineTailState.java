package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/** 机台任务链末尾的大卷与直裁规格状态。 */
@Data
@Builder
public class Cd90MachineTailState {
    /** 链尾直裁规格。 */
    private String clothCode;
    /** 链尾大卷代码。 */
    private String bigRollCode;
}
