package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型需求窗口中的自然班次明细。
 */
@Data
@Builder
public class Cd90DemandShift {

    /** 帘布代码。 */
    private String clothCode;
    /** 成型排程来源字段，取CLASS1至CLASS8。 */
    private String classField;
    /** 成型班次唯一标识。 */
    private String shiftKey;
    /** 班次开始时间。 */
    private LocalDateTime startTime;
    /** 成型计划数量，单位条。 */
    private BigDecimal formingQuantity;
    /** 当前帘布需求量，单位米。 */
    private BigDecimal clothDemandQuantity;
    /** 班次时长，单位小时。 */
    private BigDecimal shiftHours;
    /** 当前窗口计入该自然班次的比例，普通整班为1，半班为0.5。 */
    private BigDecimal windowWeight;
    /** 是否参与本次需求计算。 */
    private boolean included;
    /** 是否为停产班次。 */
    private boolean stopped;
}
