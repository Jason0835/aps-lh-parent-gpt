package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧排程看板汇总。
 */
@Data
@ApiModel(value = "胎侧排程看板汇总")
public class TcScheduleBoardSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划总量。 */
    @ApiModelProperty(value = "计划总量")
    private BigDecimal totalPlanQty = BigDecimal.ZERO;

    /** 完成总量。 */
    @ApiModelProperty(value = "完成总量")
    private BigDecimal totalFinishQty = BigDecimal.ZERO;

    /** 已排结果数。 */
    @ApiModelProperty(value = "已排结果数")
    private Long resultCount = 0L;

    /** 库存合计（六点库存汇总）。 */
    @ApiModelProperty(value = "库存合计")
    private BigDecimal totalStockQty = BigDecimal.ZERO;

    /**
     * 各班次计划量合计列表。
     *
     * <p>下标 0 对应 1 班，依此类推至 6 班；长度固定为 {@code TcScheduleConstants.TC_MAX_SHIFT_ORDER}。</p>
     */
    @ApiModelProperty(value = "各班次计划量合计")
    private List<BigDecimal> shiftPlanQtyList = new ArrayList<>();
}
