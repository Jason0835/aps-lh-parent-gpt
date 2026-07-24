package com.zlt.aps.tm.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎面排程结果看板汇总。
 *
 * <p>按查询条件对全部匹配行汇总库存与各班次计划量，供排程管理页面合计条展示。</p>
 */
@Data
@ApiModel(value = "胎面排程结果看板汇总")
public class TmScheduleSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存合计（六点库存汇总）。 */
    @ApiModelProperty(value = "库存合计")
    private BigDecimal totalStockQty = BigDecimal.ZERO;

    /**
     * 各班次计划量合计列表。
     *
     * <p>下标 0 对应 1 班，依此类推至 6 班；长度固定为 {@code TmScheduleConstants.TM_MAX_SHIFT_ORDER}。</p>
     */
    @ApiModelProperty(value = "各班次计划量合计")
    private List<BigDecimal> shiftPlanQtyList = new ArrayList<>();
}
