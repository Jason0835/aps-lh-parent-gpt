package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpFianlVersionStatisticsLog.java
 * 描    述：S2-0613.定稿版本排产结果统计表备份对象 t_mp_final_statistics_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-02-05
 */

@Data
@ApiModel(value = "S2-0613.定稿版本排产结果统计表备份", description = "S2-0613.定稿版本排产结果统计表备份 ")
@TableName(value = "T_MP_FINAL_STATISTICS_LOG")
public class MpFinalVersionStatisticsLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 年月:YYYYMM
     */
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    @TableField(value = "`YEAR_MONTH`")
    private Integer yearMonth;

    /**
     * 需求计划版本
     */
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 最新需求计划版本(每次调整后变化)
     */
    @ApiModelProperty(value = "最新需求计划版本(每次调整后变化)", name = "lastMonthPlanVersion")
    @TableField(value = "LAST_MONTH_PLAN_VERSION")
    private String lastMonthPlanVersion;

    /**
     * 排产版本号
     */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 产品品类 数据字典：biz_product_type  全钢 PCR 半钢
     */
    @ApiModelProperty(value = "产品品类 数据字典：biz_product_type  全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 英寸
     */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /**
     * 自动调整  tempFlag = 1， 确认调整 tempFlag = 0，默认0
     */
    @TableField(value = "TEMP_FLAG")
    private String tempFlag;

    /**
     * 结构类型 01 周期结构 02 常规结构
     */
    @ApiModelProperty(value = "结构类型 01 周期结构 02 常规结构", name = "structureType")
    @TableField(value = "STRUCTURE_TYPE")
    private String structureType;

    /**
     * DAY_1(EmbryoCount:X,LhMachines:X,ChangeMould:X)
     */
    @ApiModelProperty(value = "DAY_1(EmbryoCount:X,LhMachines:X,ChangeMould:X)", name = "day1")
    @TableField(value = "DAY_1")
    private String day1;

    /**
     * DAY_2
     */
    @ApiModelProperty(value = "DAY_2", name = "day2")
    @TableField(value = "DAY_2")
    private String day2;

    /**
     * DAY_3
     */
    @ApiModelProperty(value = "DAY_3", name = "day3")
    @TableField(value = "DAY_3")
    private String day3;

    /**
     * DAY_4
     */
    @ApiModelProperty(value = "DAY_4", name = "day4")
    @TableField(value = "DAY_4")
    private String day4;

    /**
     * DAY_5
     */
    @ApiModelProperty(value = "DAY_5", name = "day5")
    @TableField(value = "DAY_5")
    private String day5;

    /**
     * DAY_6
     */
    @ApiModelProperty(value = "DAY_6", name = "day6")
    @TableField(value = "DAY_6")
    private String day6;

    /**
     * DAY_7
     */
    @ApiModelProperty(value = "DAY_7", name = "day7")
    @TableField(value = "DAY_7")
    private String day7;

    /**
     * DAY_8
     */
    @ApiModelProperty(value = "DAY_8", name = "day8")
    @TableField(value = "DAY_8")
    private String day8;

    /**
     * DAY_9
     */
    @ApiModelProperty(value = "DAY_9", name = "day9")
    @TableField(value = "DAY_9")
    private String day9;

    /**
     * DAY_10
     */
    @ApiModelProperty(value = "DAY_10", name = "day10")
    @TableField(value = "DAY_10")
    private String day10;

    /**
     * DAY_11
     */
    @ApiModelProperty(value = "DAY_11", name = "day11")
    @TableField(value = "DAY_11")
    private String day11;

    /**
     * DAY_12
     */
    @ApiModelProperty(value = "DAY_12", name = "day12")
    @TableField(value = "DAY_12")
    private String day12;

    /**
     * DAY_13
     */
    @ApiModelProperty(value = "DAY_13", name = "day13")
    @TableField(value = "DAY_13")
    private String day13;

    /**
     * DAY_14
     */
    @ApiModelProperty(value = "DAY_14", name = "day14")
    @TableField(value = "DAY_14")
    private String day14;

    /**
     * DAY_15
     */
    @ApiModelProperty(value = "DAY_15", name = "day15")
    @TableField(value = "DAY_15")
    private String day15;

    /**
     * DAY_16
     */
    @ApiModelProperty(value = "DAY_16", name = "day16")
    @TableField(value = "DAY_16")
    private String day16;

    /**
     * DAY_17
     */
    @ApiModelProperty(value = "DAY_17", name = "day17")
    @TableField(value = "DAY_17")
    private String day17;

    /**
     * DAY_18
     */
    @ApiModelProperty(value = "DAY_18", name = "day18")
    @TableField(value = "DAY_18")
    private String day18;

    /**
     * DAY_19
     */
    @ApiModelProperty(value = "DAY_19", name = "day19")
    @TableField(value = "DAY_19")
    private String day19;

    /**
     * DAY_20
     */
    @ApiModelProperty(value = "DAY_20", name = "day20")
    @TableField(value = "DAY_20")
    private String day20;

    /**
     * DAY_21
     */
    @ApiModelProperty(value = "DAY_21", name = "day21")
    @TableField(value = "DAY_21")
    private String day21;

    /**
     * DAY_22
     */
    @ApiModelProperty(value = "DAY_22", name = "day22")
    @TableField(value = "DAY_22")
    private String day22;

    /**
     * DAY_23
     */
    @ApiModelProperty(value = "DAY_23", name = "day23")
    @TableField(value = "DAY_23")
    private String day23;

    /**
     * DAY_24
     */
    @ApiModelProperty(value = "DAY_24", name = "day24")
    @TableField(value = "DAY_24")
    private String day24;

    /**
     * DAY_25
     */
    @ApiModelProperty(value = "DAY_25", name = "day25")
    @TableField(value = "DAY_25")
    private String day25;

    /**
     * DAY_26
     */
    @ApiModelProperty(value = "DAY_26", name = "day26")
    @TableField(value = "DAY_26")
    private String day26;

    /**
     * DAY_27
     */
    @ApiModelProperty(value = "DAY_27", name = "day27")
    @TableField(value = "DAY_27")
    private String day27;

    /**
     * DAY_28
     */
    @ApiModelProperty(value = "DAY_28", name = "day28")
    @TableField(value = "DAY_28")
    private String day28;

    /**
     * DAY_29
     */
    @ApiModelProperty(value = "DAY_29", name = "day29")
    @TableField(value = "DAY_29")
    private String day29;

    /**
     * DAY_30
     */
    @ApiModelProperty(value = "DAY_30", name = "day30")
    @TableField(value = "DAY_30")
    private String day30;

    /**
     * DAY_31
     */
    @ApiModelProperty(value = "DAY_31", name = "day31")
    @TableField(value = "DAY_31")
    private String day31;

}