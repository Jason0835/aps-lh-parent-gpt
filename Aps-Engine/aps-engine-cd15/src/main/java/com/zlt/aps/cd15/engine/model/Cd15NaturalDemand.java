package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CD15 单个成型自然班次需求。
 */
@Data
@Builder
public class Cd15NaturalDemand {

    /** 工厂编码。 */
    private String factoryCode;
    /** 排程日期。 */
    private Date scheduleDate;
    /** 成型批次号。 */
    private String cxBatchNo;
    /** 成型机台编码。 */
    private String cxMachineCode;
    /** 胎胚施工代码。 */
    private String constructionCode;
    /** 施工版本，对应成型 CLASSn_RECIPE_NO。 */
    private String constructionVersion;
    /** 班次字段，如 class1。 */
    private String classField;
    /** 班次序号。 */
    private int classIndex;
    /** 成型自然需求条数。 */
    private BigDecimal naturalDemandQty;
}