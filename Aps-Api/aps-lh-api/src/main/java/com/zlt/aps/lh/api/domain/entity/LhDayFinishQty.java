package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhDayFinishQty.java
 * 描    述：硫化排程日完成量对象 t_lh_day_finish_qty
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "硫化排程日完成量对象", description = "硫化排程日完成量对象 ")
@Data
@TableName(value = "T_LH_DAY_FINISH_QTY")
public class LhDayFinishQty extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "年份", name = "month")
    @TableField(value = "month")
    private Integer month;

     /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.lhDayFinishQty.finishDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期", name = "finishDate")
    @TableField(value = "FINISH_DATE")
    private Date finishDate;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.lhDayFinishQty.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格代码
     */
    @Excel(name = "ui.data.column.lhDayFinishQty.specCode")
    @ApiModelProperty(value = "规格代码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 胎胚日完成量 */
    @Excel(name = "ui.data.column.lhDayFinishQty.dayFinishQty")
    @ApiModelProperty(value = "胎胚日完成量", name = "dayFinishQty")
    @TableField(value = "DAY_FINISH_QTY")
    private Integer dayFinishQty;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.lhDayFinishQty.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 版本号 */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 一班完成量
     */
    @ApiModelProperty(value = "一班完成量", name = "class1FinishQty")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.lhScheduleResult.class1FinishQty")
    private Integer class1FinishQty;

    /**
     * 二班完成量
     */
    @ApiModelProperty(value = "二班完成量", name = "class2FinishQty")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.lhScheduleResult.class2FinishQty")
    private Integer class2FinishQty;

    /**
     * 三班完成量
     */
    @ApiModelProperty(value = "三班完成量", name = "class3FinishQty")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.lhScheduleResult.class3FinishQty")
    private Integer class3FinishQty;

    /**
     * 次日一班完成量
     */
    @ApiModelProperty(value = "次日一班完成量", name = "class4FinishQty")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.lhScheduleResult.class4FinishQty")
    private Integer class4FinishQty;

    /**
     * 次日二班完成量
     */
    @ApiModelProperty(value = "次日二班完成量", name = "class5FinishQty")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.lhScheduleResult.class5FinishQty")
    private Integer class5FinishQty;

    /**
     * 次日三班完成量
     */
    @ApiModelProperty(value = "次日三班完成量", name = "class6FinishQty")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.lhScheduleResult.class6FinishQty")
    private Integer class6FinishQty;

}