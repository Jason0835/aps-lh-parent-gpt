package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuStructureRef.java
 * 描    述：SKU与结构关系对象 t_mdm_sku_structure_ref
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "SKU与结构关系对象", description = "SKU与结构关系对象 ")
@Data
@TableName(value = "T_MDM_SKU_STRUCTURE_REF")
public class MdmSkuStructureRef extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂编号 */
    @Excel(name = "ui.data.column.mdmSkuStructureRef.factoryCode", dictType = "biz_factory_name", sort = 1)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编码 */
    @ImportExcelValidated(required = true)
//    @Excel(name = "ui.data.column.mdmSkuStructureRef.materialCode", sort = 3)
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 主物料(胎胚描述)
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmSkuStructureRef.mainMaterialDesc", sort = 2)
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    @TableField(value = "MAIN_MATERIAL_DESC")
    private String mainMaterialDesc;

    /** 结构 */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmSkuStructureRef.structureName", sort = 5)
    @ApiModelProperty(value = "结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /** 物料描述 */
//    @Excel(name = "ui.data.column.mdmSkuStructureRef.materialDesc", sort = 4)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(exist = false)
    private String materialDesc;


}