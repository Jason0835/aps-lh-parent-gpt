package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 公共报表计算器使用的单条成型来源关联输入。
 */
@Data
public class ScheduleAnalysisDemandInput implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 胎胚代码。
     */
    private String embryoCode;

    /**
     * 胎胚描述；为空时报表胎胚列回退胎胚代码。
     */
    private String embryoDesc;

    /**
     * 物料编码。
     */
    private String productCode;

    /**
     * 施工解析所得物料名称。
     */
    private String productName;

    /**
     * 成型来源记录ID。
     */
    private Long sourceRecordId;

    /**
     * SKU施工关系记录ID。
     */
    private Long skuConstructionRefId;

    /**
     * 施工记录ID。
     */
    private Long constructionInfoId;

    /**
     * 查询关联结果原始顺序号。
     */
    private int associationRowIndex;

    /**
     * 施工单耗，单位米。
     */
    private BigDecimal unitConsumptionMeters;

    /**
     * 原始成型计划CLASS1至CLASS8，空值按零。
     */
    private List<BigDecimal> formingClassPlanQty = new ArrayList<>();

    /**
     * 规范化后的机台代码。
     */
    private String machineCodes;
}
