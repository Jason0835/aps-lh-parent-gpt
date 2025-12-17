package com.zlt.aps.monthplan.api.domain.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSkuProductionType.java
 * 描    述：SKU排产分类对象 t_mp_sku_production_type
 *@author yelq
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */

@ApiModel(value = "SKU排产分类对象", description = "SKU排产分类对象 ")
@Data
@TableName(value = "t_mp_sku_production_type")
public class MpSkuProductionType extends BaseEntity{

    private static final long serialVersionUID = 1L;

     /** MES物料编码 */
    @Excel(name = "ui.data.column.skuProductionType.mesMaterialCode")
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /** 物料编码 */
    @Excel(name = "ui.data.column.skuProductionType.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
    @Excel(name = "ui.data.column.skuProductionType.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 生成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.skuProductionType.generateDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "生成日期", name = "generateDate")
    @TableField(value = "GENERATE_DATE")
    private Date generateDate;

    /** 排产类型 */
    @Excel(name = "ui.data.column.skuProductionType.productionType")
    @ApiModelProperty(value = "排产类型", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

}