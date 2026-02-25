package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 排产计划切换硫化规格代码
 *
 * @author ZLT
 * @date 20250416
 */
@Data
public class ChangeSpecCodeMouldingDayResultParam implements Serializable {
    /**
     * 排产计划ID
     */
    @ApiModelProperty(value = "排产计划ID", name = "productionId")
    private Long productionId;
    /**
     * 切换后的规格代号
     */
    @ApiModelProperty(value = "切换后的规格代号", name = "specCode")
    private String specCode;
}
