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
     * 按完整计划量和班产上限计算本班均衡计划量。
     *
     * @param netDemandQuantity 净需求量
     * @param closeOut 是否收尾规格
     * @param lossRatePercent 损耗率百分数，5表示5%
     * @param minimumStartQuantity 最小起排量
     * @param vehiclePlanQuantity 单车可承载的钢带米数
     * @param equalShareThreshold 单规格均分及班产上限阈值
     * @param equalShareAlreadyApplied 是否为已完成损耗、起排量和整车取整的跨班余量
     * @return 实际排产量
     */
    public BigDecimal calculateActualQuantity(BigDecimal netDemandQuantity,
                                              boolean closeOut,
                                              BigDecimal lossRatePercent,
                                              BigDecimal minimumStartQuantity,
                                              BigDecimal vehiclePlanQuantity,
                                              BigDecimal equalShareThreshold,
                                              boolean equalShareAlreadyApplied) {
        return this.calculateActualQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, equalShareAlreadyApplied, null);
    }

    /** 按当前班该钢带的剩余额度进一步封顶。 */
    public BigDecimal calculateActualQuantity(BigDecimal netDemandQuantity,
                                              boolean closeOut,
                                              BigDecimal lossRatePercent,
                                              BigDecimal minimumStartQuantity,
                                              BigDecimal vehiclePlanQuantity,
                                              BigDecimal equalShareThreshold,
                                              boolean equalShareAlreadyApplied,
                                              BigDecimal remainingShiftQuantity) {
        this.validateInputs(netDemandQuantity, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold);
        BigDecimal fullPlanQuantity = this.calculateFullPlanQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareAlreadyApplied);
        BigDecimal currentShiftQuantity = this.balanceByShiftLimit(
                fullPlanQuantity, equalShareThreshold, vehiclePlanQuantity);
        return this.capByRemainingShiftQuantity(
                currentShiftQuantity, remainingShiftQuantity,
                vehiclePlanQuantity);
    }

    public BigDecimal calculateActualQuantity(BigDecimal netDemandQuantity,
                                              boolean closeOut,
                                              BigDecimal lossRatePercent,
                                              BigDecimal minimumStartQuantity,
                                              BigDecimal vehiclePlanQuantity,
                                              BigDecimal equalShareThreshold) {
        return this.calculateActualQuantity(netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold, false);
    }

    /** 计算普通单裁在本班安排后应转入后续班次的精确计划余量。 */
    public BigDecimal calculateActualQuantityRemainder(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold,
            boolean equalShareAlreadyApplied,
            BigDecimal remainingShiftQuantity) {
        this.validateInputs(netDemandQuantity, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold);
        BigDecimal fullPlanQuantity = this.calculateFullPlanQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareAlreadyApplied);
        BigDecimal plannedTotalQuantity = this.plannedTotalQuantity(
                fullPlanQuantity, equalShareThreshold, vehiclePlanQuantity);
        BigDecimal currentShiftQuantity = this.calculateActualQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, equalShareAlreadyApplied,
                remainingShiftQuantity);
        return this.normalize(plannedTotalQuantity.subtract(
                currentShiftQuantity).max(BigDecimal.ZERO));
    }

    /** 按完整双路计划量和班产上限计算本班均衡计划量。 */
    public BigDecimal calculateSingleSpecSplitActualQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold,
            BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied) {
        return this.calculateSingleSpecSplitActualQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, craftWidthMillimeter,
                equalShareAlreadyApplied, null);
    }

    /** 按当前班该钢带的剩余额度进一步封顶单规格一出二计划量。 */
    public BigDecimal calculateSingleSpecSplitActualQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold,
            BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied,
            BigDecimal remainingShiftQuantity) {
        this.validateInputs(netDemandQuantity, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold);
        this.requirePositive(craftWidthMillimeter, "斜裁宽度");
        BigDecimal pairQuantity = this.pairQuantity(craftWidthMillimeter);
        BigDecimal fullPlanQuantity = this.calculateSingleSpecSplitFullPlanQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                craftWidthMillimeter, equalShareAlreadyApplied);
        BigDecimal currentShiftQuantity = this.balanceByShiftLimit(
                fullPlanQuantity, equalShareThreshold, pairQuantity);
        return this.capByRemainingShiftQuantity(
                currentShiftQuantity, remainingShiftQuantity, pairQuantity);
    }

    /** 判断单规格分裁本次试算是否触发跨班均分。 */
    public boolean requiresSingleSpecSplitEqualShare(
            BigDecimal netDemandQuantity, boolean closeOut, BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity, BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold, boolean equalShareAlreadyApplied) {
        this.validateInputs(netDemandQuantity, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold);
        BigDecimal fullPlanQuantity = this.calculateFullPlanQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareAlreadyApplied);
        return fullPlanQuantity.compareTo(equalShareThreshold) > 0;
    }

    /**
     * 计算单规格分裁本班均分后必须转入后续班次的精确余量。
     * 后续班次会继续按剩余总量动态计算班数，不会重复叠加损耗。
     */
    public BigDecimal calculateSingleSpecSplitEqualShareRemainder(
            BigDecimal netDemandQuantity, boolean closeOut, BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity, BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold, BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied) {
        return this.calculateSingleSpecSplitEqualShareRemainder(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, craftWidthMillimeter,
                equalShareAlreadyApplied, null);
    }

    /** 计算考虑当前班同钢带剩余额度后的单规格分裁精确余量。 */
    public BigDecimal calculateSingleSpecSplitEqualShareRemainder(
            BigDecimal netDemandQuantity, boolean closeOut, BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity, BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold, BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied, BigDecimal remainingShiftQuantity) {
        this.validateInputs(netDemandQuantity, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity, equalShareThreshold);
        this.requirePositive(craftWidthMillimeter, "斜裁宽度");
        BigDecimal fullPlanQuantity = this.calculateSingleSpecSplitFullPlanQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                craftWidthMillimeter, equalShareAlreadyApplied);
        BigDecimal firstShiftQuantity = this.calculateSingleSpecSplitActualQuantity(
                netDemandQuantity, closeOut, lossRatePercent,
                minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, craftWidthMillimeter,
                equalShareAlreadyApplied, remainingShiftQuantity);
        return this.normalize(fullPlanQuantity.subtract(
                firstShiftQuantity).max(BigDecimal.ZERO));
    }

    /**
     * 计算单规格一出二的成品总计划量。
     * 非收尾先按整车取整，收尾只叠加损耗；两者最终均按完整双片步长取整。
     */
    public BigDecimal calculateSingleSpecSplitActualQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold,
            BigDecimal craftWidthMillimeter) {
        return this.calculateSingleSpecSplitActualQuantity(netDemandQuantity, closeOut,
                lossRatePercent, minimumStartQuantity, vehiclePlanQuantity,
                equalShareThreshold, craftWidthMillimeter, false);
    }

    /** 将受限可排量向下归整为完整的一出二双片步长。 */
    public BigDecimal roundSingleSpecSplitDown(
            BigDecimal quantity, BigDecimal craftWidthMillimeter) {
        this.requireNonNegative(quantity, "单规格分裁可排量");
        BigDecimal pairQuantity = this.pairQuantity(craftWidthMillimeter);
        return this.normalize(quantity.divide(pairQuantity, 0, RoundingMode.FLOOR)
                .multiply(pairQuantity));
    }

    private BigDecimal roundSingleSpecSplitUp(
            BigDecimal quantity, BigDecimal craftWidthMillimeter) {
        BigDecimal pairQuantity = this.pairQuantity(craftWidthMillimeter);
        return this.normalize(quantity.divide(pairQuantity, 0, RoundingMode.CEILING)
                .multiply(pairQuantity));
    }

    private BigDecimal pairQuantity(BigDecimal craftWidthMillimeter) {
        this.requirePositive(craftWidthMillimeter, "斜裁宽度");
        return craftWidthMillimeter.multiply(new BigDecimal("2"))
                .divide(new BigDecimal("1000"), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSingleSpecSplitFullPlanQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal craftWidthMillimeter,
            boolean equalShareAlreadyApplied) {
        BigDecimal fullPlanQuantity;
        if (equalShareAlreadyApplied) {
            fullPlanQuantity = netDemandQuantity;
        } else {
            BigDecimal quantityWithLoss = netDemandQuantity.multiply(
                    BigDecimal.ONE.add(lossRatePercent.divide(
                            ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
            if (closeOut) {
                fullPlanQuantity = quantityWithLoss;
            } else {
                BigDecimal branchQuantity = quantityWithLoss
                        .max(minimumStartQuantity)
                        .divide(new BigDecimal("2"));
                BigDecimal branchVehicleCount = branchQuantity.divide(
                        vehiclePlanQuantity, 0, RoundingMode.CEILING);
                fullPlanQuantity = branchVehicleCount
                        .multiply(vehiclePlanQuantity)
                        .multiply(new BigDecimal("2"));
            }
        }
        return this.roundSingleSpecSplitUp(
                fullPlanQuantity, craftWidthMillimeter);
    }

    /**
     * 计算均分前完整计划量。跨班余量已经包含首次损耗、起排量和整车取整，
     * 后续班次只重新按当前剩余量计算均衡份额。
     */
    private BigDecimal calculateFullPlanQuantity(
            BigDecimal netDemandQuantity,
            boolean closeOut,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            boolean equalShareAlreadyApplied) {
        if (equalShareAlreadyApplied) {
            return this.normalize(netDemandQuantity);
        }
        BigDecimal quantityWithLoss = netDemandQuantity.multiply(
                BigDecimal.ONE.add(lossRatePercent.divide(
                        ONE_HUNDRED, 10, RoundingMode.HALF_UP)));
        if (closeOut) {
            return this.normalize(quantityWithLoss);
        }
        return this.normalize(quantityWithLoss.max(minimumStartQuantity)
                .divide(vehiclePlanQuantity, 0, RoundingMode.CEILING)
                .multiply(vehiclePlanQuantity));
    }

    /**
     * 先按阈值计算所需班次数，再按有效生产步长均衡分配本班数量。
     * 通过单位数均分保证每班不超过阈值，且所有班次最终合计保持不变。
     */
    private BigDecimal balanceByShiftLimit(
            BigDecimal fullPlanQuantity,
            BigDecimal shiftLimit,
            BigDecimal productionStep) {
        if (fullPlanQuantity.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (fullPlanQuantity.compareTo(shiftLimit) <= 0) {
            return this.normalize(fullPlanQuantity);
        }
        BigDecimal totalUnitCount = fullPlanQuantity.divide(
                productionStep, 0, RoundingMode.CEILING);
        BigDecimal maxUnitCount = shiftLimit.divide(
                productionStep, 0, RoundingMode.FLOOR);
        if (maxUnitCount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal requiredShiftCount = totalUnitCount.divide(
                maxUnitCount, 0, RoundingMode.CEILING);
        BigDecimal currentShiftUnitCount = totalUnitCount.divide(
                requiredShiftCount, 0, RoundingMode.CEILING);
        return this.normalize(currentShiftUnitCount.multiply(productionStep));
    }

    /** 计算跨班均分后所有班次最终需要完成的总计划量。 */
    private BigDecimal plannedTotalQuantity(
            BigDecimal fullPlanQuantity,
            BigDecimal shiftLimit,
            BigDecimal productionStep) {
        if (fullPlanQuantity.compareTo(shiftLimit) <= 0) {
            return this.normalize(fullPlanQuantity);
        }
        return this.normalize(fullPlanQuantity.divide(
                productionStep, 0, RoundingMode.CEILING)
                .multiply(productionStep));
    }

    /** 按当前班该钢带剩余额度封顶，并向下归整到有效生产步长。 */
    private BigDecimal capByRemainingShiftQuantity(
            BigDecimal plannedQuantity,
            BigDecimal remainingShiftQuantity,
            BigDecimal productionStep) {
        if (remainingShiftQuantity == null) {
            return this.normalize(plannedQuantity);
        }
        this.requireNonNegative(remainingShiftQuantity, "当前班同钢带剩余额度");
        BigDecimal availableUnitCount = remainingShiftQuantity.divide(
                productionStep, 0, RoundingMode.FLOOR);
        BigDecimal availableQuantity = availableUnitCount.multiply(productionStep);
        return this.normalize(plannedQuantity.min(availableQuantity));
    }

    private void validateInputs(
            BigDecimal netDemandQuantity,
            BigDecimal lossRatePercent,
            BigDecimal minimumStartQuantity,
            BigDecimal vehiclePlanQuantity,
            BigDecimal equalShareThreshold) {
        this.requireNonNegative(netDemandQuantity, "净需求量");
        this.requireNonNegative(lossRatePercent, "损耗率");
        this.requirePositive(minimumStartQuantity, "最小起排量");
        this.requirePositive(vehiclePlanQuantity, "单车钢带承载米数");
        this.requirePositive(equalShareThreshold, "单规格均分及班产上限阈值");
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0
                ? value.setScale(0) : value.stripTrailingZeros();
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
