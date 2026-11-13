package com.zlt.aps.common.core.utils;

/**
 * 计划分析报表导出Excel的列布局定义。
 *
 * <p>各模块页面列顺序不同，导出Excel需与页面保持一致，因此按布局区分模板：
 * <ul>
 *     <li>{@link #STANDARD}：工厂、排程日期在前，物料编码在物料名称前，与胎面/胎侧/斜裁/直裁页面一致；</li>
 *     <li>{@link #EMBRYO_FIRST}：胎胚/类别在最前，物料名称在编码前，工厂与排程日期在最后，与垫胶/内衬页面一致。</li>
 * </ul>
 * 每种布局同时记录数值列的起止下标，用于统一整数数值显示格式。</p>
 */
public enum ScheduleAnalysisReportColumnLayout {

    /**
     * 标准布局：工厂、排程日期、胎胚/类别、物料编码、物料名称、六点库存、八个班次、机台。
     */
    STANDARD("excelModel/scheduleAnalysisReport.xlsx", 5, 14),

    /**
     * 胎胚优先布局：胎胚/类别、物料名称、物料编码、六点库存、八个班次、机台、工厂、排程日期。
     */
    EMBRYO_FIRST("excelModel/scheduleAnalysisReportEmbryoFirst.xlsx", 3, 12);

    /**
     * 布局对应的Excel模板资源路径
     */
    private final String templateResource;

    /**
     * 数值列起始下标（从0开始，含）
     */
    private final int firstNumericColumnIndex;

    /**
     * 数值列结束下标（从0开始，含）
     */
    private final int lastNumericColumnIndex;

    /**
     * 构造布局定义。
     *
     * @param templateResource          布局对应的Excel模板资源路径
     * @param firstNumericColumnIndex   数值列起始下标（从0开始，含）
     * @param lastNumericColumnIndex    数值列结束下标（从0开始，含）
     */
    ScheduleAnalysisReportColumnLayout(String templateResource, int firstNumericColumnIndex,
                                       int lastNumericColumnIndex) {
        this.templateResource = templateResource;
        this.firstNumericColumnIndex = firstNumericColumnIndex;
        this.lastNumericColumnIndex = lastNumericColumnIndex;
    }

    /**
     * 获取布局对应的Excel模板资源路径。
     *
     * @return 类路径下的模板资源路径
     */
    public String getTemplateResource() {
        return this.templateResource;
    }

    /**
     * 获取数值列起始下标。
     *
     * @return 数值列起始下标（从0开始，含）
     */
    public int getFirstNumericColumnIndex() {
        return this.firstNumericColumnIndex;
    }

    /**
     * 获取数值列结束下标。
     *
     * @return 数值列结束下标（从0开始，含）
     */
    public int getLastNumericColumnIndex() {
        return this.lastNumericColumnIndex;
    }
}
