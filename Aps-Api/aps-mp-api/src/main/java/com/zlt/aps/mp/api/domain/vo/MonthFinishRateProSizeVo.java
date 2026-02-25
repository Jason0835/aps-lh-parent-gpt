package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 */
@ApiModel(value = "报表管理，T月完成率-品牌对象", description = "报表管理，T月完成率-品牌对象")
@Data
public class MonthFinishRateProSizeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 品牌名称
     */
    @ApiModelProperty(value = "品牌名称", name = "brandName")
    private String brandName;

    /**
     * 寸别
     */
    @ApiModelProperty(value = "寸别", name = "proSize")
    private String proSize;

    /**
     * 生产月度计划数(月度计划量)
     */
    @ApiModelProperty(value = "生产月度计划数(月度计划量)", name = "producePlanQty")
    private Integer producePlanQty;

    /**
     * 生产月度完成数(硫化完成量)
     */
    @ApiModelProperty(value = "生产月度完成数(硫化完成量)", name = "produceFinishPlanQty")
    private Integer produceFinishPlanQty;

    /**
     * 生产月度完成率(%)
     */
    @ApiModelProperty(value = "生产月度完成率，小数", name = "produceFinishRate")
    private BigDecimal produceFinishRate;

    /**
     * 销售月度计划数(接口提报量)
     */
    @ApiModelProperty(value = "销售月度计划数(接口提报量)", name = "salePlanQty")
    private Integer salePlanQty;

    /**
     * 销售月度完成数(接口完成量)
     */
    @ApiModelProperty(value = "销售月度完成数(接口完成量)", name = "saleFinishPlanQty")
    private Integer saleFinishPlanQty;

    /**
     * 销售月度完成率(%)
     */
    @ApiModelProperty(value = "销售月度完成率，小数", name = "saleFinishRate")
    private BigDecimal saleFinishRate;

}
