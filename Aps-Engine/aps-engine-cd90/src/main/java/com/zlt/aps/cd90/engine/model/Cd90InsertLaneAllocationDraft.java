package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 插单滚动后需要替换的库排分配明细草稿。 */
@Data
@Builder
public class Cd90InsertLaneAllocationDraft {

    /** 原主结果ID；插单新增主结果为空。 */
    private Long scheduleResultId;
    /** 是否归属于本次新增插单主结果。 */
    private boolean insertResult;
    private String classField;
    private Date shiftScheduleDate;
    private String laneCode;
    private BigDecimal allocationQuantity;
    private int vehicleCount;
    private int allocationOrder;
}
