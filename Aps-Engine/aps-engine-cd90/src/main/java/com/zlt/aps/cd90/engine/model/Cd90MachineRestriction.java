package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 帘布定点或不可作业机台配置窄模型。
 */
@Data
@Builder
public class Cd90MachineRestriction {

    /** 帘布代码。 */
    private String clothCode;
    /** 机台编码。 */
    private String machineCode;
    /** 作业类型：0定点优先，1不可作业。 */
    private String jobType;
}
