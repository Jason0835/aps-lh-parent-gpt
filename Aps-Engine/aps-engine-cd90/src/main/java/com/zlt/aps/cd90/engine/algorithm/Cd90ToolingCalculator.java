package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ToolingTrial;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 直裁工装可用量计算器。
 *
 * <p>一车占用一个工装。试算阶段只读取库排占用车数，不修改工装或库排资源。</p>
 */
@Component
public class Cd90ToolingCalculator {

    /**
     * 计算当前资源快照下的工装可排量。
     *
     * @param actualQuantity 实际排产量
     * @param totalToolingCount 工装总数
     * @param occupiedVehicleCount 当前库排占用车数
     * @param standardCurlLength 标准卷曲长度，直接作为单个工装的排程容量
     * @return 工装试算结果
     */
    public Cd90ToolingTrial calculate(BigDecimal actualQuantity,
                                      int totalToolingCount,
                                      int occupiedVehicleCount,
                                      BigDecimal standardCurlLength) {
        if (actualQuantity == null || actualQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("实际排产量不能小于0");
        }
        if (totalToolingCount < 0 || occupiedVehicleCount < 0) {
            throw new IllegalArgumentException("工装总数和库排占用车数不能小于0");
        }
        if (standardCurlLength == null || standardCurlLength.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("标准卷曲长度必须大于0");
        }

        int availableCount = Math.max(0, totalToolingCount - occupiedVehicleCount);
        BigDecimal availableQuantity = standardCurlLength.multiply(BigDecimal.valueOf(availableCount));
        BigDecimal schedulableQuantity = actualQuantity.min(availableQuantity);
        return Cd90ToolingTrial.builder()
                .availableToolingCount(availableCount)
                .availableToolingQuantity(availableQuantity)
                .schedulableQuantity(schedulableQuantity)
                .limitedQuantity(actualQuantity.subtract(schedulableQuantity))
                .build();
    }
}
