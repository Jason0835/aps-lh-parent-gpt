package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/7/30
 */
@Data
public class OrderPlanAllocationReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提报Sku个数
     */
    @ApiModelProperty(value = "提报Sku个数", name = "skuCount")
    private Double skuCount = 0D;

    /**
     * 提报Sku总量
     */
    @ApiModelProperty(value = "提报Sku总量", name = "skuQty")
    private Double skuQty = 0D;

    /**
     * 对冲前库存总量
     */
    @ApiModelProperty(value = "对冲前库存总量", name = "beforeHedgingStockQty")
    private Double beforeHedgingStockQty = 0D;

    /**
     * 库存对冲量
     */
    @ApiModelProperty(value = "库存对冲量", name = "stockHedgingQty")
    private Double stockHedgingQty = 0D;

    /**
     * 缺口量
     */
    @ApiModelProperty(value = "缺口量", name = "gapQty")
    private Double gapQty = 0D;
}
