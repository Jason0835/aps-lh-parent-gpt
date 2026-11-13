package com.zlt.aps.common.core.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 胎面、胎侧计划分析报表查询条件。
 */
@Data
public class ScheduleAnalysisReportQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码。
     */
    private String factoryCode;

    /**
     * 单日排程日期。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /**
     * 物料编码包含条件。
     */
    private String productCode;

    /**
     * 物料名称包含条件。
     */
    private String productName;

    /**
     * 胎胚编码包含条件。
     */
    private String embryoCode;

    /**
     * 当前页码，从1开始。
     */
    private Integer pageNum = 1;

    /**
     * 每页物料组数。
     */
    private Integer pageSize = 20;
}
