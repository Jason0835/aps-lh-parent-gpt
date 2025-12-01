package com.zlt.aps.monthplan.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductInfoVo.java
 * 描    述：物料库位毛利率Vo t_mdm_product_info
 *@author zlt
 *@date 2025-02-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "物料库位毛利率Vo", description = "物料库位毛利率Vo")
public class MdmProductInfoVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.info.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 物料编号 */
    @Excel(name = "ui.data.column.info.productCode")
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /** 规格描述 */
    @Excel(name = "ui.data.column.info.productDesc")
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    @TableField(value = "PRODUCT_DESC")
    private String productDesc;

    /** 寸口（保留2位小数） */
    @Excel(name = "ui.data.column.info.proSize", readConverterExp = "保=留2位小数")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /** 品名编码 */
    @Excel(name = "ui.data.column.info.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /** 品名 */
    @Excel(name = "ui.data.column.info.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /** 模具大类 */
    @Excel(name = "ui.data.column.info.mouldCategory")
    @ApiModelProperty(value = "模具大类", name = "mouldCategory")
    @TableField(value = "MOULD_CATEGORY")
    private String mouldCategory;

    /** 硫化时间(min) */
    @Excel(name = "ui.data.column.info.curingTime")
    @ApiModelProperty(value = "硫化时间(min)", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /** 单模产能 */
    @Excel(name = "ui.data.column.info.mouldCapacity")
    @ApiModelProperty(value = "单模产能", name = "mouldCapacity")
    @TableField(value = "MOULD_CAPACITY")
    private Integer mouldCapacity;

    /** 规格 */
    @Excel(name = "ui.data.column.info.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /** 花纹 */
    @Excel(name = "ui.data.column.info.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /** 品牌 */
    @Excel(name = "ui.data.column.info.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /** 轮胎类型 取数据字典 biz_tire_type的编码 */
    @Excel(name = "ui.data.column.info.tireType")
    @ApiModelProperty(value = "轮胎类型 取数据字典 biz_tire_type的编码", name = "tireType")
    @TableField(value = "TIRE_TYPE")
    private String tireType;

    /** 公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用 */
    @Excel(name = "ui.data.column.info.commonType")
    @ApiModelProperty(value = "公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用", name = "commonType")
    @TableField(value = "COMMON_TYPE")
    private String commonType;

    /** 层级 */
    @Excel(name = "ui.data.column.info.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /** 替换品种分组 */
    @Excel(name = "ui.data.column.info.replaceGroup")
    @ApiModelProperty(value = "替换品种分组", name = "replaceGroup")
    @TableField(value = "REPLACE_GROUP")
    private String replaceGroup;

    /** 不能生产 */
    @Excel(name = "ui.data.column.info.cantProduce")
    @ApiModelProperty(value = "不能生产", name = "cantProduce")
    @TableField(value = "CANT_PRODUCE")
    private Integer cantProduce;

    /** 不能发货 */
    @Excel(name = "ui.data.column.info.noDelivery")
    @ApiModelProperty(value = "不能发货", name = "noDelivery")
    @TableField(value = "NO_DELIVERY")
    private Integer noDelivery;

    /** 速度 */
    @Excel(name = "ui.data.column.info.speed")
    @ApiModelProperty(value = "速度", name = "speed")
    @TableField(value = "SPEED")
    private String speed;

    /** 性能 */
    @Excel(name = "ui.data.column.info.ability")
    @ApiModelProperty(value = "性能", name = "ability")
    @TableField(value = "ABILITY")
    private String ability;

    /** 环保 */
    @Excel(name = "ui.data.column.info.environmentProtection")
    @ApiModelProperty(value = "环保", name = "environmentProtection")
    @TableField(value = "ENVIRONMENT_PROTECTION")
    private String environmentProtection;

    /** 认证串 */
    @Excel(name = "ui.data.column.info.authentication")
    @ApiModelProperty(value = "认证串", name = "authentication")
    @TableField(value = "AUTHENTICATION")
    private String authentication;

    /** 物料组 */
    @Excel(name = "ui.data.column.info.productGroupCode")
    @ApiModelProperty(value = "物料组", name = "productGroupCode")
    @TableField(value = "PRODUCT_GROUP_CODE")
    private String productGroupCode;

    /** 废停标志 */
    @Excel(name = "ui.data.column.info.forbidTag")
    @ApiModelProperty(value = "废停标志", name = "forbidTag")
    @TableField(value = "FORBID_TAG")
    private String forbidTag;

    /** 单胎重量 */
    @Excel(name = "ui.data.column.info.singleTireWeight")
    @ApiModelProperty(value = "单胎重量", name = "singleTireWeight")
    @TableField(value = "SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    /** 合模压力 */
    @Excel(name = "ui.data.column.info.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;
}