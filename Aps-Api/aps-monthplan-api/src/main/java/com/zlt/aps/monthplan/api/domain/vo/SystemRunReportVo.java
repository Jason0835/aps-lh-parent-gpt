package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Chen
 * @since 2025/8/1
 */
@Data
public class SystemRunReportVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 排程日期
     */
    @Excel(name = "ui.data.column.report.systemRunReport.scheduleDate")
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    private String scheduleDate;

    /**
     * 工序
     */
    @Excel(name = "ui.data.column.report.systemRunReport.productName")
    @ApiModelProperty(value = "工序", name = "productName")
    private String productProcessName;

    /**
     * 工序代号
     */
    @ApiModelProperty(value = "工序代号", name = "productProcess")
    private String productProcess;

    /**
     * 计划排产规格数
     */
    @Excel(name = "ui.data.column.report.systemRunReport.planSkuCount", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "计划排产规格数", name = "planSkuCount")
    private Double planSkuCount = 0D;

    /**
     * 计划总排产量
     */
    @Excel(name = "ui.data.column.report.systemRunReport.planSkuQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "计划总排产量", name = "planSkuQty")
    private Double planSkuQty = 0D;

    /**
     * 实际完成数
     */
    @Excel(name = "ui.data.column.report.systemRunReport.finishQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "实际完成数", name = "finishQty")
    private Double finishQty = 0D;

    /**
     * 期初库存
     */
    @Excel(name = "ui.data.column.report.systemRunReport.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "期初库存", name = "stockQty")
    private Double stockQty = 0D;

    /**
     * 期末库存
     */
    @Excel(name = "ui.data.column.report.systemRunReport.planStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "期末库存", name = "planStockQty")
    private Double planStockQty = 0D;

    /**
     * 异常生产规格数（完成率不足90%）
     */
    @Excel(name = "ui.data.column.report.systemRunReport.abnormalSkuCount", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "异常生产规格数（完成率不足90%）", name = "abnormalSkuCount")
    private Double abnormalSkuCount = 0D;

    /**
     * 达成率
     */
    @Excel(name = "ui.data.column.report.systemRunReport.finishRate", cellType = Excel.ColumnType.NUMERIC, suffix = "%")
    @ApiModelProperty(value = "达成率", name = "finishRate")
    private Double finishRate = 0D;

    /**
     * 计量单位
     */
    @Excel(name = "ui.data.column.report.systemRunReport.meteringUnit")
    @ApiModelProperty(value = "计量单位", name = "meteringUnit")
    private String meteringUnit;

    /**
     * 夜班计划
     */
    @ApiModelProperty(value = "夜班计划", name = "nightPlanQty")
    private Double nightPlanQty = 0D;

    /**
     * 昨日早班计划数
     */
    @ApiModelProperty(value = "昨日早班计划数", name = "lastDayPlanQty")
    private Double lastDayPlanQty = 0D;
}
