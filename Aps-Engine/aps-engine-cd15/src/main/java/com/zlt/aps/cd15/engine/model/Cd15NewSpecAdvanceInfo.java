package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 单个钢带代号的新增规格提前生产证据。
 */
@Data
@Builder
public class Cd15NewSpecAdvanceInfo {

    /** 钢带代号。 */
    private String steelStripCode;
    /** 历史回看起始日，包含边界。 */
    private LocalDate historyStartDate;
    /** 历史回看结束日，包含边界。 */
    private LocalDate historyEndDate;
    /** 原需求日期，去重后按日期升序。 */
    private List<LocalDate> sourceDemandDates;
    /** 被搬移的原需求唯一键，格式为钢带代号#班次键。 */
    private List<String> sourceDemandKeys;
    /** 需要提前生产的全部净需求量。 */
    private BigDecimal advanceDemandQuantity;
    /** 目标斜裁生产日。 */
    private LocalDate targetProductionDate;
    /** 排程结果原因分析文案。 */
    private String analysis;
}
