package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 大卷与机台物理生产绑定窄模型。
 */
@Data
@Builder
public class Cd15MachineRollBinding {

    /** 大卷代码，对应施工CORD_SPEC。 */
    private String bigRollCode;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 可生产班次编码，多个值使用逗号分隔。 */
    private String shiftCode;
    /** 机台编码。 */
    private String machineCode;
}
