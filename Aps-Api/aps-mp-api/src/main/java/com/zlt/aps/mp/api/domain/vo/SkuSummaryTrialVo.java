package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/14
 */
@Data
public class SkuSummaryTrialVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 月份，如果是13=年累计、14=H1、15=环比
     */
    @ApiModelProperty(value = "月份，如果是13=年累计、14=H1、15=环比", name = "month")
    private Integer month;

    /**
     * 合计(个)
     */
    @ApiModelProperty(value = "合计(个)", name = "totalCount")
    private BigDecimal totalCount;

    /**
     * 合计(条)
     */
    @ApiModelProperty(value = "合计(条)", name = "totalSum")
    private BigDecimal totalSum;

    /**
     * 生产1天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产1天的sku数(个)", name = "dayCount1")
    private BigDecimal dayCount1;

    /**
     * 生产2天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产2天的sku数(个)", name = "dayCount2")
    private BigDecimal dayCount2;

    /**
     * 生产3天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产3天的sku数(个)", name = "dayCount3")
    private BigDecimal dayCount3;

    /**
     * 生产4天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产4天的sku数(个)", name = "dayCount4")
    private BigDecimal dayCount4;

    /**
     * 生产5天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产5天的sku数(个)", name = "dayCount5")
    private BigDecimal dayCount5;

    /**
     * 生产6天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产6天的sku数(个)", name = "dayCount6")
    private BigDecimal dayCount6;

    /**
     * 生产7天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产7天的sku数(个)", name = "dayCount7")
    private BigDecimal dayCount7;

    /**
     * 生产8天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产8天的sku数(个)", name = "dayCount8")
    private BigDecimal dayCount8;

    /**
     * 生产9天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产9天的sku数(个)", name = "dayCount9")
    private BigDecimal dayCount9;

    /**
     * 生产10天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产10天的sku数(个)", name = "dayCount10")
    private BigDecimal dayCount10;

    /**
     * 生产11天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产11天的sku数(个)", name = "dayCount11")
    private BigDecimal dayCount11;

    /**
     * 生产12天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产12天的sku数(个)", name = "dayCount12")
    private BigDecimal dayCount12;

    /**
     * 生产13天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产13天的sku数(个)", name = "dayCount13")
    private BigDecimal dayCount13;

    /**
     * 生产14天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产14天的sku数(个)", name = "dayCount14")
    private BigDecimal dayCount14;

    /**
     * 生产15天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产15天的sku数(个)", name = "dayCount15")
    private BigDecimal dayCount15;

    /**
     * 生产16天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产16天的sku数(个)", name = "dayCount16")
    private BigDecimal dayCount16;

    /**
     * 生产17天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产17天的sku数(个)", name = "dayCount17")
    private BigDecimal dayCount17;

    /**
     * 生产18天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产18天的sku数(个)", name = "dayCount18")
    private BigDecimal dayCount18;

    /**
     * 生产19天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产19天的sku数(个)", name = "dayCount19")
    private BigDecimal dayCount19;

    /**
     * 生产20天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产20天的sku数(个)", name = "dayCount20")
    private BigDecimal dayCount20;

    /**
     * 生产21天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产21天的sku数(个)", name = "dayCount21")
    private BigDecimal dayCount21;

    /**
     * 生产22天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产22天的sku数(个)", name = "dayCount22")
    private BigDecimal dayCount22;

    /**
     * 生产23天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产23天的sku数(个)", name = "dayCount23")
    private BigDecimal dayCount23;

    /**
     * 生产24天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产24天的sku数(个)", name = "dayCount24")
    private BigDecimal dayCount24;

    /**
     * 生产25天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产25天的sku数(个)", name = "dayCount25")
    private BigDecimal dayCount25;

    /**
     * 生产26天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产26天的sku数(个)", name = "dayCount26")
    private BigDecimal dayCount26;

    /**
     * 生产27天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产27天的sku数(个)", name = "dayCount27")
    private BigDecimal dayCount27;

    /**
     * 生产28天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产28天的sku数(个)", name = "dayCount28")
    private BigDecimal dayCount28;

    /**
     * 生产29天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产29天的sku数(个)", name = "dayCount29")
    private BigDecimal dayCount29;

    /**
     * 生产30天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产30天的sku数(个)", name = "dayCount30")
    private BigDecimal dayCount30;

    /**
     * 生产31天的SKU数量，单位为个
     */
    @ApiModelProperty(value = "生产31天的sku数(个)", name = "dayCount31")
    private BigDecimal dayCount31;

    /**
     * 生产1天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产1天的sku数(条)", name = "daySum1")
    private BigDecimal daySum1;

    /**
     * 生产2天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产2天的sku数(条)", name = "daySum2")
    private BigDecimal daySum2;

    /**
     * 生产3天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产3天的sku数(条)", name = "daySum3")
    private BigDecimal daySum3;

    /**
     * 生产4天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产4天的sku数(条)", name = "daySum4")
    private BigDecimal daySum4;

    /**
     * 生产5天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产5天的sku数(条)", name = "daySum5")
    private BigDecimal daySum5;

    /**
     * 生产6天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产6天的sku数(条)", name = "daySum6")
    private BigDecimal daySum6;

    /**
     * 生产7天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产7天的sku数(条)", name = "daySum7")
    private BigDecimal daySum7;

    /**
     * 生产8天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产8天的sku数(条)", name = "daySum8")
    private BigDecimal daySum8;

    /**
     * 生产9天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产9天的sku数(条)", name = "daySum9")
    private BigDecimal daySum9;

    /**
     * 生产10天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产10天的sku数(条)", name = "daySum10")
    private BigDecimal daySum10;

    /**
     * 生产11天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产11天的sku数(条)", name = "daySum11")
    private BigDecimal daySum11;

    /**
     * 生产12天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产12天的sku数(条)", name = "daySum12")
    private BigDecimal daySum12;

    /**
     * 生产13天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产13天的sku数(条)", name = "daySum13")
    private BigDecimal daySum13;

    /**
     * 生产14天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产14天的sku数(条)", name = "daySum14")
    private BigDecimal daySum14;

    /**
     * 生产15天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产15天的sku数(条)", name = "daySum15")
    private BigDecimal daySum15;

    /**
     * 生产16天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产16天的sku数(条)", name = "daySum16")
    private BigDecimal daySum16;

    /**
     * 生产17天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产17天的sku数(条)", name = "daySum17")
    private BigDecimal daySum17;

    /**
     * 生产18天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产18天的sku数(条)", name = "daySum18")
    private BigDecimal daySum18;

    /**
     * 生产19天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产19天的sku数(条)", name = "daySum19")
    private BigDecimal daySum19;

    /**
     * 生产20天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产20天的sku数(条)", name = "daySum20")
    private BigDecimal daySum20;

    /**
     * 生产21天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产21天的sku数(条)", name = "daySum21")
    private BigDecimal daySum21;

    /**
     * 生产22天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产22天的sku数(条)", name = "daySum22")
    private BigDecimal daySum22;

    /**
     * 生产23天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产23天的sku数(条)", name = "daySum23")
    private BigDecimal daySum23;

    /**
     * 生产24天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产24天的sku数(条)", name = "daySum24")
    private BigDecimal daySum24;

    /**
     * 生产25天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产25天的sku数(条)", name = "daySum25")
    private BigDecimal daySum25;

    /**
     * 生产26天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产26天的sku数(条)", name = "daySum26")
    private BigDecimal daySum26;

    /**
     * 生产27天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产27天的sku数(条)", name = "daySum27")
    private BigDecimal daySum27;

    /**
     * 生产28天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产28天的sku数(条)", name = "daySum28")
    private BigDecimal daySum28;

    /**
     * 生产29天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产29天的sku数(条)", name = "daySum29")
    private BigDecimal daySum29;

    /**
     * 生产30天的SKU数量，单位为条
     */
    @ApiModelProperty(value = "生产31天的sku数(条)", name = "daySum31")
    private BigDecimal daySum31;

}
