package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 日寸口产能分配明细
 *
 * @author ZLT
 * @date 20250619
 */
@Data
public class DaySizeCapacityDetailVo implements Serializable {
    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;
    /**
     * 寸口产能
     */
    @ApiModelProperty(value = "寸口产能", name = "sizeCapacityQty")
    private Long sizeCapacityQty;
}
