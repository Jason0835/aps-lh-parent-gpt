package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自动排程使用的机台资源窄模型。
 */
@Data
@Builder
public class Cd15MachineResource {

    /** 机台编码。 */
    private String machineCode;
    /** 机台状态，1为开启。 */
    private String status;
    /** 开机班次编码。 */
    private String openMachineClass;
    /** 满班生产定额，单位米/班。 */
    private BigDecimal quota;
    /** 是否支持一出二分裁，对应cd15MachineInfo.isOutTwo=0。 */
    private boolean splitCutSupported;
    /** 机台可裁钢带宽度上限，对应cd15MachineInfo.clothWidthMax。 */
    private BigDecimal clothWidthMax;
    /** 机台可裁钢带宽度下限，对应cd15MachineInfo.clothWidthMin。 */
    private BigDecimal clothWidthMin;
    /** 当前班次内检修开始时间。 */
    private LocalDateTime maintenanceStart;
    /** 当前班次内检修结束时间。 */
    private LocalDateTime maintenanceEnd;
}
