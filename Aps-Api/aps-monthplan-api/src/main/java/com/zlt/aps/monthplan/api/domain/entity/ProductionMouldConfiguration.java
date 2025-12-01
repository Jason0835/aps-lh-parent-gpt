package com.zlt.aps.monthplan.api.domain.entity;

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
 * 文件名称：ProductionMouldConfiguration.java
 * 描    述：模具正在生产的品种对象 t_mdm_production_mould
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */

@Data
@TableName(value = "T_MDM_PRODUCTION_MOULD")
@ApiModel(value = "模具正在生产的品种对象", description = "模具正在生产的品种对象 ")
public class ProductionMouldConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编号
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.productCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.mouldCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 规格代号
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.specCode")
    @ApiModelProperty(value = "规格代号", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 胎胚号
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.embryoCode")
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;
    /**
     * 排产分组
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.productionGroupValue")
    @ApiModelProperty(value = "排产分组", name = "productionGroupValue")
    @TableField(value = "PRODUCTION_GROUP_VALUE")
    private String productionGroupValue;

    /**
     * 排产分组-排产模台数
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.mouldQty")
    @ApiModelProperty(value = "排产模台数", name = "mouldQty")
    @TableField(value = "MOULD_QTY")
    private Integer mouldQty;
    /**
     * 排产分组-本身模台数
     */
    @Excel(name = "ui.data.column.productionMouldConfiguration.mouldNumber")
    @ApiModelProperty(value = "本身模台数", name = "mouldNumber")
    @TableField(value = "MOULD_NUMBER")
    private Integer mouldNumber;

    /**
     * 获取重复key值
     * 分厂 物料编码 模具号， 硫化规格代号
     *
     * @return
     */
    public String getDuplicateKey() {
        String duplicateFormat = "%s|*|%s|*|%s|*|%s";
        return String.format(duplicateFormat, factoryCode, productCode, mouldCode, specCode);
    }
}