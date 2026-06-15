package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面需求量计算输入对象。
 *
 * <p>用于向需求量策略传递成型需求、胎面长度和班次等输入。骨架阶段不定义具体算法。</p>
 */
@Data
public class TmDemandQtyInput {

    /** 胎面编码 */
    private String treadCode;

    /** 基础需求量 */
    private BigDecimal baseDemandQty;
}
