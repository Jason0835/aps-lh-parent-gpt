package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 自动排程使用的成型排程窄模型。
 */
@Data
@Builder
public class Cd15FormingScheduleSource {

    /** 成型批次号。 */
    private String cxBatchNo;
    /** 排程日期。 */
    private LocalDate scheduleDate;
    /** 胎胚代码，对应施工信息CONSTRUCTION_CODE。 */
    private String embryoCode;
    /** 成型机台代码，用于按钢带统计去重供成型机台数。 */
    private String cxMachineCode;
    /** CLASS1至CLASS8成型计划量。 */
    private List<BigDecimal> classPlanQuantities;
    /** CLASS1至CLASS8对应的施工版本号，来自成型排程CLASSn_RECIPE_NO。 */
    private List<String> classRecipeNos;
}
