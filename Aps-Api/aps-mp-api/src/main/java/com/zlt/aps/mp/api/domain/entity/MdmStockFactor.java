package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStockFactor.java
 * 描    述：备货系数配置对象 t_mdm_stock_factor
 *@author zlt
 *@date 2025-02-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */

@ApiModel(value = "备货系数配置对象", description = "备货系数配置对象 ")
@Data
@TableName(value = "T_MDM_STOCK_FACTOR")
public class MdmStockFactor extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 分厂编号 */
    @Excel(name = "ui.data.column.mdmStockFactor.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 库位类别，字典：biz_stor_type */
    @Excel(name = "ui.data.column.mdmStockFactor.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "库位类别，字典：biz_stor_type", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private Integer locationType;

    /** 区间值-下限 */
    @Excel(name = "ui.data.column.mdmStockFactor.valueMin")
    @ApiModelProperty(value = "区间值-下限", name = "valueMin")
    @TableField(value = "VALUE_MIN")
    private Long valueMin;

    /** 区间值-上限 */
    @Excel(name = "ui.data.column.mdmStockFactor.valueMax")
    @ApiModelProperty(value = "区间值-上限", name = "valueMax")
    @TableField(value = "VALUE_MAX")
    private Long valueMax;

    /** 系数倍数值 */
    @Excel(name = "ui.data.column.mdmStockFactor.factorValue")
    @ApiModelProperty(value = "系数倍数值", name = "factorValue")
    @TableField(value = "FACTOR_VALUE")
    private BigDecimal factorValue;

    /** 版本号 */
    @Excel(name = "ui.data.column.mdmStockFactor.version")
    @ApiModelProperty(value = "版本号", name = "version")
    @TableField(value = "VERSION")
    private Long version;

    @TableField(exist = false)
    private String remark;
}