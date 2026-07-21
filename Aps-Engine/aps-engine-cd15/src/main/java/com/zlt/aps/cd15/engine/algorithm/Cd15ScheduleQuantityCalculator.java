package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 斜裁自动排程实际排产量计算器。
 *
 * <p>该计算器为纯计算组件，不读取数据库，也不修改排程资源快照。</p>
 */
@Component
public class Cd15ScheduleQuantityCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * 根据净需求、收尾标识和候选机台损耗率计算实际排产量。
     *
     * @param netDemandQuantity 净需求量
     * @param closeOut 是否收尾规格
     * @param lossRatePercent 损耗率百分数，5表示5%
     * @param minimumStartQuantity 最小起排量
     * @param vehiclePlanQuantity 单车对应的斜裁排程米数
     * @param equalShareThreshold 各班计划量均分阈值，按加损耗前的净需求量判断
     * @return 实际排产量
     */
    public BigDecimal calculateActualQuantity(BigDecimal netDemandQuantity,
                                              boolean closeOut,
                                              BigDecimal lossRatePercent,
                                              BigDecimal minimumStartQuantity,
                                              BigDecimal vehiclePlanQuantity,
                                              BigDecimal equalShareThreshold) {
        requireNonNegative(netDemandQuantity, "净需求量");
        requireNonNegative(lossRatePercent, "损耗率");
        requirePositive(minimumStartQuantity, "最小起排量");
        requirePositive(vehiclePlanQuantity, "单车斜裁排程米数");
        requirePositive(equalShareThreshold, "各班计划量均分阈值");

        BigDecimal baseDemandQuantity = adjustDemandForEqualShare(netDemandQuantity, closeOut, equalShareThreshold);
        BigDecimal quantityWithLoss = baseDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
        if (closeOut) {
            return normalize(quantityWithLoss);
        }

        BigDecimal startQuantity = quantityWithLoss.max(minimumStartQuantity);
        BigDecimal vehicleCount = startQuantity.divide(vehiclePlanQuantity, 0, RoundingMode.CEILING);
        return normalize(vehicleCount.multiply(vehiclePlanQuantity));
    }

    /**
     * 计算单规格一出二的成品总计划量。
     * 非收尾按两路各一车的总量取整，收尾按完整双片步长取整。
     */
    public BigDecimal calculateSingleSpecSplitActualQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold,
            BigDecimal craftWidthMillimeter) {
        requireNonNegative(netDemandQuantity, "净需求量");
        requireNonNegative(lossRatePercent, "损耗率");
        requirePositive(minimumStartQuantity, "最小起排量");
        requirePositive(vehiclePlanQuantity, "单车斜裁排程米数");
        requirePositive(equalShareThreshold, "各班计划量均分阈值");
        requirePositive(craftWidthMillimeter, "斜裁宽度");
        BigDecimal baseDemandQuantity = this.adjustDemandForEqualShare(
                netDemandQuantity, closeOut, equalShareThreshold);
        BigDecimal quantityWithLoss = baseDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(
                        ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
        if (closeOut) {
            return this.roundSingleSpecSplitUp(
                    quantityWithLoss, craftWidthMillimeter);
        }
        BigDecimal startQuantity = quantityWithLoss.max(minimumStartQuantity);
        BigDecimal twoStreamVehicleQuantity = vehiclePlanQuantity
                .multiply(new BigDecimal("2"));
        BigDecimal vehiclePairCount = startQuantity.divide(
                twoStreamVehicleQuantity, 0, RoundingMode.CEILING);
        return normalize(vehiclePairCount.multiply(twoStreamVehicleQuantity));
    }

    /** 将受限可排量向下归整为完整的一出二双片步长。 */
    public BigDecimal roundSingleSpecSplitDown(
            BigDecimal quantity, BigDecimal craftWidthMillimeter) {
        requireNonNegative(quantity, "单规格分裁可排量");
        BigDecimal pairQuantity = this.pairQuantity(craftWidthMillimeter);
        return normalize(quantity.divide(pairQuantity, 0, RoundingMode.FLOOR)
                .multiply(pairQuantity));
    }

    private BigDecimal roundSingleSpecSplitUp(
            BigDecimal quantity, BigDecimal craftWidthMillimeter) {
        BigDecimal pairQuantity = this.pairQuantity(craftWidthMillimeter);
        return normalize(quantity.divide(pairQuantity, 0, RoundingMode.CEILING)
                .multiply(pairQuantity));
    }

    private BigDecimal pairQuantity(BigDecimal craftWidthMillimeter) {
        requirePositive(craftWidthMillimeter, "斜裁宽度");
        return craftWidthMillimeter.multiply(new BigDecimal("2"))
                .divide(new BigDecimal("1000"), 10, RoundingMode.HALF_UP);
    }

    /**
     * 非收尾规格按加损耗前的净需求量判断是否触发均分；触发后先除以2，再进入损耗和整卷计算。
     */
    private BigDecimal adjustDemandForEqualShare(BigDecimal netDemandQuantity,
                                                 boolean closeOut,
                                                 BigDecimal equalShareThreshold) {
        if (closeOut || netDemandQuantity.compareTo(equalShareThreshold) <= 0) {
            return netDemandQuantity;
        }
        return netDemandQuantity.divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }

    private void requirePositive(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + "必须大于0");
        }
    }

    private void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(name + "不能小于0");
        }
    }
}
