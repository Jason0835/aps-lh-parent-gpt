package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单笔大卷成熟流水占用明细。
 */
@Data
@Builder
public class Cd90BigRollAgingAllocationItem {

    /** 被占用的大卷流水。 */
    private Cd90BigRollAgingStock stock;
    /** 本次任务从该流水占用的米数。 */
    private BigDecimal quantity;
}
