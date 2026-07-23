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
     * 按均分前完整计划量判断是否拆分班次；均分后的剩余量不重复叠加损耗和整车取整。
     *
     * @param netDemandQuantity 净需求量
     * @param closeOut 是否收尾规格
     * @param lossRatePercent 损耗率百分数，5表示5%
     * @param minimumStartQuantity 最小起排量
     * @param vehiclePlanQuantity 单车对应的斜裁排程米数
     * @param equalShareThreshold 各班计划量均分阈值，按均分前完整计划量判断
     * @param equalShareAlreadyApplied 是否已执行过首次均分
     * @return 实际排产量
     */
    public BigDecimal calculateActualQuantity(BigDecimal netDemandQuantity,
                                              boolean closeOut,
                                              BigDecimal lossRatePercent,
                                              BigDecimal minimumStartQuantity,
                                              BigDecimal vehiclePlanQuantity,
                                              BigDecimal equalShareThreshold,
                                              boolean equalShareAlreadyApplied) {
        requireNonNegative(netDemandQuantity, "净需求量");
        requireNonNegative(lossRatePercent, "损耗率");
        requirePositive(minimumStartQuantity, "最小起排量");
        requirePositive(vehiclePlanQuantity, "单车斜裁排程米数");
        requirePositive(equalShareThreshold, "各班计划量均分阈值");
        if (equalShareAlreadyApplied) {
            return normalize(netDemandQuantity);
        }
        BigDecimal quantityWithLoss = netDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(ONE_HUNDRED, 10,
                        RoundingMode.HALF_UP)));
        if (closeOut) {
            return normalize(quantityWithLoss);
        }
        BigDecimal startQuantity = quantityWithLoss.max(minimumStartQuantity);
        BigDecimal vehicleCount = startQuantity.divide(vehiclePlanQuantity,
                0, RoundingMode.CEILING);
        BigDecimal fullPlanQuantity = normalize(vehicleCount.multiply(vehiclePlanQuantity));
        return fullPlanQuantity.compareTo(equalShareThreshold) > 0
                ? normalize(fullPlanQuantity.divide(new BigDecimal("2"), 10,
                        RoundingMode.HALF_UP)) : fullPlanQuantity;
    }

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

        return this.calculateActualQuantity(netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold, false);
    }

    /**
     * 按均分前完整双路计划量判断是否拆分班次；后续班次按剩余量直接排产。
     */
    public BigDecimal calculateSingleSpecSplitActualQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold,
            BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied) {
        requireNonNegative(netDemandQuantity, "净需求量");
        requireNonNegative(lossRatePercent, "损耗率");
        requirePositive(minimumStartQuantity, "最小起排量");
        requirePositive(vehiclePlanQuantity, "单车斜裁排程米数");
        requirePositive(equalShareThreshold, "各班计划量均分阈值");
        requirePositive(craftWidthMillimeter, "斜裁宽度");
        if (equalShareAlreadyApplied) {
            return this.roundSingleSpecSplitUp(netDemandQuantity, craftWidthMillimeter);
        }
        BigDecimal quantityWithLoss = netDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(
                        ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
        if (closeOut) {
            return this.roundSingleSpecSplitUp(
                    quantityWithLoss, craftWidthMillimeter);
        }
        BigDecimal startQuantity = quantityWithLoss.max(minimumStartQuantity);
        BigDecimal vehicleCount = startQuantity.divide(
                vehiclePlanQuantity, 0, RoundingMode.CEILING);
        BigDecimal fullPlanQuantity = normalize(
                vehicleCount.multiply(vehiclePlanQuantity));
        return fullPlanQuantity.compareTo(equalShareThreshold) > 0
                ? this.roundSingleSpecSplitUp(
                        fullPlanQuantity.divide(new BigDecimal("2"), 10,
                                RoundingMode.HALF_UP), craftWidthMillimeter)
                : fullPlanQuantity;
    }

    /** 判断单规格分裁本次试算是否触发跨班均分。 */
    public boolean requiresSingleSpecSplitEqualShare(
            BigDecimal netDemandQuantity, boolean closeOut, BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity, BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold, boolean equalShareAlreadyApplied) {
        requireNonNegative(netDemandQuantity, "净需求量");
        requireNonNegative(lossRatePercent, "损耗率");
        requirePositive(minimumStartQuantity, "最小起排量");
        requirePositive(vehiclePlanQuantity, "单车斜裁排程米数");
        requirePositive(equalShareThreshold, "各班计划量均分阈值");
        if (closeOut || equalShareAlreadyApplied) {
            return false;
        }
        BigDecimal quantityWithLoss = netDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(
                        ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
        BigDecimal fullPlanQuantity = normalize(quantityWithLoss.max(minimumStartQuantity)
                .divide(vehiclePlanQuantity, 0, RoundingMode.CEILING)
                .multiply(vehiclePlanQuantity));
        return fullPlanQuantity.compareTo(equalShareThreshold) > 0;
    }

    /**
     * 计算单规格分裁首次均分后必须转入下一班的精确余量。
     * 首班量按双片步长向上取整，下一班取完整计划量与首班量之差，保证两班合计不变。
     */
    public BigDecimal calculateSingleSpecSplitEqualShareRemainder(
            BigDecimal netDemandQuantity, boolean closeOut, BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity, BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold, BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied) {
        if (!this.requiresSingleSpecSplitEqualShare(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, equalShareAlreadyApplied)) {
            return BigDecimal.ZERO;
        }
        BigDecimal fullPlanQuantity = this.calculateSingleSpecSplitFullPlanQuantity(
                netDemandQuantity, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity);
        BigDecimal firstShiftQuantity = this.calculateSingleSpecSplitActualQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, craftWidthMillimeter,
                equalShareAlreadyApplied);
        return normalize(fullPlanQuantity.subtract(firstShiftQuantity));
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
        return this.calculateSingleSpecSplitActualQuantity(netDemandQuantity, closeOut,
                lossRatePercent, minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, craftWidthMillimeter, false);
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


    private BigDecimal calculateSingleSpecSplitFullPlanQuantity(
            BigDecimal netDemandQuantity,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity) {
        BigDecimal quantityWithLoss = netDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(
                        ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
        return normalize(quantityWithLoss.max(minimumStartQuantity)
                .divide(vehiclePlanQuantity, 0, RoundingMode.CEILING)
                .multiply(vehiclePlanQuantity));
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
