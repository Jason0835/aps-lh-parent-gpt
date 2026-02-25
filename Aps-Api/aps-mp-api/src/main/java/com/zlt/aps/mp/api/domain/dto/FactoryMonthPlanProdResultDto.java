package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分厂排产月计划结果查询条件对象
 *
 * @author ZLT
 * @date 20250320
 */
@Data
@ApiModel(value = "分厂排产月计划结果查询条件对象", description = "分厂排产月计划结果查询条件对象")
public class FactoryMonthPlanProdResultDto implements Serializable {
    /**
     * 生产分厂编号
     */
    @ApiModelProperty(required = true, value = "生产分厂编号", name = "factoryCode")
    private String factoryCode;
    /**
     * 年份
     */
    @ApiModelProperty(required = true, value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(required = true, value = "月份", name = "month")
    private Integer month;
    /**
     * 生产物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;
    /**
     * 生产规格描述
     */
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    private String productDesc;
    /**
     * 库位类别
     */
    @ApiModelProperty(value = "库位类别", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;
    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    private String hierarchy;
    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;

    /**
     * 模具
     */
    @ApiModelProperty(value = "模具", name = "mouldNo")
    private String mouldNo;
    /**
     * 生胎代码
     */
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;
}
