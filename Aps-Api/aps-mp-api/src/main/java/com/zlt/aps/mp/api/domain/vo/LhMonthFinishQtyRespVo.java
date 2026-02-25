package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhMonthFinishQtyRespVo {

    private List<LhMonthFinishQtyVo> lhMonthFinishQtyVos;

    private List<LhMonthFinishStatisticsDayQtyVo> lhMonthFinishStatisticsDayQtyVos;
}
