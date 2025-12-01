package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProdDetailFinal;
import lombok.Data;

/**
 * 月计划终稿明细Vo
 *
 * @author Chen
 * @date 2025/3/31
 */
@Data
public class MonthPlanProdDetailFinalVo extends MonthPlanProdDetailFinal {

    /**
     * 订单号
     */
    private String orderNo;
}
