package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单笔GDYY大卷成熟流水占用明细。
 */
@Data
@Builder
public class Cd15BigRollAgingAllocationItem {

    /** 被占用的大卷流水。 */
    private Cd15BigRollAgingStock stock;
    /** 本次任务从该流水占用的米数。 */
    private BigDecimal quantity;
}