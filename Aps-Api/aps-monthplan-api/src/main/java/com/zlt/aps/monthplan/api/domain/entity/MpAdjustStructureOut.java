package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureOut.java
 * 描    述：调整-结构调整记录对象 t_mp_adjust_structure_out
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "调整-结构调整记录对象", description = "调整-结构调整记录对象 ")
@Data
@TableName(value = "T_MP_ADJUST_STRUCTURE_OUT")
public class MpAdjustStructureOut extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 成型机台 */
    @Excel(name = "ui.data.column.mpAdjustStructureOut.cxMachineCode")
    @ApiModelProperty(value = "成型机台", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /** 产品结构 */
    @Excel(name = "ui.data.column.mpAdjustStructureOut.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 调整前计划量 */
    @Excel(name = "ui.data.column.mpAdjustStructureOut.beforePlanQty")
    @ApiModelProperty(value = "调整前计划量", name = "beforePlanQty")
    @TableField(value = "BEFORE_PLAN_QTY")
    private Integer beforePlanQty;

    /** 调整前开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpAdjustStructureOut.beforeStartDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "调整前开始日期", name = "beforeStartDate")
    @TableField(value = "BEFORE_START_DATE")
    private Date beforeStartDate;

    /** 调整前结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpAdjustStructureOut.beforeEndDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "调整前结束日期", name = "beforeEndDate")
    @TableField(value = "BEFORE_END_DATE")
    private Date beforeEndDate;

    /** 调整后计划量 */
    @Excel(name = "ui.data.column.mpAdjustStructureOut.afterPlanQty")
    @ApiModelProperty(value = "调整后计划量", name = "afterPlanQty")
    @TableField(value = "AFTER_PLAN_QTY")
    private Integer afterPlanQty;

    /** 调整后开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpAdjustStructureOut.afterStartDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "调整后开始日期", name = "afterStartDate")
    @TableField(value = "AFTER_START_DATE")
    private Date afterStartDate;

    /** 调整后结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mpAdjustStructureOut.afterEndDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "调整后结束日期", name = "afterEndDate")
    @TableField(value = "AFTER_END_DATE")
    private Date afterEndDate;

    /** 调整方向 */
    @Excel(name = "ui.data.column.mpAdjustStructureOut.adjustDirection")
    @ApiModelProperty(value = "调整方向", name = "adjustDirection")
    @TableField(value = "ADJUST_DIRECTION")
    private String adjustDirection;


}