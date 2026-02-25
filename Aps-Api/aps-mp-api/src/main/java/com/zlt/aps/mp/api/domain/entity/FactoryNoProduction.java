package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryNoProduction.java
 * 描    述：基础数据-分厂不排产对象 t_mdm_factory_no_production
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-26
 */

@ApiModel(value = "基础数据-分厂不排产对象", description = "基础数据-分厂不排产对象 ")
@Data
@TableName(value = "T_MDM_FACTORY_NO_PRODUCTION")
public class FactoryNoProduction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.factoryNoProduction.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.factoryNoProduction.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.factoryNoProduction.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.factoryNoProduction.productCode")
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 品名
     */
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(exist = false)
    private String productTypeName;

    /**
     * 产品描述
     */
    @Excel(name = "ui.data.column.factoryNoProduction.productDesc")
    @ApiModelProperty(value = "产品描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

}
