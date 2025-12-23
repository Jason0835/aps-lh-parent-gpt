package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @since 2025/10/10
 */
@Data
public class MonthPlanDayResultStatisticsVo implements Serializable {

    /**
     * 累计完成量
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.finishQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "FINISH_QTY")
    private Long finishQty;

    /**
     * 剩余量
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.remainingQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "REMAINING_QTY")
    private Long remainingQty;

    /**
     * 需要生产天数
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.needProductionDay", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "NEED_PRODUCTION_DAY")
    private Long needProductionDay;

    /**
     * 剩余生产天数
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.remainingProductionDay", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "REMAINING_PRODUCTION_DAY")
    private Long remainingProductionDay;

    /**
     * 胎面胶种
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.treadGlue")
    @TableField(value = "TREAD_GLUE")
    private String treadGlue;

    /**
     * 毛利率Json
     */
    @ApiModelProperty(value = "毛利率Json", name = "grossRateJson")
    @TableField(value = "GROSS_RATE_JSON")
    private String grossRateJson;

    /**
     * 外销毛利率
     */
    @Excel(name = "ui.data.column.info.outGrossRate", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "外销毛利率", name = "outGrossRate")
    @TableField(exist = false)
    private BigDecimal outGrossRate;

    /**
     * 内销毛利率
     */
    @Excel(name = "ui.data.column.info.inGrossRate", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "内销毛利率", name = "inGrossRate")
    @TableField(exist = false)
    private BigDecimal inGrossRate;

    /**
     * OE毛利率
     */
    @Excel(name = "ui.data.column.info.oeGrossRate", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "OE毛利率", name = "oeGrossRate")
    @TableField(exist = false)
    private BigDecimal oeGrossRate;

    /**
     * 单胎重量
     */
    @Excel(name = "ui.data.column.info.singleTireWeight")
    @ApiModelProperty(value = "单胎重量", name = "singleTireWeight")
    @TableField(value = "SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    /**
     * 月度计划重量
     */
    @Excel(name = "ui.data.column.info.monthPlanTireWeight")
    @ApiModelProperty(value = "月度计划重量", name = "monthPlanTireWeight")
    @TableField(value = "MONTH_PLAN_TIRE_WEIGHT")
    private BigDecimal monthPlanTireWeight;

    /**
     * 夏季硫化时间
     */
    @ApiModelProperty(value = "夏季硫化时间", name = "curingTime")
    private BigDecimal curingTime;

    /**
     * 冬季硫化时间
     */
    @ApiModelProperty(value = "冬季硫化时间", name = "curingTime2")
    private BigDecimal curingTime2;

    /**
     * 施工代号
     */
    @Excel(name = "ui.data.column.mdmProductConstruction.constructionCode")
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * 共用模具
     */
    @Excel(name = "ui.data.column.info.sharedMold")
    @ApiModelProperty(value = "共用模具", name = "sharedMold")
    private String sharedMold;
}
