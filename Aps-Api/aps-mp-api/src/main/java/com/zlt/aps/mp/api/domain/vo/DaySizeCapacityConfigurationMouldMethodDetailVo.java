package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 日产能分配明细
 *
 * @author ZLT
 * @date 20250619
 */
@Data
public class DaySizeCapacityConfigurationMouldMethodDetailVo implements Serializable {
    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;
    /**
     * 成型-日产能明细
     */
    @ApiModelProperty(value = "成型-日产能明细", name = "mouldMethodList")
    List<MouldMethodMonthCycleCapacityDetailVo> mouldMethodList;
}
