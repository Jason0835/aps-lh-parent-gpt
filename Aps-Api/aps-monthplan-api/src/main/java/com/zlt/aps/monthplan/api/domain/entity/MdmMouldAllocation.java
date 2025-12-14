package com.zlt.aps.monthplan.api.domain.entity;

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
 * 文件名称：MdmMouldAllocation.java
 * 描    述：模具分配比例(同结构/不同结构)对象 t_mdm_mould_allocation
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "模具分配比例(同结构/不同结构)对象", description = "模具分配比例(同结构/不同结构)对象 ")
@Data
@TableName(value = "T_MDM_MOULD_ALLOCATION")
public class MdmMouldAllocation extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 主花纹 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /** 花纹 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 年份 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 产品品类 biz_product_type TBR 全钢 PCR 半钢 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类 biz_product_type TBR 全钢 PCR 半钢", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 结构 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.structrueName")
    @ApiModelProperty(value = "结构", name = "structrueName")
    @TableField(value = "STRUCTRUE_NAME")
    private String structrueName;

    /** 规格 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 模具分配数 */
    @Excel(name = "ui.data.column.mdmMouldAllocation.allocationQty")
    @ApiModelProperty(value = "模具分配数", name = "allocationQty")
    @TableField(value = "ALLOCATION_QTY")
    private Integer allocationQty;


}