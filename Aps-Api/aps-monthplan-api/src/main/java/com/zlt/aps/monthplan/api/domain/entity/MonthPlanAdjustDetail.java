package com.zlt.aps.monthplan.api.domain.entity;

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
 * 文件名称：MonthPlanAdjustDetail.java
 * 描    述：月计划调整明细对象 T_MP_MONTH_PLAN_ADJUST_DETAIL
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-0603
 */

@Data
@TableName(value = "T_MP_MONTH_PLAN_ADJUST_DETAIL")
@ApiModel(value = "月计划调整明细对象", description = "月计划调整明细对象")
public class MonthPlanAdjustDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;
    /**
     * 通知单号
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.noticeNo", dictType = "biz_factory_name")
    @ApiModelProperty(value = "调整通知单号", name = "noticeNo")
    @TableField(value = "NOTICE_NO")
    private String noticeNo;

    /**
     * 操作批次号
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.workNo")
    @ApiModelProperty(value = "操作批次号", name = "workNo")
    @TableField(value = "WORK_NO")
    private String workNo;

    /**
     * 排产制造单号
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.productionNo")
    @ApiModelProperty(value = "排产制造单号", name = "productionNo")
    @TableField(value = "PRODUCTION_NO")
    private String productionNo;
    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 生产物料编号
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.productCode")
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 生产规格描述
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.productDesc")
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /**
     * 开始调整日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.startDate")
    @ApiModelProperty(value = "开始调整日期", name = "startDate")
    @TableField(value = "START_DATE")
    private Date startDate;
    /**
     * 调整量
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.adjustQty")
    @ApiModelProperty(value = "调整量", name = "adjustQty")
    @TableField(value = "ADJUST_QTY")
    private Long adjustQty;

    /**
     * 调整方式 0 调减 1 调增
     */
    @Excel(name = "ui.data.column.monthPlanAdjustDetail.adjustType")
    @ApiModelProperty(value = "调整方式", name = "adjustType")
    @TableField(value = "ADJUST_TYPE")
    private Integer adjustType;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private Integer isImport;

}