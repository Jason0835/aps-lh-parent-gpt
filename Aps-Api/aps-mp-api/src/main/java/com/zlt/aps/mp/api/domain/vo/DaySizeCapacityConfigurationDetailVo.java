package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 日产能分配明细
 *
 * @author ZLT
 * @date 20250619
 */
@Data
public class DaySizeCapacityConfigurationDetailVo implements Serializable {
    /**
     * 天数
     */
    @ApiModelProperty(value = "天数", name = "day")
    private Integer day;
    /**
     * 天总产能
     */
    @ApiModelProperty(value = "天总产能", name = "sumCapacityQty")
    private Long sumCapacityQty;

    /**
     * 天产能明细
     */
    @ApiModelProperty(value = "天产能明细", name = "detail")
    private List<DaySizeCapacityDetailVo> detail;
}
