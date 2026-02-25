package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductVulcanizingLimit.java
 * 描    述：基础数据-品种限制硫化机对象 t_mdm_vulcanizing_limit
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */

@Data
@TableName(value = "T_MDM_VULCANIZING_LIMIT")
@ApiModel(value = "基础数据-品种限制硫化机对象", description = "基础数据-品种限制硫化机对象 ")
public class ProductVulcanizingLimit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.ProductVulcanizingLimit.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.ProductVulcanizingLimit.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.ProductVulcanizingLimit.productCode")
    @ApiModelProperty(value = "物料编码", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 硫化机ID
     */
    @Excel(name = "ui.data.column.ProductVulcanizingLimit.vulcanizingMachineId")
    @ApiModelProperty(value = "硫化机ID", name = "vulcanizingMachineId")
    @TableField(value = "VULCANIZING_MACHINE_ID")
    private Long vulcanizingMachineId;

    /**
     * 限制生产:0-禁止生产，1-专用生产
     */
    @Excel(name = "ui.data.column.ProductVulcanizingLimit.limitType")
    @ApiModelProperty(value = "限制生产:0-禁止生产，1-专用生产", name = "limitType")
    @TableField(value = "LIMIT_TYPE")
    private Integer limitType;

}