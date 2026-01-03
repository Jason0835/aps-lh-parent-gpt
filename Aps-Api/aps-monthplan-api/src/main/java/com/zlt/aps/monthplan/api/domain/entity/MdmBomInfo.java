package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.zlt.common.annotation.EntityMapping;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmBomInfo.java
 * 描    述：BOM示方书对象 t_mdm_bom_info
 *@author zlt
 *@date 2025-12-05
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "BOM示方书对象", description = "BOM示方书对象 ")
@Data
@TableName(value = "T_MDM_BOM_INFO")
public class MdmBomInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmBomInfo.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

     /** 子物料品号 */
    @Excel(name = "ui.data.column.mdmBomInfo.childMaterialCode")
    @ApiModelProperty(value = "子物料品号", name = "childMaterialCode")
    @TableField(value = "CHILD_MATERIAL_CODE")
    private String childMaterialCode;

    /** 子物料名称 */
    @Excel(name = "ui.data.column.mdmBomInfo.childMaterialName")
    @ApiModelProperty(value = "子物料名称", name = "childMaterialName")
    @TableField(value = "CHILD_MATERIAL_NAME")
    private String childMaterialName;

    /** 子物料名称编码(名称中文映射) */
    @Excel(name = "ui.data.column.mdmBomInfo.childMaterialNameCode")
    @ApiModelProperty(value = "子物料名称编码(名称中文映射)", name = "childMaterialNameCode")
    @TableField(value = "CHILD_MATERIAL_NAME_CODE")
    private String childMaterialNameCode;

    /** 子物料代码 */
    @Excel(name = "ui.data.column.mdmBomInfo.childCode")
    @ApiModelProperty(value = "子物料代码", name = "childCode")
    @TableField(value = "CHILD_CODE")
    private String childCode;

    /** 单位描述 */
    @Excel(name = "ui.data.column.mdmBomInfo.unit")
    @ApiModelProperty(value = "单位描述", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /** 用量，单胎消耗量 */
    @Excel(name = "ui.data.column.mdmBomInfo.dosage")
    @ApiModelProperty(value = "用量，单胎消耗量", name = "dosage")
    @TableField(value = "DOSAGE")
    private BigDecimal dosage;

    /** 组成用量，单胎需要的数量 */
    @Excel(name = "ui.data.column.mdmBomInfo.dosageForm", cellType = Excel.ColumnType.NUMERIC)
    @ImportExcelValidated(digits = true, min = 0, max = 9999)
    @ApiModelProperty(value = "组成用量，单胎需要的数量", name = "dosageForm")
    @TableField(value = "DOSAGE_FORM")
    private Integer dosageForm;

    /** 父物料品号 */
    @Excel(name = "ui.data.column.mdmBomInfo.parentMaterialCode")
    @ApiModelProperty(value = "父物料品号", name = "parentMaterialCode")
    @TableField(value = "PARENT_MATERIAL_CODE")
    private String parentMaterialCode;

    /** 父物料名称 */
    @Excel(name = "ui.data.column.mdmBomInfo.parentMaterialName")
    @ApiModelProperty(value = "父物料名称", name = "parentMaterialName")
    @TableField(value = "PARENT_MATERIAL_NAME")
    private String parentMaterialName;

    /** 父物料代码 */
    @Excel(name = "ui.data.column.mdmBomInfo.parentCode")
    @ApiModelProperty(value = "父物料代码", name = "parentCode")
    @TableField(value = "PARENT_CODE")
    private String parentCode;

    /** 父物料版本 */
    @Excel(name = "ui.data.column.mdmBomInfo.parentVersion")
    @ApiModelProperty(value = "父物料版本", name = "parentVersion")
    @TableField(value = "PARENT_VERSION")
    private String parentVersion;

    /** 生产阶段 */
    @Excel(name = "ui.data.column.mdmBomInfo.productionStage", dictType = "PRODUCTION_STAGE")
    @ApiModelProperty(value = "生产阶段", name = "productionStage")
    @TableField(value = "PRODUCTION_STAGE")
    private String productionStage;

    /** 生产阶段中文映射（0：投产阶段；1试做阶段） */
//    @Excel(name = "ui.data.column.mdmBomInfo.productionStageCode", readConverterExp = "0=：投产阶段；1：试做阶段")
    @ApiModelProperty(value = "生产阶段中文映射", name = "productionStageCode")
    @TableField(value = "PRODUCTION_STAGE_CODE")
    private String productionStageCode;

    /** BOM信息版本 */
    @Excel(name = "ui.data.column.mdmBomInfo.bomVersion")
    @ApiModelProperty(value = "BOM信息版本", name = "bomVersion")
    @TableField(value = "BOM_VERSION")
    private String bomVersion;

    /** 子物料版本 */
    @Excel(name = "ui.data.column.mdmBomInfo.childMaterialVersion")
    @ApiModelProperty(value = "子物料版本", name = "childMaterialVersion")
    @TableField(value = "CHILD_MATERIAL_VERSION")
    private String childMaterialVersion;

    /** BOM类型 */
//    @Excel(name = "ui.data.column.mdmBomInfo.bomType")
    @ApiModelProperty(value = "BOM类型", name = "bomType")
    @TableField(value = "BOM_TYPE")
    private String bomType;

    /** 状态(1正常3废止) */
    @Excel(name = "ui.data.column.mdmBomInfo.status", dictType = "BOM_STATUS")
    @ApiModelProperty(value = "状态(1正常3废止)", name = "status")
    @TableField(value = "STATUS")
    private String status;

    /** MES系统创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mdmBomInfo.mesCreateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES系统创建时间", name = "mesCreateDate")
    @TableField(value = "MES_CREATE_DATE")
    private Date mesCreateDate;

    /** MES更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.mdmBomInfo.mesUpdateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "MES更新时间", name = "mesUpdateDate")
    @TableField(value = "MES_UPDATE_DATE")
    private Date mesUpdateDate;

}