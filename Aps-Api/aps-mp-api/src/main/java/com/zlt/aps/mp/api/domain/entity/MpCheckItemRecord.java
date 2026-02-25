package com.zlt.aps.mp.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpCheckItemRecord.java
 * 描    述：S2-1202 检测项记录对象 t_mp_check_item_record
 *@author hsc
 *@date 2026-01-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */

@ApiModel(value = "S2-1202 检测项记录对象", description = "S2-1202 检测项记录对象 ")
@Data
@TableName(value = "T_MP_CHECK_ITEM_RECORD")
public class MpCheckItemRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 年份 */
    @Excel(name = "ui.data.column.checkItemRecord.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.checkItemRecord.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 工厂编码 */
    @Excel(name = "ui.data.column.checkItemRecord.factoryCode")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 品名 */
    @Excel(name = "ui.data.column.checkItemRecord.productTypeCode")
    @ApiModelProperty(value = "品名", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 需求计划版本 */
    @Excel(name = "ui.data.column.checkItemRecord.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /** 检测项 */
    @Excel(name = "ui.data.column.checkItemRecord.checkItem")
    @ApiModelProperty(value = "检测项", name = "checkItem")
    @TableField(value = "CHECK_ITEM")
    private String checkItem;

    /** 检测内容 */
    @Excel(name = "ui.data.column.checkItemRecord.checkContent")
    @ApiModelProperty(value = "检测内容", name = "checkContent")
    @TableField(value = "CHECK_CONTENT")
    private String checkContent;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.checkItemRecord.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;
}