package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎面、胎侧计划分析报表明细行。
 */
@Data
public class ScheduleAnalysisReportRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前响应内稳定的业务行键，不对应数据库主键。
     */
    private String rowKey;

    /**
     * 工厂编码。
     */
    private String factoryCode;

    /**
     * 排程日期。
     */
    private String scheduleDate;

    /**
     * 数据类型：1成型需求、2交接班库存、3排程计划。
     */
    private Integer dataType;

    /**
     * 成型胎胚编码，库存和计划行为空。
     */
    private String embryoCode;

    /**
     * 成型胎胚描述，仅需求行有值；前端与导出优先展示该字段，为空时回退胎胚编码。
     */
    private String embryoDesc;

    /**
     * 胎面或胎侧物料编码。
     */
    private String productCode;

    /**
     * 胎面或胎侧物料名称。
     */
    private String productName;

    /**
     * 库存基线净量，仅库存行有值；直裁按当前库存班次参数取值，其余报表沿用六点口径。
     */
    private BigDecimal sixClockStockQty;

    /**
     * 第一班数量或库存。
     */
    private BigDecimal class1;

    /**
     * 第二班数量或库存。
     */
    private BigDecimal class2;

    /**
     * 第三班数量或库存。
     */
    private BigDecimal class3;

    /**
     * 第四班数量或库存。
     */
    private BigDecimal class4;

    /**
     * 第五班数量或库存。
     */
    private BigDecimal class5;

    /**
     * 第六班数量或库存。
     */
    private BigDecimal class6;

    /**
     * 第七班数量或库存。
     */
    private BigDecimal class7;

    /**
     * 第八班数量或库存；计划行为空。
     */
    private BigDecimal class8;

    /**
     * 成型机或硫化机编码；仅需求行有值。
     */
    private String machineCodes;

    /**
     * 需求机台并集数量；仅计划行有值。
     */
    private Integer machineCount;

    /**
     * 成型来源记录ID，仅需求行有值。
     */
    private Long sourceRecordId;

    /**
     * SKU施工关系记录ID，仅需求行有值。
     */
    private Long skuConstructionRefId;

    /**
     * 施工记录ID，仅需求行有值。
     */
    private Long constructionInfoId;
}
