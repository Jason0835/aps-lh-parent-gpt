package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ToolingTrial;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 斜裁工装可用量计算器。
 *
 * <p>一车占用一个工装。试算阶段只读取库排占用车数，不修改工装或库排资源。</p>
 */
@Component
public class Cd15ToolingCalculator {

    /**
     * 计算当前资源快照下的工装可排量。
     *
     * @param actualQuantity 实际排产量
     * @param totalToolingCount 工装总数
     * @param occupiedVehicleCount 当前库排占用车数
     * @param vehiclePlanQuantity 单个工装对应的斜裁排程米数
     * @return 工装试算结果
     */
    public Cd15ToolingTrial calculate(BigDecimal actualQuantity,
                                      int totalToolingCount,
                                      int occupiedVehicleCount,
                                      BigDecimal vehiclePlanQuantity) {
        if (actualQuantity == null || actualQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("实际排产量不能小于0");
        }
        if (totalToolingCount < 0 || occupiedVehicleCount < 0) {
            throw new IllegalArgumentException("工装总数和库排占用车数不能小于0");
        }
        if (vehiclePlanQuantity == null || vehiclePlanQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("单车斜裁排程米数必须大于0");
        }

        int availableCount = Math.max(0, totalToolingCount - occupiedVehicleCount);
        BigDecimal availableQuantity = vehiclePlanQuantity.multiply(BigDecimal.valueOf(availableCount));
        BigDecimal schedulableQuantity = actualQuantity.min(availableQuantity);
        return Cd15ToolingTrial.builder()
                .availableToolingCount(availableCount)
                .availableToolingQuantity(availableQuantity)
                .schedulableQuantity(schedulableQuantity)
                .limitedQuantity(actualQuantity.subtract(schedulableQuantity))
                .build();
    }

    /** 单规格一出二按成对工装计算可排总量。 */
    public Cd15ToolingTrial calculateSingleSpecSplit(
            BigDecimal actualQuantity,
            int totalToolingCount,
            int occupiedVehicleCount,
            BigDecimal vehiclePlanQuantity) {
        if (actualQuantity == null || actualQuantity.signum() < 0) {
            throw new IllegalArgumentException("实际排产量不能小于0");
        }
        if (totalToolingCount < 0 || occupiedVehicleCount < 0) {
            throw new IllegalArgumentException("工装总数和库排占用车数不能小于0");
        }
        if (vehiclePlanQuantity == null || vehiclePlanQuantity.signum() <= 0) {
            throw new IllegalArgumentException("单车斜裁排程米数必须大于0");
        }
        int availableCount = Math.max(0,
                totalToolingCount - occupiedVehicleCount);
        int availablePairCount = availableCount / 2;
        BigDecimal availableQuantity = vehiclePlanQuantity
                .multiply(new BigDecimal("2"))
                .multiply(BigDecimal.valueOf(availablePairCount));
        BigDecimal schedulableQuantity = actualQuantity.min(availableQuantity);
        return Cd15ToolingTrial.builder()
                .availableToolingCount(availableCount)
                .availableToolingQuantity(availableQuantity)
                .schedulableQuantity(schedulableQuantity)
                .limitedQuantity(actualQuantity.subtract(schedulableQuantity))
                .build();
    }
}
