package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/27
 */
@Data
public class TireTypeSingleMouldVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;

    /**
     * 模具号
     */
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    private String mouldCode;

    /**
     * 订单号，逗号分隔
     */
    @ApiModelProperty(value = "订单号，逗号分隔", name = "orderNo")
    private String orderNo;

    /**
     * 未排产原因，逗号分隔
     */
    @ApiModelProperty(value = "未排产原因，逗号分隔", name = "reason")
    private String reason;

    /**
     * 数量
     */
    @ApiModelProperty(value = "数量", name = "count")
    private Integer count;

    /**
     * 需求量
     */
    @ApiModelProperty(value = "需求量", name = "qty")
    private BigDecimal qty;
}
