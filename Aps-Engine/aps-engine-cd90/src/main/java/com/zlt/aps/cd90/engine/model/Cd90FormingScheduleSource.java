package com.zlt.aps.cd90.engine.model;

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
public class Cd90FormingScheduleSource {

    /** 成型批次号。 */
    private String cxBatchNo;
    /** 排程日期。 */
    private LocalDate scheduleDate;
    /** 胎胚代码，对应施工信息CONSTRUCTION_CODE。 */
    private String embryoCode;
    /** CLASS1至CLASS8成型计划量。 */
    private List<BigDecimal> classPlanQuantities;
}
