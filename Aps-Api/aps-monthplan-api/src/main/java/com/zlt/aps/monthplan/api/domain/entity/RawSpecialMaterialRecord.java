package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.common.domain.CommonBusiEntity;

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
@TableName(value = "T_RAW_SPECIAL_MATERIAL_RECORD")
@KeySequence(value = "SEQ_SPECIAL_MATERIAL_RECORD")
public class RawSpecialMaterialRecord extends CommonBusiEntity{

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.factoryCode")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 胶料 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.rubberSpec")
    @ApiModelProperty(value = "胶料", name = "rubberSpec")
    @TableField(value = "RUBBER_SPEC")
    private String rubberSpec;

    /** 物料编码 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 物料类型            数据字典 biz_rawMaterial_type 01 常规产品 04 特殊材料            匹配特殊原材料，则 类型 = 04 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.materialType")
    @ApiModelProperty(value = "物料类型            数据字典 biz_rawMaterial_type 01 常规产品 04 特殊材料            匹配特殊原材料，则 类型 = 04", name = "materialType")
    @TableField(value = "MATERIAL_TYPE")
    private String materialType;

    /** 定额 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.quota")
    @ApiModelProperty(value = "定额", name = "quota")
    @TableField(value = "QUOTA")
    private BigDecimal quota;

    /** 单位 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.unit")
    @ApiModelProperty(value = "单位", name = "unit")
    @TableField(value = "UNIT")
    private String unit;

}