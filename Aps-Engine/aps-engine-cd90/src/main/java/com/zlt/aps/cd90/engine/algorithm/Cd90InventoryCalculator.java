package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InventoryProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 基于6点库存快照的预计库存计算器。
 */
@Component
public class Cd90InventoryCalculator {

    /**
     * 从原始6点库存重新累计到目标班次开始时点。
     *
     * @param stockAtSix 6点库存快照
     * @param cumulativeFormingConsumption 累计成型消耗
     * @param cumulativeCuttingInbound 累计直裁入库
     * @return 预计库存结果
     */
    public Cd90InventoryProjection project(BigDecimal stockAtSix,
                                           BigDecimal cumulativeFormingConsumption,
                                           BigDecimal cumulativeCuttingInbound) {
        requireNonNegative(stockAtSix, "6点库存");
        requireNonNegative(cumulativeFormingConsumption, "累计成型消耗");
        requireNonNegative(cumulativeCuttingInbound, "累计直裁入库");
        BigDecimal balance = stockAtSix
                .subtract(cumulativeFormingConsumption)
                .add(cumulativeCuttingInbound);
        return Cd90InventoryProjection.builder()
                .inventoryBalance(balance)
                .expectedAvailableStock(balance.max(BigDecimal.ZERO))
                .accumulatedShortageQuantity(balance.min(BigDecimal.ZERO).abs())
                .build();
    }

    private void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + "不能小于0");
        }
    }
}
