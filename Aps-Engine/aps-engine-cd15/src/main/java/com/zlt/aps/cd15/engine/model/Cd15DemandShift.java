package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成型需求窗口中的自然班次明细。
 */
@Data
@Builder
public class Cd15DemandShift {

    /** 钢带代码。 */
    private String steelStripCode;
    /** 施工材料稳定键：钢带、大卷、角度和工艺尺寸。 */
    private String materialKey;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 斜裁有效宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单片长度，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 大卷幅宽，单位毫米。 */
    private BigDecimal cordWidth;
    /** 标准卷曲长度，单位米。 */
    private BigDecimal curlLength;
        /** 成型排程来源字段，取CLASS1至CLASS8。 */
    private String classField;
    /** 成型班次唯一标识。 */
    private String shiftKey;
    /** 班次开始时间。 */
    private LocalDateTime startTime;
    /** 成型计划数量，单位条。 */
    private BigDecimal formingQuantity;
    /** 当前钢带需求量，单位米。 */
    private BigDecimal steelStripDemandQuantity;
    /** 班次时长，单位小时。 */
    private BigDecimal shiftHours;
    /** 当前窗口计入该自然班次的比例，普通整班为1，半班为0.5。 */
    private BigDecimal windowWeight;
    /** 是否参与本次需求计算。 */
    private boolean included;
    /** 是否为停产班次。 */
    private boolean stopped;
}
