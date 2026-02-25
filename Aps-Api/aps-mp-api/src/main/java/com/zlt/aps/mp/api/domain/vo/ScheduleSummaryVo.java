package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chen
 * @date 2025/6/12
 */
@Data
public class ScheduleSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排程日期
     */
    @ApiModelProperty(value = "排程日期")
    private String scheduleDate;

    /**
     * 昨日早班合计
     */
    @ApiModelProperty(value = "昨日早班合计")
    private Double lastDayPlanQty;

    /**
     * 夜班合计
     */
    @ApiModelProperty(value = "夜班合计")
    private Double dayPlanQty;

    /**
     * 早班合计
     */
    @ApiModelProperty(value = "早班合计")
    private Double nightPlanQty;

    /**
     * 次日夜班合计
     */
    @ApiModelProperty(value = "次日夜班合计")
    private Double nextDayPlanQty;

    /**
     * 库存合计
     */
    @ApiModelProperty(value = "库存合计")
    private Double stockQty;

    /**
     * 库存2合计
     */
    @ApiModelProperty(value = "库存2合计")
    private Double stockQty2;

    /**
     * 成型消耗合计
     */
    @ApiModelProperty(value = "成型消耗合计")
    private Double cxConsumeQty;

    /**
     * 理论交班库存
     */
    @ApiModelProperty(value = "理论交班库存")
    private Double theoreticClassStockQty;
}
