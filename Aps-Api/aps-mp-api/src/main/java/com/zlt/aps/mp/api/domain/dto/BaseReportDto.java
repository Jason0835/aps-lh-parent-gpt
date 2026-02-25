package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/3/20
 */
@Data
public class BaseReportDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "分厂，字典：biz_factory_name", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月", name = "month")
    private Integer month;

    /**
     * 终稿版本
     */
    @ApiModelProperty(value = "终稿版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 品牌尺寸汇总类型，1=品牌，2=尺寸
     */
    @ApiModelProperty(value = "品牌尺寸汇总类型，1=品牌，2=尺寸", name = "brandProSizeSummaryType")
    private String brandProSizeSummaryType;
}
