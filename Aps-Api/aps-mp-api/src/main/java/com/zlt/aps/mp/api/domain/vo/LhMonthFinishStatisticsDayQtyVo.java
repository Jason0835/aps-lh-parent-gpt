package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 统计月计划完成数量
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhMonthFinishStatisticsDayQtyVo {
    /**
     * 可利用模具
     */
    private Integer usedMouldQtyTotal;
    /**
     * 期初库存
     */
    private String initQtyTotal;
    /**
     * 计划数量
     */
    private Integer monthPlanQtyTotal = 0;
    /**
     * 本月完成数量
     */
    private Integer monthFinishQtyTotal = 0;
    /**
     * 剩余数量
     */
    private Integer monthRemainQtyTotal = 0;

    /**
     * 每天总量
     */
    private List<LhMonthDayFinishQtyVo> dayFinishQtyList;
}
