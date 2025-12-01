package com.zlt.aps.common.engine.domain;

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
     * 昨日早班合计(卷)
     */
    @ApiModelProperty(value = "昨日早班合计(卷)")
    private Double lastDayPlanQtyRollNum;

    /**
     * 夜班合计
     */
    @ApiModelProperty(value = "夜班合计")
    private Double dayPlanQty;

    /**
     * 夜班合计(卷)
     */
    @ApiModelProperty(value = "夜班合计(卷)")
    private Double dayPlanQtyRollNum;

    /**
     * 早班合计
     */
    @ApiModelProperty(value = "早班合计")
    private Double nightPlanQty;

    /**
     * 早班合计(卷)
     */
    @ApiModelProperty(value = "早班合计(卷)")
    private Double nightPlanQtyRollNum;

    /**
     * 次日夜班合计
     */
    @ApiModelProperty(value = "次日夜班合计")
    private Double nextDayPlanQty;

    /**
     * 次日夜班合计(卷)
     */
    @ApiModelProperty(value = "次日夜班合计(卷)")
    private Double nextDayPlanQtyRollNum;

    /**
     * 库存合计
     */
    @ApiModelProperty(value = "库存合计")
    private Double stockQty;

    /**
     * 库存合计(卷)
     */
    @ApiModelProperty(value = "库存合计(卷)")
    private Double stockQtyRollNum;

    /**
     * 库存2合计
     */
    @ApiModelProperty(value = "库存2合计")
    private Double stockQty2;

    /**
     * 库存2合计(卷)
     */
    @ApiModelProperty(value = "库存2合计(卷)")
    private Double stockQty2RollNum;

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

    /**
     * 理论交班库存(卷)
     */
    @ApiModelProperty(value = "理论交班库存(卷)")
    private Double theoreticClassStockQtyRollNum;
}
