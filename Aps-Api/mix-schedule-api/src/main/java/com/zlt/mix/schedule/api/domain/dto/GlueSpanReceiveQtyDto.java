package com.zlt.mix.schedule.api.domain.dto;

import com.zlt.mix.common.core.domain.ZltBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 跨区接收请求用于传输机台各班次计划总量对象
 * @author: Chen
 * @since: 2022/8/22 9:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value="跨区接收请求用于传输机台各班次计划总量对象", description="跨区接收请求用于传输机台各班次计划总量对象")
public class GlueSpanReceiveQtyDto extends ZltBaseDto {

    /**
     * 中班计划总量
     */
    @ApiModelProperty(value = "中班计划总量", position = 10)
    private Double totalMidPlanQty = BigDecimal.ZERO.doubleValue();

    /**
     * 夜班计划总量
     */
    @ApiModelProperty(value = "夜班计划总量", position = 10)
    private Double totalNightPlanQty = BigDecimal.ZERO.doubleValue();

    /**
     * 白班计划总量
     */
    @ApiModelProperty(value = "白班计划总量", position = 10)
    private Double totalDayPlanQty = BigDecimal.ZERO.doubleValue();
}
