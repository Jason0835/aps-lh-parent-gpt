package com.zlt.aps.mdm.api.domain.entity;

import java.math.BigDecimal;

import com.ruoyi.common.core.web.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmRawMaterialConversion.java
 * 描    述：成品原材料折算表 t_mdm_raw_material_conversion
 * @author zlt
 * @date 2026-07-13
 * @version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "成品原材料折算表", description = "成品原材料折算表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_MDM_RAW_MATERIAL_CONVERSION")
public class MdmRawMaterialConversion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** MES物料编码 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 标准重量 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.singleTireWeight")
    @ApiModelProperty(value = "标准重量", name = "singleTireWeight")
    @TableField(value = "SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    /** 原材料物料名称 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.rawMaterialName")
    @ApiModelProperty(value = "原材料物料名称", name = "rawMaterialName")
    @TableField(value = "RAW_MATERIAL_NAME")
    private String rawMaterialName;

    /** 原材料物料编码 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.rawMaterialCode")
    @ApiModelProperty(value = "原材料物料编码", name = "rawMaterialCode")
    @TableField(value = "RAW_MATERIAL_CODE")
    private String rawMaterialCode;

    /** 原材料物料重量 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.rawMaterialWeight")
    @ApiModelProperty(value = "原材料物料重量", name = "rawMaterialWeight")
    @TableField(value = "RAW_MATERIAL_WEIGHT")
    private BigDecimal rawMaterialWeight;

    /** 厂别 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 示方类型，T 量试、X 试制、S 正式 */
    @Excel(name = "ui.data.column.mdmRawMaterialConversion.constructionStage")
    @ApiModelProperty(value = "示方类型", name = "constructionStage")
    @TableField(value = "CONSTRUCTION_STAGE")
    private String constructionStage;
}
