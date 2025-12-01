package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionGroupResultHelper.java
 * 描    述：分厂月生产计划排产结果-分组排产结果辅助信息对象 t_mp_production_group_helper
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-18
 */

@Data
@TableName(value = "T_MP_PRODUCTION_GROUP_HELPER")
@ApiModel(value = "分厂月生产计划排产结果-分组排产结果辅助信息对象", description = "分厂月生产计划排产结果-分组排产结果辅助信息对象")
public class ProductionGroupResultHelper extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 分组编号
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.productionGroupNo")
    @ApiModelProperty(value = "分组编号", name = "productionGroupNo")
    @TableField(value = "PRODUCTION_GROUP_NO")
    private String productionGroupNo;

    /**
     * 模台编号
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.groupMouldTableNo")
    @ApiModelProperty(value = "模台编号", name = "groupMouldTableNo")
    @TableField(value = "GROUP_MOULD_TABLE_NO")
    private String groupMouldTableNo;
    /**
     * 模台数 1 单模台 2 双模台
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.mouldNumber")
    @ApiModelProperty(value = "模台数", name = "mouldNumber")
    @TableField(value = "MOULD_NUMBER")
    private Integer mouldNumber;

    /**
     * DAY_1-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day1")
    @ApiModelProperty(value = "DAY_1", name = "day1")
    @TableField(value = "DAY_1")
    private String day1;

    /**
     * DAY_2-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day2")
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private String day2;

    /**
     * DAY_3-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day3")
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private String day3;

    /**
     * DAY_4-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day4")
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private String day4;

    /**
     * DAY_5-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day5")
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private String day5;

    /**
     * DAY_6-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day6")
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private String day6;

    /**
     * DAY_7-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day7")
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private String day7;

    /**
     * DAY_8-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day8")
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private String day8;

    /**
     * DAY_9-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day9")
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private String day9;

    /**
     * DAY_10-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day10")
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private String day10;

    /**
     * DAY_11-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day11")
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private String day11;

    /**
     * DAY_12-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day12")
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private String day12;

    /**
     * DAY_13-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day13")
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private String day13;

    /**
     * DAY_14-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day14")
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private String day14;

    /**
     * DAY_15-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day15")
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private String day15;

    /**
     * DAY_16-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day16")
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private String day16;

    /**
     * DAY_17-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day17")
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private String day17;

    /**
     * DAY_18-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day18")
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private String day18;

    /**
     * DAY_19-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day19")
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private String day19;

    /**
     * DAY_20-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day20")
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private String day20;

    /**
     * DAY_21-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day21")
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private String day21;

    /**
     * DAY_22-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day22")
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private String day22;

    /**
     * DAY_23-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day23")
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private String day23;

    /**
     * DAY_24-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day24")
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private String day24;

    /**
     * DAY_25-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day25")
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private String day25;

    /**
     * DAY_26-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day26")
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private String day26;

    /**
     * DAY_27-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day27")
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private String day27;

    /**
     * DAY_28-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day28")
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private String day28;

    /**
     * DAY_29-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day29")
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private String day29;

    /**
     * DAY_30-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day30")
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private String day30;

    /**
     * DAY_31-排产日信息json格式
     */
    @Excel(name = "ui.data.column.productionGroupResultHelper.day31")
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private String day31;

}