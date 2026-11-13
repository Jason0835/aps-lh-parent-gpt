package com.zlt.aps.common.core.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zlt.aps.common.core.serializer.FactoryZoneDateSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 胎面、胎侧计划分析报表查询结果。
 */
@Data
public class ScheduleAnalysisReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码。
     */
    private String factoryCode;

    /**
     * 排程日期。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /**
     * 本次读取完成时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonSerialize(using = FactoryZoneDateSerializer.class)
    private Date queriedAt;

    /**
     * 查询时间序列化使用的工厂时区，该内部字段不加入接口响应。
     */
    @JsonIgnore
    private String queriedAtZoneId;

    /**
     * 当前机台字段口径说明。
     */
    private String machineSourceField;

    /**
     * 可选库存业务班次；直裁按排程批次快照返回，斜裁沿用工厂库存班次参数。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String stockShiftCode;

    /**
     * 可选库存基线标题，含业务时间；未设置的报表沿用六点库存标题。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String stockBaselineLabel;

    /**
     * 八班动态表头。
     */
    private List<ScheduleAnalysisShiftHeaderVO> shiftHeaders = new ArrayList<>();

    /**
     * 匹配物料组总数。
     */
    private long totalGroups;

    /**
     * 匹配物料组的全部报表行数。
     */
    private long totalRows;

    /**
     * 当前页码。
     */
    private int pageNum;

    /**
     * 每页物料组数。
     */
    private int pageSize;

    /**
     * 当前页物料组的完整明细。
     */
    private List<ScheduleAnalysisReportRowVO> rows = new ArrayList<>();
}
