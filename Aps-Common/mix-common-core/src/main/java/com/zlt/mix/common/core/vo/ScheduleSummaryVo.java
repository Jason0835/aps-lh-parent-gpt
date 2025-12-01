package com.zlt.mix.common.core.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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
    private Date scheduleDate;

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
     * 库存合计
     */
    @ApiModelProperty(value = "库存合计")
    private Double stockQty;

    /**
     * 半部件消耗合计
     */
    @ApiModelProperty(value = "半部件消耗合计")
    private Double cxConsumeQty;

    /**
     * 理论交班库存
     */
    @ApiModelProperty(value = "理论交班库存")
    private Double theoreticClassStockQty;
}
