package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuConstructionRef.java
 * 描    述：SKU与施工（示方书）关系对象 t_mdm_sku_construction_ref
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "SKU与施工（示方书）关系对象", description = "SKU与施工（示方书）关系对象 ")
@Data
@TableName(value = "T_MDM_SKU_CONSTRUCTION_REF")
public class MdmSkuConstructionRef extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 规格代号 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 施工代号 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.constructionCode")
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    @TableField(value = "CONSTRUCTION_CODE")
    private String constructionCode;

    /** 胎胚号 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.embryoCode")
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /** 生产版本 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.productionVersion")
    @ApiModelProperty(value = "生产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 成型法 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.mouldMethod")
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    @TableField(value = "MOULD_METHOD")
    private String mouldMethod;

    /** BOM版本 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.bomVersion")
    @ApiModelProperty(value = "BOM版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /** 合模压力 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /** 模具型腔 */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.mouldCavity")
    @ApiModelProperty(value = "模具型腔", name = "mouldCavity")
    @TableField(value = "MOULD_CAVITY")
    private String mouldCavity;

    /** 夏季机械硫化时间(秒) */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.curingTime")
    @ApiModelProperty(value = "夏季机械硫化时间(秒)", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /** 夏季液压硫化时间(秒) */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.hydraulicPressureCuringTime")
    @ApiModelProperty(value = "夏季液压硫化时间(秒)", name = "hydraulicPressureCuringTime")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    /** 冬季机械硫化时间(秒) */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.curingTime2")
    @ApiModelProperty(value = "冬季机械硫化时间(秒)", name = "curingTime2")
    @TableField(value = "CURING_TIME2")
    private Integer curingTime2;

    /** 冬季液压硫化时间(秒) */
    @Excel(name = "ui.data.column.mdmSkuConstructionRef.hydraulicPressureCuringTime2")
    @ApiModelProperty(value = "冬季液压硫化时间(秒)", name = "hydraulicPressureCuringTime2")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME2")
    private Integer hydraulicPressureCuringTime2;


}