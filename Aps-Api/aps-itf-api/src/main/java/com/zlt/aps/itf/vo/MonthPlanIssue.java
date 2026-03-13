package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanIssue.java
 * 描    述：月计划下发对象 month_plan_issue
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-24
 */
@ApiModel(value = "月计划下发对象", description = "月计划下发对象 ")
@Data
@TableName(value = "MONTH_PLAN_ISSUE")
public class MonthPlanIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 月计划版本号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.mpVersionNo")
    @ApiModelProperty(value = "月计划版本号", name = "mpVersionNo")
    @TableField(value = "MP_VERSION_NO")
    private String mpVersionNo;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanIssue.mpYear")
    @ApiModelProperty(value = "年份", name = "mpYear")
    @TableField(value = "MP_YEAR")
    private String mpYear;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanIssue.mpMonth")
    @ApiModelProperty(value = "月份", name = "mpMonth")
    @TableField(value = "MP_MONTH")
    private String mpMonth;

    /**
     * 工单号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.orderNo")
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 结构
     */
    @Excel(name = "ui.data.column.monthPlanIssue.strucCode")
    @ApiModelProperty(value = "结构", name = "strucCode")
    @TableField(value = "STRUC_CODE")
    private String strucCode;

    /**
     * 物料编码（MES）
     */
    @Excel(name = "ui.data.column.monthPlanIssue.mesMaterialCode", readConverterExp = "M=ES")
    @ApiModelProperty(value = "物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.monthPlanIssue.specDesc")
    @ApiModelProperty(value = "物料描述", name = "specDesc")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 成型胎胚物料
     */
    @Excel(name = "ui.data.column.monthPlanIssue.embryoSpec")
    @ApiModelProperty(value = "成型胎胚物料", name = "embryoSpec")
    @TableField(value = "EMBRYO_SPEC")
    private String embryoSpec;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.monthPlanIssue.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 型腔
     */
    @Excel(name = "ui.data.column.monthPlanIssue.cavity")
    @ApiModelProperty(value = "型腔", name = "cavity")
    @TableField(value = "CAVITY")
    private Integer cavity;

    /**
     * 活块
     */
    @Excel(name = "ui.data.column.monthPlanIssue.liveBlock")
    @ApiModelProperty(value = "活块", name = "liveBlock")
    @TableField(value = "LIVE_BLOCK")
    private Integer liveBlock;

    /**
     * 净需求
     */
    @Excel(name = "ui.data.column.monthPlanIssue.netDemand")
    @ApiModelProperty(value = "净需求", name = "netDemand")
    @TableField(value = "NET_DEMAND")
    private Integer netDemand;

    /**
     * 高优先级
     */
    @Excel(name = "ui.data.column.monthPlanIssue.advNum")
    @ApiModelProperty(value = "高优先级", name = "advNum")
    @TableField(value = "ADV_NUM")
    private Integer advNum;

    /**
     * 月均销量
     */
    @Excel(name = "ui.data.column.monthPlanIssue.monthAvgNum")
    @ApiModelProperty(value = "月均销量", name = "monthAvgNum")
    @TableField(value = "MONTH_AVG_NUM")
    private Integer monthAvgNum;

    /**
     * 库销比
     */
    @Excel(name = "ui.data.column.monthPlanIssue.stockSaleRatio")
    @ApiModelProperty(value = "库销比", name = "stockSaleRatio")
    @TableField(value = "STOCK_SALE_RATIO")
    private BigDecimal stockSaleRatio;

    /**
     * 日硫化量
     */
    @Excel(name = "ui.data.column.monthPlanIssue.dayVulQty")
    @ApiModelProperty(value = "日硫化量", name = "dayVulQty")
    @TableField(value = "DAY_VUL_QTY")
    private Integer dayVulQty;

    /**
     * 调整1
     */
    @Excel(name = "ui.data.column.monthPlanIssue.adjustQty1")
    @ApiModelProperty(value = "调整1", name = "adjustQty1")
    @TableField(value = "ADJUST_QTY1")
    private Integer adjustQty1;

    /**
     * 调整2
     */
    @Excel(name = "ui.data.column.monthPlanIssue.adjustQty2")
    @ApiModelProperty(value = "调整2", name = "adjustQty2")
    @TableField(value = "ADJUST_QTY2")
    private Integer adjustQty2;

    /**
     * 调整3
     */
    @Excel(name = "ui.data.column.monthPlanIssue.adjustQty3")
    @ApiModelProperty(value = "调整3", name = "adjustQty3")
    @TableField(value = "ADJUST_QTY3")
    private Integer adjustQty3;

    /**
     * 调整4
     */
    @Excel(name = "ui.data.column.monthPlanIssue.adjustQty4")
    @ApiModelProperty(value = "调整4", name = "adjustQty4")
    @TableField(value = "ADJUST_QTY4")
    private Integer adjustQty4;

    /**
     * 1号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day1")
    @ApiModelProperty(value = "1号", name = "day1")
    @TableField(value = "DAY1")
    private Integer day1;

    /**
     * 2号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day2")
    @ApiModelProperty(value = "2号", name = "day2")
    @TableField(value = "DAY2")
    private Integer day2;

    /**
     * 3号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day3")
    @ApiModelProperty(value = "3号", name = "day3")
    @TableField(value = "DAY3")
    private Integer day3;

    /**
     * 4号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day4")
    @ApiModelProperty(value = "4号", name = "day4")
    @TableField(value = "DAY4")
    private Integer day4;

    /**
     * 5号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day5")
    @ApiModelProperty(value = "5号", name = "day5")
    @TableField(value = "DAY5")
    private Integer day5;

    /**
     * 6号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day6")
    @ApiModelProperty(value = "6号", name = "day6")
    @TableField(value = "DAY6")
    private Integer day6;

    /**
     * 7号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day7")
    @ApiModelProperty(value = "7号", name = "day7")
    @TableField(value = "DAY7")
    private Integer day7;

    /**
     * 8号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day8")
    @ApiModelProperty(value = "8号", name = "day8")
    @TableField(value = "DAY8")
    private Integer day8;

    /**
     * 9号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day9")
    @ApiModelProperty(value = "9号", name = "day9")
    @TableField(value = "DAY9")
    private Integer day9;

    /**
     * 10号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day10")
    @ApiModelProperty(value = "10号", name = "day10")
    @TableField(value = "DAY10")
    private Integer day10;

    /**
     * 11号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day11")
    @ApiModelProperty(value = "11号", name = "day11")
    @TableField(value = "DAY11")
    private Integer day11;

    /**
     * 12号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day12")
    @ApiModelProperty(value = "12号", name = "day12")
    @TableField(value = "DAY12")
    private Integer day12;

    /**
     * 13号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day13")
    @ApiModelProperty(value = "13号", name = "day13")
    @TableField(value = "DAY13")
    private Integer day13;

    /**
     * 14号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day14")
    @ApiModelProperty(value = "14号", name = "day14")
    @TableField(value = "DAY14")
    private Integer day14;

    /**
     * 15号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day15")
    @ApiModelProperty(value = "15号", name = "day15")
    @TableField(value = "DAY15")
    private Integer day15;

    /**
     * 16号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day16")
    @ApiModelProperty(value = "16号", name = "day16")
    @TableField(value = "DAY16")
    private Integer day16;

    /**
     * 17号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day17")
    @ApiModelProperty(value = "17号", name = "day17")
    @TableField(value = "DAY17")
    private Integer day17;

    /**
     * 18号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day18")
    @ApiModelProperty(value = "18号", name = "day18")
    @TableField(value = "DAY18")
    private Integer day18;

    /**
     * 19号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day19")
    @ApiModelProperty(value = "19号", name = "day19")
    @TableField(value = "DAY19")
    private Integer day19;

    /**
     * 20号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day20")
    @ApiModelProperty(value = "20号", name = "day20")
    @TableField(value = "DAY20")
    private Integer day20;

    /**
     * 21号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day21")
    @ApiModelProperty(value = "21号", name = "day21")
    @TableField(value = "DAY21")
    private Integer day21;

    /**
     * 22号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day22")
    @ApiModelProperty(value = "22号", name = "day22")
    @TableField(value = "DAY22")
    private Integer day22;

    /**
     * 23号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day23")
    @ApiModelProperty(value = "23号", name = "day23")
    @TableField(value = "DAY23")
    private Integer day23;

    /**
     * 24号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day24")
    @ApiModelProperty(value = "24号", name = "day24")
    @TableField(value = "DAY24")
    private Integer day24;

    /**
     * 25号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day25")
    @ApiModelProperty(value = "25号", name = "day25")
    @TableField(value = "DAY25")
    private Integer day25;

    /**
     * 26号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day26")
    @ApiModelProperty(value = "26号", name = "day26")
    @TableField(value = "DAY26")
    private Integer day26;

    /**
     * 27号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day27")
    @ApiModelProperty(value = "27号", name = "day27")
    @TableField(value = "DAY27")
    private Integer day27;

    /**
     * 28号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day28")
    @ApiModelProperty(value = "28号", name = "day28")
    @TableField(value = "DAY28")
    private Integer day28;

    /**
     * 29号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day29")
    @ApiModelProperty(value = "29号", name = "day29")
    @TableField(value = "DAY29")
    private Integer day29;

    /**
     * 30号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day30")
    @ApiModelProperty(value = "30号", name = "day30")
    @TableField(value = "DAY30")
    private Integer day30;

    /**
     * 31号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.day31")
    @ApiModelProperty(value = "31号", name = "day31")
    @TableField(value = "DAY31")
    private Integer day31;

    /**
     * 版本号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.dataVersion")
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 分公司编码（116）
     */
    @Excel(name = "ui.data.column.monthPlanIssue.companyCode", readConverterExp = "1=16")
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /**
     * 厂别（116）
     */
    @Excel(name = "ui.data.column.monthPlanIssue.factoryCode", readConverterExp = "1=16")
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 施工阶段 0 无工艺 1 试制 2 量试 3 正式
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.schedulingType")
    @ApiModelProperty(value = "施工阶段 0 无工艺 1 试制 2 量试 3 正式", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /**
     * 文字示方书号
     */
    @Excel(name = "ui.data.column.FactoryMonthPlanFinalResult.textNo")
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    @TableField(value = "TEXT_NO")
    private String textNo;

}
