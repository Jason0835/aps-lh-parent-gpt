package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 大卷与机台物理生产绑定窄模型。
 */
@Data
@Builder
public class Cd90MachineRollBinding {

    /** 大卷代码，对应施工CORD_SPEC。 */
    private String bigRollCode;
    /** 帘布代码。 */
    private String clothCode;
    /** 机台编码。 */
    private String machineCode;
}
