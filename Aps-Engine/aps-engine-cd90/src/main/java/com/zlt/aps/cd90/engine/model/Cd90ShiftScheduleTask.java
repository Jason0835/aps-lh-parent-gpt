package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 当前班次内存任务链节点。 */
@Data
@Builder
public class Cd90ShiftScheduleTask {
    /** 直裁班次字段。 */
    private String classField;
    /** 帘布代码。 */
    private String clothCode;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 帘线规格。 */
    private String cordSpec;
    /** 机台编码。 */
    private String machineCode;
    /** 本班计划量。 */
    private BigDecimal planQuantity;
    /** 计划入库车数。 */
    private int vehicleCount;
    /** 当前机台生产顺序。 */
    private int produceOrder;
    /** 预计开始时间。 */
    private LocalDateTime expectedStartTime;
    /** 预计结束时间。 */
    private LocalDateTime expectedEndTime;
    /** 库排分配明细。 */
    private List<Cd90StorageLaneAllocation> laneAllocations;
}
