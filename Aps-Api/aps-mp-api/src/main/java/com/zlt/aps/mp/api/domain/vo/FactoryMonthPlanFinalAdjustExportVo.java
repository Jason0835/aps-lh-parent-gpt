package com.zlt.aps.mp.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 月计划定稿表For调整导入导出表
 * @author zlt
 *
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class FactoryMonthPlanFinalAdjustExportVo extends BaseEntity {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.materialCode", width = 8)
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.materialDesc", width = 24)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /**
     * 成型机台信息 多个以，分隔
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.cxMachineCode", width = 8)
    @ApiModelProperty(value = "成型机台信息", name = "cxMachineCode")
    private String cxMachineCode;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    private String structureName;

    /**
     * 生产实际排产量
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.totalQty", width = 10, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    private Integer totalQty;

    /**
     * 开始日期
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.beginDay", width = 6, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.endDay", width = 6, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "结束日期", name = "endDay")
    private Integer endDay;
    
    // TODO 锁定上机日期
    
    /**
     * DAY_1
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day1", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_1", name = "day1")
    private Integer day1;

    /**
     * DAY_2
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day2", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_2", name = "day2")
    private Integer day2;

    /**
     * DAY_3
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day3", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_3", name = "day3")
    private Integer day3;

    /**
     * DAY_4
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day4", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_4", name = "day4")
    private Integer day4;

    /**
     * DAY_5
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day5", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_5", name = "day5")
    private Integer day5;

    /**
     * DAY_6
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day6", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_6", name = "day6")
    private Integer day6;

    /**
     * DAY_7
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day7", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_7", name = "day7")
    private Integer day7;

    /**
     * DAY_8
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day8", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_8", name = "day8")
    private Integer day8;

    /**
     * DAY_9
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day9", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_9", name = "day9")
    private Integer day9;

    /**
     * DAY_10
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day10", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_10", name = "day10")
    private Integer day10;

    /**
     * DAY_11
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day11", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_11", name = "day11")
    private Integer day11;

    /**
     * DAY_12
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day12", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_12", name = "day12")
    private Integer day12;

    /**
     * DAY_13
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day13", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_13", name = "day13")
    private Integer day13;

    /**
     * DAY_14
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day14", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_14", name = "day14")
    private Integer day14;

    /**
     * DAY_15
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day15", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_15", name = "day15")
    private Integer day15;

    /**
     * DAY_16
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day16", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_16", name = "day16")
    private Integer day16;

    /**
     * DAY_17
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day17", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_17", name = "day17")
    private Integer day17;

    /**
     * DAY_18
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day18", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_18", name = "day18")
    private Integer day18;

    /**
     * DAY_19
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day19", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_19", name = "day19")
    private Integer day19;

    /**
     * DAY_20
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day20", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_20", name = "day20")
    private Integer day20;

    /**
     * DAY_21
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day21", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_21", name = "day21")
    private Integer day21;

    /**
     * DAY_22
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day22", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_22", name = "day22")
    private Integer day22;

    /**
     * DAY_23
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day23", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_23", name = "day23")
    private Integer day23;

    /**
     * DAY_24
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day24", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_24", name = "day24")
    private Integer day24;

    /**
     * DAY_25
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day25", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_25", name = "day25")
    private Integer day25;

    /**
     * DAY_26
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day26", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_26", name = "day26")
    private Integer day26;

    /**
     * DAY_27
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day27", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_27", name = "day27")
    private Integer day27;

    /**
     * DAY_28
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day28", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_28", name = "day28")
    private Integer day28;

    /**
     * DAY_29
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day29", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_29", name = "day29")
    private Integer day29;

    /**
     * DAY_30
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day30", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_30", name = "day30")
    private Integer day30;

    /**
     * DAY_31
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.day31", width = 4, cellType = ColumnType.NUMERIC)
    @ApiModelProperty(value = "DAY_31", name = "day31")
    private Integer day31;
}