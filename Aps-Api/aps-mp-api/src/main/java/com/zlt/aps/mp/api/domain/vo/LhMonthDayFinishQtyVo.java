package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

/**
 * 月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhMonthDayFinishQtyVo {

    /**
     * 日期
     */
    private String finishDay;

    /**
     * 完成量
     */
    private Integer finishQty;
}
