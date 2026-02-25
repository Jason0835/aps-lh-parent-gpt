package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 寸口成型法月周期产能明细
 * 即第1天~第n天
 * 正常为1~31
 *
 * @author ZLT
 * @date 2025731
 */
@Data
public class MouldMethodMonthCycleCapacityDetailVo extends MonthCycleDayVo {

    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    private String mouldMethod;

}
