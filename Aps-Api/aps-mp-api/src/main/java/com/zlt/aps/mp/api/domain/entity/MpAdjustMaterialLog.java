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
 * 文件名称：MpAdjustMaterialLog.java
 * 描    述：S2-0808.调整-调整日志（未调整及已调整）对象 t_mp_adjust_material_log
 *@author zlt
 *@date 2026-02-09
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "调整-调整日志（未调整及已调整）对象", description = "调整-调整日志（未调整及已调整）对象 ")
@Data
@TableName(value = "T_MP_ADJUST_MATERIAL_LOG")
public class MpAdjustMaterialLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 年份 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /** 月份 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /** 排产版本号 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.productionVersion")
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /** 版本规则：ADJ+年月日+3位流水号； */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.adjVersion")
    @ApiModelProperty(value = "版本规则：ADJ+年月日+3位流水号；", name = "adjVersion")
    @TableField(value = "ADJ_VERSION")
    private String adjVersion;

    @Excel(name = "ui.data.column.mpAdjustMaterialLog.adjustType")
    @ApiModelProperty(value = "调整类型：调整类型01-结构内，02-结构延长，03-结构缩短，04-新增结构", name = "adjustType")
    @TableField(value = "ADJUST_TYPE")
    private String adjustType;

    /** 产品结构 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 物料编码 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 是否含特殊材料            2、0-否，1-是 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.hasSpecialMaterial")
    @ApiModelProperty(value = "是否含特殊材料            2、0-否，1-是", name = "hasSpecialMaterial")
    @TableField(value = "HAS_SPECIAL_MATERIAL")
    private String hasSpecialMaterial;

    /** 调整明细 */
    @Excel(name = "ui.data.column.mpAdjustMaterialLog.adjustDetail")
    @ApiModelProperty(value = "调整明细", name = "adjustDetail")
    @TableField(value = "ADJUST_DETAIL")
    private String adjustDetail;


}