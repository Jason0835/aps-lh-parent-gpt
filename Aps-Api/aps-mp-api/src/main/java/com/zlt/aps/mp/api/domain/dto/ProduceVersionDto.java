package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chen
 * @date 2025/7/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProduceVersionDto extends BaseReportDto {

    /**
     * 查询类型-1：排产量查询定稿表，其他查询生产计划排产结果表
     */
    @ApiModelProperty(value = "查询类型", name = "queryType")
    private String queryType;

    /**
     * 物料号
     */
    @ApiModelProperty(value = "物料号", name = "productCode")
    private String productCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "productDesc")
    private String productDesc;

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
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;
}
