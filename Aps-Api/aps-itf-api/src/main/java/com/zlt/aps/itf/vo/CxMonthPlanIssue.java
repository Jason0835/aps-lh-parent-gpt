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
 * 描    述：成型月计划下发对象 cx_month_plan_issue
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
@ApiModel(value = "成型月计划下发对象", description = "成型月计划下发对象")
@Data
@TableName(value = "CX_MONTH_PLAN_ISSUE")
public class CxMonthPlanIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "mpMonth")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 示方类型
     */
    @ApiModelProperty(value = "示方类型", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;

    /**
     * 需求量
     */
    @ApiModelProperty(value = "需求量", name = "demandQty")
    @TableField(value = "DEMAND_QTY")
    private BigDecimal demandQty;

    /**
     * 版本号
     */
    @Excel(name = "ui.data.column.monthPlanIssue.dataVersion")
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField(value = "REMARK")
    private String remark;
}
