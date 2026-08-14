package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 月计划与实际产量对比报表查询对象
 *
 * @author APS
 * @date 2026-08-13
 */
@ApiModel(value = "月计划与实际产量对比报表查询对象", description = "月计划与实际产量对比报表查询对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class MonthPlanCompareDto extends BaseReportDto {

    private static final long serialVersionUID = 1L;

    /**
     * 排产版本号（定稿版本，必选）
     */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    private String productionVersion;

    /**
     * 物料编码（模糊查询）
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述（模糊查询）
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /**
     * 产品状态（字典：trial_status）
     */
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    private String productStatus;

    /**
     * 产品品类（字典：biz_product_type）
     */
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    private String productTypeCode;

    /**
     * 当前页码（从1开始，列表查询必填，导出时不传）
     */
    @ApiModelProperty(value = "当前页码", name = "pageNum")
    private Integer pageNum;

    /**
     * 每页SKU数量（列表查询必填，导出时不传）
     */
    @ApiModelProperty(value = "每页SKU数量", name = "pageSize")
    private Integer pageSize;

    /**
     * 分页偏移量（由 Service 计算，= (pageNum-1)*pageSize，导出时不传）
     */
    @ApiModelProperty(value = "分页偏移量", name = "offset")
    private Integer offset;

    /**
     * 当前页SKU的物料键列表（materialCode|productStatus），用于限定实际产量查询范围
     */
    @ApiModelProperty(value = "当前页物料键列表", name = "materialKeys")
    private List<String> materialKeys;
}
