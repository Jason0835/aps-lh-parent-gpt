package com.zlt.aps.monthplan.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
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
 * 文件名称：RawSpecialMaterialStock.java
 * 描    述：特殊材料库存对象 t_raw_special_material_stock
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "特殊材料库存对象", description = "特殊材料库存对象 ")
@Data
@TableName(value = "T_RAW_SPECIAL_MATERIAL_STOCK")
public class RawSpecialMaterialStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.factoryCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料代码 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.materialCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "物料代码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.materialDesc")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 标准长 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.standardLength")
    @ImportExcelValidated(required = true,  digits = true , min = 0, max = 999999)
    @ApiModelProperty(value = "标准长", name = "standardLength")
    @TableField(value = "STANDARD_LENGTH")
    private Integer standardLength;

    /** 库存 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.stock")
    @ImportExcelValidated(required = true,  digits = true , min = 0, max = 999999)
    @ApiModelProperty(value = "库存", name = "stock")
    @TableField(value = "STOCK")
    private Integer stock;

    /** 单位 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.unit")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "单位", name = "unit")
    @TableField(value = "UNIT")
    private String unit;
}