package com.zlt.aps.dj.engine.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 排产量计算结果：本班生产量及（不生产时的）跳过原因
 */
@Getter
public class ProduceQtyResult {
    /** 本班生产量（米），<=0 表示不生产 */
    private BigDecimal produceQty;
    /** 跳过原因（仅当不生产时有值），用于输出真实日志 */
    private String skipReason;

    private ProduceQtyResult(BigDecimal produceQty, String skipReason) {
        this.produceQty = produceQty;
        this.skipReason = skipReason;
    }

    /**
     * 构建可生产的计算结果
     *
     * @param produceQty 生产量（米）
     * @return 结果对象
     */
    public static ProduceQtyResult success(BigDecimal produceQty) {
        return new ProduceQtyResult(produceQty, null);
    }

    /**
     * 构建不生产的计算结果
     *
     * @param skipReason 跳过原因
     * @return 结果对象
     */
    public static ProduceQtyResult skip(String skipReason) {
        return new ProduceQtyResult(BigDecimal.ZERO, skipReason);
    }
}
