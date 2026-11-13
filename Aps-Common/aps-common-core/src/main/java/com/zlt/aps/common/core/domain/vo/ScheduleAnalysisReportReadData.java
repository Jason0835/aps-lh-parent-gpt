package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计划分析报表单次只读事务收集的数据。
 */
@Data
public class ScheduleAnalysisReportReadData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前日期有效成型来源的逐条关联行。
     */
    private List<ScheduleAnalysisDemandInput> demands = new ArrayList<>();

    /**
     * 当前日期有效排程结果行。
     */
    private List<ScheduleAnalysisPlanInput> currentResults = new ArrayList<>();

    /**
     * 前一日有效排程结果行。
     */
    private List<ScheduleAnalysisPlanInput> previousResults = new ArrayList<>();

    /**
     * 物料编码至六点净库存的映射。
     */
    private Map<String, BigDecimal> sixClockStocks = new LinkedHashMap<>();

    /**
     * 当前有效工厂配置生成的动态班次表头。
     */
    private List<ScheduleAnalysisShiftHeaderVO> shiftHeaders = new ArrayList<>();

    /**
     * 当前机台字段的来源说明。
     */
    private String machineSourceField;

    /**
     * 当前工厂时区参数解析后的时区标识。
     */
    private String factoryZoneId;
}
