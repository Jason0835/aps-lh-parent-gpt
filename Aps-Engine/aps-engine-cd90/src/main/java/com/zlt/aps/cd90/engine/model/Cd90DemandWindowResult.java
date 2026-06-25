package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成型需求窗口计算结果。
 */
@Data
@Builder
public class Cd90DemandWindowResult {

    /** 最终帘布需求量，单位米。 */
    private BigDecimal demandQuantity;
    /** 实际参与平均或求和的有效班次数。 */
    private int effectiveShiftCount;
    /** 保留自然班次和额外班次的需求明细。 */
    private List<Cd90DemandShift> shiftDetails;
}
