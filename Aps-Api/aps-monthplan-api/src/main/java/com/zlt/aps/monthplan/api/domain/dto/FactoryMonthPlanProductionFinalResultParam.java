package com.zlt.aps.monthplan.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工厂月度计划排产结果查询条件对象
 *
 * @author ZLT
 * @date 20260112
 */
@Data
@ApiModel(value = "工厂月度计划排产结果查询条件对象", description = "工厂月度计划排产结果查询条件对象")
public class FactoryMonthPlanProductionFinalResultParam implements Serializable {
    /**
     * 工厂编号
     */
    @ApiModelProperty(required = true, value = "工厂编号", name = "factoryCode")
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
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;
    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;
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
    private String proSize;
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
     * 生胎代码
     */
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;
    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    private String structureName;
    /**
     * 排产版本号
     */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    private String productionVersion;
}
