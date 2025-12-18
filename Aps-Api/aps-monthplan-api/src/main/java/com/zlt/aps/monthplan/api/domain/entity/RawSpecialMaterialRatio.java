package com.zlt.aps.monthplan.api.domain.entity;

import java.math.BigDecimal;

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
 * 文件名称：RawSpecialMaterialRatio.java
 * 描    述：特殊材料批次比例对象 t_raw_special_material_ratio
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "特殊材料批次比例对象", description = "特殊材料批次比例对象 ")
@Data
@TableName(value = "T_RAW_SPECIAL_MATERIAL_RATIO")
public class RawSpecialMaterialRatio extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRatio.factoryCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 材料代码 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRatio.materialCode")
    @ApiModelProperty(value = "材料代码", name = "materialCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 材料名称 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRatio.materialName")
    @ApiModelProperty(value = "材料名称", name = "materialDesc")
    @ImportExcelValidated(required = true, maxLength = 100)
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 标准长 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRatio.standardLength")
    @ImportExcelValidated(required = true,  digits = true , min = 0, max = 100)
    @ApiModelProperty(value = "标准长", name = "standardLength")
    @TableField(value = "STANDARD_LENGTH")
    private Integer standardLength;

    /** 比例 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRatio.ratio")
    @ImportExcelValidated(required = true,  number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "比例", name = "ratio")
    @TableField(value = "RATIO")
    private BigDecimal ratio;

    /** 单位数据字典 biz_unit 01 米 */
    @Excel(name = "ui.data.column.rawSpecialMaterialRatio.unit")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "单位", name = "unit")
    @TableField(value = "UNIT")
    private String unit;


}