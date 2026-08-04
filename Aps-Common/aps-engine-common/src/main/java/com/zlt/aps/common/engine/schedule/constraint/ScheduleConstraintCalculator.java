package com.zlt.aps.common.engine.schedule.constraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 胎面、胎侧共用的排程约束纯计算器。
 *
 * <p>本类无数据库、Spring 和运行态上下文依赖，相同输入必须得到相同结果。</p>
 */
public class ScheduleConstraintCalculator {

    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal("60");

    private static final int CALCULATION_SCALE = 8;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * 按损耗率、最小起排量和卷曲长度依次结算计划量。
     *
     * <p>损耗率使用百分比口径。收尾任务可通过 {@code skipMinStartAndRound} 跳过最小起排和卷曲取整，
     * 但仍会计算损耗补偿量。</p>
     *
     * @param preLossPlanQty 损耗计算前计划量
     * @param lossRatePercent 损耗率百分比
     * @param minStartQty 最小起排量
     * @param curlRollLength 卷曲长度
     * @param skipMinStartAndRound 是否跳过最小起排和卷曲取整
     * @param calculationScale 小数计算精度；负数按0处理
     * @return 计划量各阶段结算结果
     */
    public SchedulePlanQtyAdjustmentResult calculatePlanQtyAfterLoss(BigDecimal preLossPlanQty,
                                                                      BigDecimal lossRatePercent,
                                                                      BigDecimal minStartQty,
                                                                      BigDecimal curlRollLength,
                                                                      boolean skipMinStartAndRound,
                                                                      int calculationScale) {
        int normalizedScale = Math.max(calculationScale, 0);
        BigDecimal normalizedPreLossPlanQty = this.nonNegative(preLossPlanQty)
                .setScale(normalizedScale, RoundingMode.HALF_UP);
        BigDecimal normalizedLossRate = this.nonNegative(lossRatePercent);
        BigDecimal lossAddQty = BigDecimal.ZERO.setScale(normalizedScale, RoundingMode.HALF_UP);
        if (normalizedPreLossPlanQty.compareTo(BigDecimal.ZERO) > 0
                && normalizedLossRate.compareTo(BigDecimal.ZERO) > 0) {
            lossAddQty = normalizedPreLossPlanQty.multiply(normalizedLossRate)
                    .divide(ONE_HUNDRED, normalizedScale, RoundingMode.HALF_UP);
        }
        BigDecimal planQtyAfterLoss = normalizedPreLossPlanQty.add(lossAddQty)
                .setScale(normalizedScale, RoundingMode.HALF_UP);
        BigDecimal planQtyAfterMinStart = planQtyAfterLoss;
        BigDecimal normalizedMinStartQty = this.nonNegative(minStartQty)
                .setScale(normalizedScale, RoundingMode.HALF_UP);
        if (!skipMinStartAndRound
                && planQtyAfterLoss.compareTo(BigDecimal.ZERO) > 0
                && normalizedMinStartQty.compareTo(BigDecimal.ZERO) > 0
                && planQtyAfterLoss.compareTo(normalizedMinStartQty) < 0) {
            planQtyAfterMinStart = normalizedMinStartQty;
        }
        BigDecimal minStartAdjustQty = planQtyAfterMinStart.subtract(planQtyAfterLoss)
                .setScale(normalizedScale, RoundingMode.HALF_UP);
        BigDecimal finalPlanQty = planQtyAfterMinStart;
        BigDecimal normalizedCurlRollLength = this.nonNegative(curlRollLength)
                .setScale(normalizedScale, RoundingMode.HALF_UP);
        if (!skipMinStartAndRound
                && planQtyAfterMinStart.compareTo(BigDecimal.ZERO) > 0
                && normalizedCurlRollLength.compareTo(BigDecimal.ZERO) > 0) {
            finalPlanQty = planQtyAfterMinStart.divide(normalizedCurlRollLength, 0, RoundingMode.CEILING)
                    .multiply(normalizedCurlRollLength)
                    .setScale(normalizedScale, RoundingMode.HALF_UP);
        }
        BigDecimal roundAdjustQty = finalPlanQty.subtract(planQtyAfterMinStart)
                .setScale(normalizedScale, RoundingMode.HALF_UP);

        SchedulePlanQtyAdjustmentResult result = new SchedulePlanQtyAdjustmentResult();
        result.setPreLossPlanQty(normalizedPreLossPlanQty);
        result.setLossAddQty(lossAddQty);
        result.setPlanQtyAfterLoss(planQtyAfterLoss);
        result.setMinStartAdjustQty(minStartAdjustQty);
        result.setPlanQtyAfterMinStart(planQtyAfterMinStart);
        result.setRoundAdjustQty(roundAdjustQty);
        result.setFinalPlanQty(finalPlanQty);
        return result;
    }

    /**
     * 计算两个相邻任务之间的切换产能。
     *
     * @param previousTask 前置任务；班次没有有效前置时可为空
     * @param currentTask 当前任务
     * @param constraintConfig 约束参数；为空时按零扣减
     * @return 切换产能扣减明细
     */
    public ScheduleTransitionConstraintResult calculateTransition(ScheduleTaskConstraint previousTask,
                                                                  ScheduleTaskConstraint currentTask,
                                                                  ScheduleConstraintConfig constraintConfig) {
        ScheduleTransitionConstraintResult result = new ScheduleTransitionConstraintResult();
        if (previousTask == null || currentTask == null || constraintConfig == null) {
            return result;
        }
        if (this.isConfirmedChange(previousTask.getSpecCode(), currentTask.getSpecCode())) {
            BigDecimal specSwitchHours = this.nonNegative(constraintConfig.getSpecChangeMinutes())
                    .divide(MINUTES_PER_HOUR, CALCULATION_SCALE, RoundingMode.HALF_UP);
            result.setSpecSwitchCapacityDeduct(specSwitchHours
                    .multiply(this.nonNegative(currentTask.getMachineSpeed())));
        }
        if (this.isConfirmedChange(previousTask.getGlueCode(), currentTask.getGlueCode())) {
            result.setGlueSwitchCapacityDeduct(
                    this.nonNegative(constraintConfig.getGlueChangeCapacityDeduct()));
        }
        return result;
    }

    /**
     * 计算班次扣除已排计划量和完整任务链切换后的剩余产能。
     *
     * @param baseCapacity 已扣除维修等班次静态限制的基础产能
     * @param assignedPlanQty 班次已排计划量
     * @param switchCapacityDeduct 班次完整任务链切换扣减
     * @return 非负剩余产能
     */
    public BigDecimal calculateRemainCapacity(BigDecimal baseCapacity, BigDecimal assignedPlanQty,
                                              BigDecimal switchCapacityDeduct) {
        return this.nonNegative(baseCapacity)
                .subtract(this.nonNegative(assignedPlanQty))
                .subtract(this.nonNegative(switchCapacityDeduct))
                .max(BigDecimal.ZERO);
    }

    /**
     * 根据当前可用工装数量限制任务可排量。
     *
     * @param requestedPlanQty 当前希望安排的计划量
     * @param availableToolQty 当前全局可用工装数量；为空表示未启用工装限制
     * @param curlRollLength 单套工装可生产长度
     * @return 工装允许的非负计划量
     */
    public BigDecimal limitPlanQtyByTool(BigDecimal requestedPlanQty, BigDecimal availableToolQty,
                                         BigDecimal curlRollLength) {
        BigDecimal normalizedRequestedQty = this.nonNegative(requestedPlanQty);
        if (availableToolQty == null || this.nonNegative(curlRollLength).compareTo(BigDecimal.ZERO) <= 0) {
            return normalizedRequestedQty;
        }
        BigDecimal maxPlanQty = this.nonNegative(availableToolQty)
                .multiply(this.nonNegative(curlRollLength));
        return normalizedRequestedQty.min(maxPlanQty).max(BigDecimal.ZERO);
    }

    /**
     * 计算实际计划量占用的工装数量。
     *
     * @param assignedPlanQty 实际安排计划量
     * @param curlRollLength 单套工装可生产长度
     * @return 非负工装占用量；长度无效时返回0
     */
    public BigDecimal calculateToolUsedQty(BigDecimal assignedPlanQty, BigDecimal curlRollLength) {
        if (this.nonNegative(curlRollLength).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return this.nonNegative(assignedPlanQty).divide(this.nonNegative(curlRollLength),
                CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 按生产增量和成型消耗量结算一次全局工装账本。
     *
     * <p>生产增量受当前可用工装限制，成型消耗量用于释放已占用工装。人工滚动只传命令增量，
     * 自动排程传当前任务实际生产量和当前班成型需求量，两条链路因此共用同一净占用公式。</p>
     *
     * @param requestedProductionQty 请求新增的生产量
     * @param releasedDemandQty 当前结算释放的成型需求量
     * @param availableToolQty 结算前可用工装；为空表示未启用工装限制
     * @param totalToolQty 工装池上限；为空时不额外限制上限
     * @param curlRollLength 单套工装可生产长度
     * @return 工装账本结算结果
     */
    public ScheduleToolLedgerResult settleToolLedger(BigDecimal requestedProductionQty,
                                                     BigDecimal releasedDemandQty,
                                                     BigDecimal availableToolQty,
                                                     BigDecimal totalToolQty,
                                                     BigDecimal curlRollLength) {
        ScheduleToolLedgerResult result = new ScheduleToolLedgerResult();
        BigDecimal normalizedProductionQty = this.nonNegative(requestedProductionQty);
        BigDecimal normalizedReleasedQty = this.nonNegative(releasedDemandQty);
        result.setAvailableToolQty(availableToolQty);
        if (availableToolQty == null || this.nonNegative(curlRollLength).compareTo(BigDecimal.ZERO) <= 0) {
            result.setAllowedPlanQty(normalizedProductionQty);
            result.setOverflowPlanQty(BigDecimal.ZERO);
            result.setToolUsedQty(BigDecimal.ZERO.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP));
            result.setRemainingToolQty(availableToolQty);
            return result;
        }
        BigDecimal releasedToolQty = this.calculateToolUsedQty(normalizedReleasedQty, curlRollLength);
        BigDecimal balanceAfterRelease = this.nonNegative(availableToolQty).add(releasedToolQty);
        if (totalToolQty != null) {
            balanceAfterRelease = balanceAfterRelease.min(this.nonNegative(totalToolQty));
        }
        BigDecimal allowedPlanQty = this.limitPlanQtyByTool(
                normalizedProductionQty, balanceAfterRelease, curlRollLength);
        BigDecimal productionToolQty = this.calculateToolUsedQty(allowedPlanQty, curlRollLength);
        BigDecimal toolUsedQty = productionToolQty.subtract(releasedToolQty)
                .setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal remainingToolQty = balanceAfterRelease.subtract(productionToolQty).max(BigDecimal.ZERO);
        if (totalToolQty != null) {
            remainingToolQty = remainingToolQty.min(this.nonNegative(totalToolQty));
        }
        result.setAllowedPlanQty(allowedPlanQty);
        result.setOverflowPlanQty(normalizedProductionQty.subtract(allowedPlanQty).max(BigDecimal.ZERO));
        result.setToolUsedQty(toolUsedQty);
        result.setRemainingToolQty(remainingToolQty.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP));
        return result;
    }

    /**
     * 结算已经通过前置约束确认的生产量，不再次压缩计划量。
     *
     * @param committedProductionQty 已确认生产量
     * @param releasedDemandQty 当前结算释放的成型需求量
     * @param availableToolQty 结算前可用工装
     * @param totalToolQty 工装池上限
     * @param curlRollLength 单套工装可生产长度
     * @return 工装账本结算结果
     */
    public ScheduleToolLedgerResult settleCommittedToolLedger(BigDecimal committedProductionQty,
                                                              BigDecimal releasedDemandQty,
                                                              BigDecimal availableToolQty,
                                                              BigDecimal totalToolQty,
                                                              BigDecimal curlRollLength) {
        ScheduleToolLedgerResult result = new ScheduleToolLedgerResult();
        BigDecimal normalizedProductionQty = this.nonNegative(committedProductionQty);
        result.setAvailableToolQty(availableToolQty);
        result.setAllowedPlanQty(normalizedProductionQty);
        result.setOverflowPlanQty(BigDecimal.ZERO);
        if (availableToolQty == null || this.nonNegative(curlRollLength).compareTo(BigDecimal.ZERO) <= 0) {
            result.setToolUsedQty(BigDecimal.ZERO.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP));
            result.setRemainingToolQty(availableToolQty);
            return result;
        }
        BigDecimal toolUsedQty = this.calculateToolUsedQty(
                normalizedProductionQty.subtract(this.nonNegative(releasedDemandQty)), curlRollLength);
        if (normalizedProductionQty.compareTo(this.nonNegative(releasedDemandQty)) < 0) {
            toolUsedQty = this.nonNegative(releasedDemandQty).subtract(normalizedProductionQty)
                    .divide(this.nonNegative(curlRollLength), CALCULATION_SCALE, RoundingMode.HALF_UP)
                    .negate();
        }
        BigDecimal remainingToolQty = this.nonNegative(availableToolQty).subtract(toolUsedQty).max(BigDecimal.ZERO);
        if (totalToolQty != null) {
            remainingToolQty = remainingToolQty.min(this.nonNegative(totalToolQty));
        }
        result.setToolUsedQty(toolUsedQty.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP));
        result.setRemainingToolQty(remainingToolQty.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP));
        return result;
    }

    /**
     * 判断两个非空编码是否发生变化。
     *
     * @param previousCode 前置编码
     * @param currentCode 当前编码
     * @return 两个编码均有效且不相同时返回true
     */
    private boolean isConfirmedChange(String previousCode, String currentCode) {
        return !this.isBlank(previousCode) && !this.isBlank(currentCode)
                && !Objects.equals(previousCode, currentCode);
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待判断字符串
     * @return 为空或全空白时返回true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 将空值或负值归零。
     *
     * @param value 原始数值
     * @return 非负数值
     */
    private BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }
}
