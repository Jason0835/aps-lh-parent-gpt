package com.zlt.aps.mp.api.domain.entity;

import java.math.BigDecimal;

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
 * 文件名称：RawSpecialMaterialRecord.java
 * 描    述：特殊材料清单对象 t_raw_special_material_record
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "特殊材料清单对象", description = "特殊材料清单对象 ")
@Data
@TableName(value = "T_MDM_SPECIAL_MATERIAL_RECORD")
public class RawSpecialMaterialRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 适用范围 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.rubberSpec")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "适用范围", name = "rubberSpec")
    @TableField(value = "RUBBER_SPEC")
    private String rubberSpec;

    /** 物料编码 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialDesc")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 物料类型            数据字典 biz_rawMaterial_type 01 常规产品 04 特殊材料            匹配特殊原材料，则 类型 = 04 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialType",dictType = "biz_rawMaterial_type")
    @ImportExcelValidated(required = true, dictType = "biz_rawMaterial_type")
    @ApiModelProperty(value = "物料类型 biz_rawMaterial_type")
    @TableField(value = "MATERIAL_TYPE")
    private String materialType;

    /** 定额 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.quota")
    @ImportExcelValidated(required = true,  number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "定额", name = "quota")
    @TableField(value = "QUOTA")
    private BigDecimal quota;

    /** 单位 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.unit")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "单位", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

    /** 部件名称 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.partName")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "部件名称", name = "partName")
    @TableField(value = "PART_NAME")
    private String partName;


    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.remark")
    @ImportExcelValidated(maxLength = 300)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}