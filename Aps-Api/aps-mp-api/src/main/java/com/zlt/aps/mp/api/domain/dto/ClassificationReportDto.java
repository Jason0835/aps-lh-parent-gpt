package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报表管理-品牌、渠道、寸别、库位分类差异报表查询对象
 * @author Chen
 * @date 2025/3/20
 */
@ApiModel(value = "报表管理-品牌、渠道、寸别、库位分类差异报表查询对象", description = "报表管理-品牌、渠道、寸别、库位分类差异报表查询对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class ClassificationReportDto extends BaseReportDto {

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
     * 库位
     */
    @ApiModelProperty(value = "库位", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 品牌尺寸汇总类型，1=品牌，2=尺寸
     */
    @ApiModelProperty(value = "品牌尺寸汇总类型，1=品牌，2=尺寸", name = "brandProSizeSummaryType")
    private String brandProSizeSummaryType;
}
