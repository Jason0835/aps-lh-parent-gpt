package com.zlt.aps.monthplan.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chen
 */
@ApiModel(value = "报表管理，报表查询对象", description = "报表管理，报表查询对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MonthPlanReportDto extends BaseReportDto {

    /**
     * 投产阶段，0=投产，1=试制
     */
    @ApiModelProperty(value = "投产阶段，0=投产，1=试制", name = "productionStage")
    private String productionStage;

    /**
     * 品牌尺寸汇总类型，1=品牌，2=尺寸
     */
    @ApiModelProperty(value = "品牌尺寸汇总类型，1=品牌，2=尺寸", name = "brandProSizeSummaryType")
    private String brandProSizeSummaryType;
}
