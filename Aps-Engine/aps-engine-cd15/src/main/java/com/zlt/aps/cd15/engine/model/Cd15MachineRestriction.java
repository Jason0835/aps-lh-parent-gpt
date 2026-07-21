package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 钢带定点或不可作业机台配置窄模型。
 */
@Data
@Builder
public class Cd15MachineRestriction {

    /** 钢带代码。 */
    private String steelStripCode;
    /** 机台编码。 */
    private String machineCode;
    /** 作业类型：0定点优先，1不可作业。 */
    private String jobType;
}
