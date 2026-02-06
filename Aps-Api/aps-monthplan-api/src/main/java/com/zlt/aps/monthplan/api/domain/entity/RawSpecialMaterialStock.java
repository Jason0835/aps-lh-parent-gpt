package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

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
@TableName(value = "T_MDM_SPECIAL_MATERIAL_STOCK")
public class RawSpecialMaterialStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 工厂 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.monthStock.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.monthStock.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.stockDate", dateFormat = "yyyy-MM-dd")
    @ImportExcelValidated(required = true, date = true)
    @ApiModelProperty(value = "库存日期", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    /** 物料代码 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.materialCode")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "物料代码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 物料描述 */
//    @Excel(name = "ui.data.column.rawSpecialMaterialStock.materialDesc")
//    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /** 物料名称 */
    @Excel(name = "ui.data.column.rawSpecialMaterialStock.materialName")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "物料名称", name = "materialName")
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

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

    @Excel(name = "ui.data.column.rawSpecialMaterialRecord.remark")
    @ImportExcelValidated(maxLength = 300)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
