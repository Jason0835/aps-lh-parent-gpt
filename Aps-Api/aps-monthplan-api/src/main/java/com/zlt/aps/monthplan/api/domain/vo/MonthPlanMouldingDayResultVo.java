package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Chen
 * @since 2025/9/22
 */
@Data
@ApiModel(value = "分厂月生产计划排产过程-合并SKU", description = "分厂月生产计划排产过程-合并SKU")
public class MonthPlanMouldingDayResultVo extends BaseEntity {

    /**
     * 工厂编码（对应SQL：a.FACTORY_CODE）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.factoryCode", dictType = "biz_factory_name")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份（对应SQL：a.`YEAR`）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份（对应SQL：a.`MONTH`）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 产品编码（对应SQL：a.PRODUCT_CODE）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格编码（对应SQL：a.SPEC_CODE）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 胎胚编码（对应SQL：a.EMBRYO_CODE）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 库位类型（对应SQL：a.LOCATION_TYPE）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.locationType", dictType = "biz_stor_type")
    @TableField(value = "LOCATION_TYPE")
    private Integer locationType;

    /**
     * 渠道（对应SQL：a.CHANNEL）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.channel", dictType = "biz_channel_type")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 产品描述（对应SQL：a.PRODUCT_DESC）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 规格详情（对应SQL：a.SPECIFICATIONS）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 产品尺寸（对应SQL：a.PRO_SIZE）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /**
     * 花纹（对应SQL：a.PATTERN）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 品牌（对应SQL：a.BRAND）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.brand", dictType = "biz_brand_type")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 模具编号（对应SQL：a.MOULD_NO）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;

    /**
     * 模具数量（对应SQL：a.MOULD_QTY）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;

    /**
     * 内销计划量（对应SQL：saleOrder.inPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.inPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "inPlanQty")
    private BigDecimal inPlanQty;

    /**
     * 外销计划量（对应SQL：saleOrder.outPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.outPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "outPlanQty")
    private BigDecimal outPlanQty;

    /**
     * OE计划量（对应SQL：saleOrder.oePlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.oePlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "oePlanQty")
    private BigDecimal oePlanQty;

    /**
     * 总计划量（对应SQL：saleOrder.totalPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.totalPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "totalPlanQty")
    private BigDecimal totalPlanQty;

    /**
     * 配套渠道计划量（对应SQL：saleOrder.ptPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.ptPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "ptPlanQty")
    private BigDecimal ptPlanQty;

    /**
     * 途虎渠道计划量（对应SQL：saleOrder.thPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.thPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "thPlanQty")
    private BigDecimal thPlanQty;

    /**
     * 快准渠道计划量（对应SQL：saleOrder.kzPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.kzPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "kzPlanQty")
    private BigDecimal kzPlanQty;

    /**
     * RT渠道计划量（对应SQL：saleOrder.rtPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.rtPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "rtPlanQty")
    private BigDecimal rtPlanQty;

    /**
     * 外贸渠道计划量（对应SQL：saleOrder.wmPlanQty）
     */
//    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.wmPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "wmPlanQty")
    private BigDecimal wmPlanQty;

    /**
     * 外销贴牌
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.wmBrandPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "wmBrandPlanQty")
    private BigDecimal wmBrandPlanQty;

    /**
     * 非贴牌外销
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.wmUnBrandPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "wmUnBrandPlanQty")
    private BigDecimal wmUnBrandPlanQty;

    /**
     * KA渠道计划量（对应SQL：saleOrder.kaPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.kaPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "kaPlanQty")
    private BigDecimal kaPlanQty;

    /**
     * 上月库存数量（对应SQL：stock.stock_qty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "stock_qty")
    private BigDecimal stockQty;

    /**
     * 内销月均销量（对应SQL：stockUp.inMonthAvgSaleQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.inMonthAvgSaleQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "inMonthAvgSaleQty")
    private BigDecimal inMonthAvgSaleQty;

    /**
     * 内销备货率（对应SQL：stockUp.inStockUpRate）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.inStockUpRate", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "inStockUpRate")
    private BigDecimal inStockUpRate;

    /**
     * 内销备货计划量（对应SQL：stockUp.inStockUpPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.inStockUpPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "inStockUpPlanQty")
    private BigDecimal inStockUpPlanQty;

    /**
     * 外销月均销量（对应SQL：stockUp.outMonthAvgSaleQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.outMonthAvgSaleQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "outMonthAvgSaleQty")
    private BigDecimal outMonthAvgSaleQty;

    /**
     * 外销备货率（对应SQL：stockUp.outStockUpRate）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.outStockUpRate", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "outStockUpRate")
    private BigDecimal outStockUpRate;

    /**
     * 外销备货计划量（对应SQL：stockUp.outStockUpPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.outStockUpPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "outStockUpPlanQty")
    private BigDecimal outStockUpPlanQty;

    /**
     * OE月均销量（对应SQL：stockUp.oeMonthAvgSaleQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.oeMonthAvgSaleQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "oeMonthAvgSaleQty")
    private BigDecimal oeMonthAvgSaleQty;

    /**
     * OE备货率（对应SQL：stockUp.oeStockUpRate）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.oeStockUpRate", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "oeStockUpRate")
    private BigDecimal oeStockUpRate;

    /**
     * OE备货计划量（对应SQL：stockUp.oeStockUpPlanQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.oeStockUpPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "oeStockUpPlanQty")
    private BigDecimal oeStockUpPlanQty;

    /**
     * 总备货量（对应SQL：stockUp.totalStockUpQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.totalStockUpQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "totalStockUpQty")
    private BigDecimal totalStockUpQty;

    /**
     * 总分配量（对应SQL：allocation.totalAllocationQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.totalAllocationQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "totalAllocationQty")
    private BigDecimal totalAllocationQty;

    /**
     * 总应生产量（对应SQL：allocation.totalProduceQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.totalProduceQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "totalProduceQty")
    private BigDecimal totalProduceQty;

    /**
     * 总计划生产量（对应SQL：final.totalProductQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.totalProductQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "totalProductQty")
    private BigDecimal totalProductQty;

    /**
     * 实际备货量（对应SQL：final.totalStockUpActQty）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.totalStockUpActQty", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "totalStockUpActQty")
    private BigDecimal totalStockUpActQty;

    /**
     * 月末库存（对应SQL：stock.stock_qty + final.totalProductQty AS monthStock）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.monthStock", cellType = Excel.ColumnType.NUMERIC)
    @TableField(value = "monthStock")
    private BigDecimal monthStock;

    /**
     * 备注原因（对应SQL：final.REASON，GROUP_CONCAT结果）
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.reason")
    @TableField(value = "REASON")
    private String reason;

    /**
     * 续作标识
     */
    @TableField(value = "continueFlag")
    private String continueFlag;

    /**
     * 重要客户标识
     */
    @TableField(value = "importantCustomFlag")
    private String importantCustomFlag;

    /**
     * 必保标识
     */
    @TableField(value = "ensurePlanFlag")
    private String ensurePlanFlag;

    /**
     * 急单标识
     */
    @TableField(value = "emergencyFlag")
    private String emergencyFlag;

    /**
     * 欠产标识
     */
    @TableField(value = "debitPlanFlag")
    private String debitPlanFlag;

    /**
     * 备货标识
     */
    @TableField(value = "stockUpFlag")
    private String stockUpFlag;

    /**
     * 交期标识
     */
    @TableField(value = "dateDueFlag")
    private String dateDueFlag;

    /**
     * 展示标识，将上面的标识拼接，空的不拼接
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.showFlag")
    @TableField(exist = false)
    private String showFlag;


    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day1", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private Long day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day2", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private Long day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day3", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private Long day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day4", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private Long day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day5", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private Long day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day6", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private Long day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day7", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private Long day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day8", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private Long day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day9", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private Long day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day10", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private Long day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day11", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private Long day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day12", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private Long day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day13", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private Long day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day14", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private Long day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day15", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private Long day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day16", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private Long day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day17", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private Long day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day18", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private Long day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day19", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private Long day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day20", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private Long day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day21", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private Long day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day22", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private Long day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day23", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private Long day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day24", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private Long day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day25", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private Long day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day26", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private Long day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day27", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private Long day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day28", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private Long day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day29", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private Long day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day30", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private Long day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.day31", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 99999999)
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private Long day31;

    /**
     * 开始日期
     */
//    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.beginDate")
//    @ImportExcelValidated(required = true, min = 1, max = 31)
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Integer beginDate;

    /**
     * 开始日期
     */
//    @Excel(name = "ui.data.column.monthPlanMouldingDayResult.endDay")
//    @ImportExcelValidated(required = true, min = 1, max = 31)
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;
}
