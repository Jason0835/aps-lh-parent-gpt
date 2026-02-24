package com.zlt.aps.mdm.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialInfoGrossRateVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMaterialInfo.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编号
     */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.mdmMaterialInfo.materialCode", sort = 2)
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * MES物料编号
     */
    @ImportExcelValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.mdmMaterialInfo.mesMaterialCode", sort = 2)
    @ApiModelProperty(value = "MES物料编号", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 规格描述
     */
    @ImportExcelValidated(required = true, maxLength = 256)
    @Excel(name = "ui.data.column.mdmMaterialInfo.materialDesc", sort = 3)
    @ApiModelProperty(value = "规格描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 寸口（保留2位小数）
     */
//    @Excel(name = "ui.data.column.info.proSize", readConverterExp = "保=留2位小数")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 品名编码
     */
//    @Excel(name = "ui.data.column.info.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
//    @Excel(name = "ui.data.column.info.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 模具大类
     */
//    @Excel(name = "ui.data.column.info.mouldCategory")
    @ApiModelProperty(value = "模具大类", name = "mouldCategory")
    @TableField(value = "MOULD_CATEGORY")
    private String mouldCategory;

    /**
     * 机械硫化时间(秒)
     */
    @ApiModelProperty(value = "机械硫化时间(秒)", name = "curingTime")
    @TableField(value = "CURING_TIME")
    private Integer curingTime;

    /**
     * 液压硫化时间(秒)
     */
    @ApiModelProperty(value = "液压硫化时间(秒)", name = "hydraulicPressureCuringTime")
    @TableField(value = "HYDRAULIC_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    /**
     * 单模产能
     */
//    @Excel(name = "ui.data.column.info.mouldCapacity")
    @ApiModelProperty(value = "单模产能", name = "mouldCapacity")
    @TableField(value = "MOULD_CAPACITY")
    private Integer mouldCapacity;

    /**
     * 规格
     */
//    @Excel(name = "ui.data.column.info.specifications")
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
//    @Excel(name = "ui.data.column.info.pattern")
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 品牌
     */
//    @Excel(name = "ui.data.column.info.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 轮胎类型 取数据字典 biz_tire_type的编码
     */
//    @Excel(name = "ui.data.column.info.tireType")
    @ApiModelProperty(value = "轮胎类型 取数据字典 biz_tire_type的编码", name = "tireType")
    @TableField(value = "TIRE_TYPE")
    private String tireType;

    /**
     * 公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMaterialInfo.commonType", dictType = "biz_common_type")
    @ApiModelProperty(value = "公用类型 取数据字典 biz_common_type的编码 1 公用规格 2 外销专用 3 内销专用 4 OE专用", name = "commonType")
    @TableField(value = "COMMON_TYPE")
    private String commonType;

    /**
     * 层级
     */
//    @Excel(name = "ui.data.column.info.hierarchy")
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 替换品种分组
     */
//    @Excel(name = "ui.data.column.info.replaceGroup")
    @ApiModelProperty(value = "替换品种分组", name = "replaceGroup")
    @TableField(value = "REPLACE_GROUP")
    private String replaceGroup;

    /**
     * 不能生产
     */
//    @Excel(name = "ui.data.column.info.cantProduce")
    @ApiModelProperty(value = "不能生产", name = "cantProduce")
    @TableField(value = "CANT_PRODUCE")
    private Integer cantProduce;

    /**
     * 不能发货
     */
//    @Excel(name = "ui.data.column.info.noDelivery")
    @ApiModelProperty(value = "不能发货", name = "noDelivery")
    @TableField(value = "NO_DELIVERY")
    private Integer noDelivery;

    /**
     * 速度
     */
//    @Excel(name = "ui.data.column.info.speed")
    @ApiModelProperty(value = "速度", name = "speed")
    @TableField(value = "SPEED")
    private String speed;

    /**
     * 性能
     */
//    @Excel(name = "ui.data.column.info.ability")
    @ApiModelProperty(value = "性能", name = "ability")
    @TableField(value = "ABILITY")
    private String ability;

    /**
     * 环保
     */
//    @Excel(name = "ui.data.column.info.environmentProtection")
    @ApiModelProperty(value = "环保", name = "environmentProtection")
    @TableField(value = "ENVIRONMENT_PROTECTION")
    private String environmentProtection;

    /**
     * 认证串
     */
//    @Excel(name = "ui.data.column.info.authentication")
    @ApiModelProperty(value = "认证串", name = "authentication")
    @TableField(value = "AUTHENTICATION")
    private String authentication;

    /**
     * 物料组
     */
//    @Excel(name = "ui.data.column.mdmMaterialInfo.productGroupCode")
    @ApiModelProperty(value = "物料组", name = "materialGroupCode")
    @TableField(value = "MATERIAL_GROUP_CODE")
    private String materialGroupCode;

    /**
     * 废停标志
     */
//    @Excel(name = "ui.data.column.info.forbidTag")
    @ApiModelProperty(value = "废停标志", name = "forbidTag")
    @TableField(value = "FORBID_TAG")
    private String forbidTag;

    /**
     * 单胎重量
     */
//    @Excel(name = "ui.data.column.info.singleTireWeight")
    @ApiModelProperty(value = "单胎重量", name = "singleTireWeight")
    @TableField(value = "SINGLE_TIRE_WEIGHT")
    private BigDecimal singleTireWeight;

    /**
     * 合模压力            PA
     */
//    @Excel(name = "ui.data.column.info.mouldClampingPressure")
    @ApiModelProperty(value = "合模压力            PA", name = "mouldClampingPressure")
    @TableField(value = "MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /**
     * 毛利率Json
     */
    @ApiModelProperty(value = "毛利率Json", name = "grossRateJson")
    @TableField(value = "GROSS_RATE_JSON")
    private String grossRateJson;

    /**
     * 外销毛利率
     */
    @Excel(name = "ui.data.column.info.outGrossRate", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "外销毛利率", name = "outGrossRate")
    @TableField(exist = false)
    private BigDecimal outGrossRate;

    /**
     * 内销毛利率
     */
    @Excel(name = "ui.data.column.info.inGrossRate", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "内销毛利率", name = "inGrossRate")
    @TableField(exist = false)
    private BigDecimal inGrossRate;

    /**
     * OE毛利率
     */
    @Excel(name = "ui.data.column.info.oeGrossRate", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "OE毛利率", name = "oeGrossRate")
    @TableField(exist = false)
    private BigDecimal oeGrossRate;
}
