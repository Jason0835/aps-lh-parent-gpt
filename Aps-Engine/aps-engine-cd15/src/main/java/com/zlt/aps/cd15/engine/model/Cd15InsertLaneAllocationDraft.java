package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 插单滚动后需要替换的库排分配明细草稿。 */
@Data
@Builder
public class Cd15InsertLaneAllocationDraft {

    /** 原主结果ID；插单新增主结果为空。 */
    private Long scheduleResultId;
    /** 是否归属于本次新增插单主结果。 */
    private boolean insertResult;
    /** 新增主结果稳定键，用于分裁组合的两条新增结果分别挂接库排明细。 */
    private String newResultKey;
    private String classField;
    private Date shiftScheduleDate;
    private String laneCode;
    private BigDecimal allocationQuantity;
    private int vehicleCount;
    private int allocationOrder;
}
