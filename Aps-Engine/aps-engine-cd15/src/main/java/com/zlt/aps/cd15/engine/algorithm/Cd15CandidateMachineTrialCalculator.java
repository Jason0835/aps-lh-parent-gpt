package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocationItem;
import com.zlt.aps.cd15.engine.model.Cd15CandidateMachineTrialInput;
import com.zlt.aps.cd15.engine.model.Cd15LossRateSelection;
import com.zlt.aps.cd15.engine.model.Cd15MachineCapacityTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15ToolingTrial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个候选机台组合试算器。
 *
 * <p>依次执行损耗率解析、实际排产量、工装、机台产能和成熟大卷可供量试算。
 * 整个过程不修改资源快照。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cd15CandidateMachineTrialCalculator {

    private final Cd15LossRateResolver lossRateResolver;
    private final Cd15ScheduleQuantityCalculator quantityCalculator;
    private final Cd15ToolingCalculator toolingCalculator;
    private final Cd15MachineCapacityCalculator capacityCalculator;
    private final Cd15BigRollAgingAllocator agingAllocator;
    private final Cd15BigRollMeterCalculator bigRollMeterCalculator;

    /**
     * 计算单台候选机台方案。
     *
     * @param input 候选机台试算输入
     * @return 候选机台试算结果
     */
    public Cd15MachineTrial calculate(Cd15CandidateMachineTrialInput input) {
        if (input == null) {
            throw new IllegalArgumentException("候选机台试算输入不能为空");
        }

        Cd15LossRateSelection lossRate = lossRateResolver.resolve(
                input.getSteelStripCode(), input.getMachineCode(), input.getLossRateRules(),
                input.getFallbackLossRatePercent());
        BigDecimal actualQuantity = input.isSingleSpecSplit()
                ? quantityCalculator.calculateSingleSpecSplitActualQuantity(
                        input.getNetDemandQuantity(), input.isCloseOut(),
                        lossRate.getLossRatePercent(), input.getMinimumStartQuantity(),
                        input.getVehiclePlanQuantity(), input.getEqualShareThreshold(),
                        input.getCraftWidth(), input.isEqualShareAlreadyApplied(),
                        input.getRemainingSpecShiftQuantity())
                : quantityCalculator.calculateActualQuantity(
                        input.getNetDemandQuantity(), input.isCloseOut(),
                        lossRate.getLossRatePercent(), input.getMinimumStartQuantity(),
                        input.getVehiclePlanQuantity(), input.getEqualShareThreshold(),
                        input.isEqualShareAlreadyApplied(),
                        input.getRemainingSpecShiftQuantity());
        BigDecimal equalShareRemainderQuantity = input.isSingleSpecSplit()
                ? quantityCalculator.calculateSingleSpecSplitEqualShareRemainder(
                        input.getNetDemandQuantity(), input.isCloseOut(),
                        lossRate.getLossRatePercent(), input.getMinimumStartQuantity(),
                        input.getVehiclePlanQuantity(), input.getEqualShareThreshold(),
                        input.getCraftWidth(), input.isEqualShareAlreadyApplied(),
                        input.getRemainingSpecShiftQuantity())
                : quantityCalculator.calculateActualQuantityRemainder(
                        input.getNetDemandQuantity(), input.isCloseOut(),
                        lossRate.getLossRatePercent(), input.getMinimumStartQuantity(),
                        input.getVehiclePlanQuantity(), input.getEqualShareThreshold(),
                        input.isEqualShareAlreadyApplied(),
                        input.getRemainingSpecShiftQuantity());
        boolean equalShareApplied = equalShareRemainderQuantity.signum() > 0;
        Cd15ToolingTrial tooling = input.isSingleSpecSplit()
                ? toolingCalculator.calculateSingleSpecSplit(
                        actualQuantity, input.getTotalToolingCount(),
                        input.getOccupiedVehicleCount(), input.getVehiclePlanQuantity())
                : toolingCalculator.calculate(
                        actualQuantity, input.getTotalToolingCount(),
                        input.getOccupiedVehicleCount(), input.getVehiclePlanQuantity());

        // 先按机台原预计开工时间计算理论产能，大卷只需覆盖其他资源共同允许的上限。
        Cd15MachineCapacityTrial initialCapacity = this.calculateCapacity(
                input, input.getRemainingSeconds(), actualQuantity);
        BigDecimal resourceUpperQuantity = actualQuantity
                .min(tooling.getSchedulableQuantity())
                .min(initialCapacity.getCapacityQuantity());
        if (resourceUpperQuantity.signum() <= 0) {
            String limitReason = this.limitReason(actualQuantity,
                    tooling.getSchedulableQuantity(), initialCapacity.getCapacityQuantity(),
                    null, BigDecimal.ZERO);
            return this.zeroTrial(input, lossRate, actualQuantity, equalShareApplied,
                    equalShareRemainderQuantity, tooling,
                    initialCapacity.getCapacityQuantity(), null, limitReason);
        }

        // 部分成熟供给作为可排上限；完全无可用流水时本班不可排。
        Cd15BigRollAgingAllocation agingAllocation = this.agingAllocation(
                input, resourceUpperQuantity);
        if (agingAllocation != null && !agingAllocation.isSuccess()) {
            return this.zeroTrial(input, lossRate, actualQuantity, equalShareApplied,
                    equalShareRemainderQuantity, tooling, BigDecimal.ZERO,
                    agingAllocation, Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT);
        }

        BigDecimal agingSchedulableQuantity = agingAllocation == null
                ? resourceUpperQuantity
                : this.maximumAgingSchedulableQuantity(
                        input, resourceUpperQuantity, agingAllocation);
        if (agingSchedulableQuantity.signum() <= 0) {
            return this.zeroTrial(input, lossRate, actualQuantity, equalShareApplied,
                    equalShareRemainderQuantity, tooling, BigDecimal.ZERO,
                    agingAllocation, Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT);
        }
        if (agingAllocation != null) {
            agingAllocation = this.agingAllocation(input, agingSchedulableQuantity);
        }

        // 最终任务只等待实际使用的成熟流水，不等待未进入本次任务的剩余需求。
        int agingDelaySeconds = agingAllocation == null ? 0 : agingAllocation.getDelaySeconds();
        int remainingSeconds = Math.max(0,
                input.getRemainingSeconds() - agingDelaySeconds);
        Cd15MachineCapacityTrial capacity = this.calculateCapacity(
                input, remainingSeconds, actualQuantity);
        BigDecimal finalQuantity = actualQuantity
                .min(tooling.getSchedulableQuantity())
                .min(capacity.getCapacityQuantity())
                .min(agingSchedulableQuantity);
        if (input.isSingleSpecSplit()) {
            finalQuantity = quantityCalculator.roundSingleSpecSplitDown(
                    finalQuantity, input.getCraftWidth());
        }
        if (finalQuantity.signum() <= 0) {
            return this.zeroTrial(input, lossRate, actualQuantity, equalShareApplied,
                    equalShareRemainderQuantity, tooling,
                    capacity.getCapacityQuantity(), agingAllocation,
                    Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT);
        }

        // 按最终量重算耗时，避免部分排产仍扣除完整需求的机台秒数。
        Cd15MachineCapacityTrial executionCapacity = this.calculateCapacity(
                input, remainingSeconds, finalQuantity);
        String limitReason = this.limitReason(actualQuantity,
                tooling.getSchedulableQuantity(), capacity.getCapacityQuantity(),
                agingSchedulableQuantity, finalQuantity);
        if (agingDelaySeconds > 0
                && capacity.getCapacityQuantity().compareTo(actualQuantity) < 0) {
            limitReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
        }

        Cd15MachineTrial result = Cd15MachineTrial.builder()
                .machineCode(input.getMachineCode())
                .lossRatePercent(lossRate.getLossRatePercent())
                .lossRateLevel(lossRate.getMatchedLevel())
                .actualQuantity(actualQuantity)
                .equalShareApplied(equalShareApplied)
                .equalShareRemainderQuantity(equalShareRemainderQuantity)
                .remainingSpecShiftQuantity(input.getRemainingSpecShiftQuantity())
                .vehiclePlanQuantity(input.getVehiclePlanQuantity())
                .toolingQuantity(tooling.getSchedulableQuantity())
                .capacityQuantity(capacity.getCapacityQuantity())
                .finalSchedulableQuantity(finalQuantity)
                .fullyAccommodated(finalQuantity.compareTo(actualQuantity) >= 0)
                .preferredMachine(input.isPreferredMachine())
                .priorityOrder(input.getPriorityOrder())
                .changeSeconds(executionCapacity.getChangeSeconds())
                .productionSeconds(executionCapacity.getProductionSeconds())
                .sameTailSpec(input.getCurrentTail() == null
                        ? input.getPreviousSpec() != null
                                && input.getPreviousSpec().equals(input.getCurrentSpec())
                        : input.getPreviousTail() != null
                                && input.getPreviousTail().getSteelStripCode() != null
                                && input.getPreviousTail().getSteelStripCode()
                                        .equals(input.getCurrentTail().getSteelStripCode()))
                .historyMachine(input.isHistoryMachine())
                .remainingSeconds(executionCapacity.getRemainingSeconds())
                .taskStartTime(agingAllocation == null ? input.getOriginalStartTime()
                        : agingAllocation.getTaskStartTime())
                .agingDelaySeconds(agingDelaySeconds)
                .agingAllocation(agingAllocation)
                .limitReason(limitReason)
                .build();
        log.debug("[斜裁自动排程] 候选机台试算完成, steelStripCode={}, machineCode={}, "
                        + "lossRateLevel={}, actualQuantity={}, toolingQuantity={}, "
                        + "capacityQuantity={}, agingQuantity={}, finalQuantity={}",
                input.getSteelStripCode(), input.getMachineCode(),
                lossRate.getMatchedLevel(), actualQuantity,
                tooling.getSchedulableQuantity(), capacity.getCapacityQuantity(),
                agingSchedulableQuantity, finalQuantity);
        return result;
    }

    /** 调用大卷成熟分配器进行试算；原开工时间为空时不启用成熟约束。 */
    private Cd15BigRollAgingAllocation agingAllocation(
            Cd15CandidateMachineTrialInput input,
            BigDecimal requestedPlanQuantity) {
        if (input.getOriginalStartTime() == null) {
            return null;
        }
        BigDecimal bigRollConsumeQuantity = bigRollMeterCalculator.calculateForPlanQuantity(
                requestedPlanQuantity, input.getUnitConsumeMillimeter(),
                input.getCraftWidth(), input.getCordWidth(),
                input.getSteelStripCode(), input.getBigRollCode());
        return agingAllocator.preview(input.getBigRollAgingStocks(),
                input.getBigRollCode(), bigRollConsumeQuantity,
                input.getOriginalStartTime());
    }

    /**
     * 在成熟流水各释放时点中选择本班可排量最大的方案。
     * 同一释放时点的流水整体累计后再比较。
     */
    private BigDecimal maximumAgingSchedulableQuantity(
            Cd15CandidateMachineTrialInput input,
            BigDecimal resourceUpperQuantity,
            Cd15BigRollAgingAllocation allocation) {
        List<Cd15BigRollAgingAllocationItem> items = allocation.getItems();
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal cumulativeBigRollMeters = BigDecimal.ZERO;
        BigDecimal maximumQuantity = BigDecimal.ZERO;
        for (int index = 0; index < items.size(); index++) {
            Cd15BigRollAgingAllocationItem item = items.get(index);
            cumulativeBigRollMeters = cumulativeBigRollMeters.add(item.getQuantity());
            LocalDateTime releaseTime = this.effectiveReleaseTime(
                    item, input.getOriginalStartTime());
            boolean lastAtReleaseTime = index == items.size() - 1
                    || !releaseTime.equals(this.effectiveReleaseTime(
                            items.get(index + 1), input.getOriginalStartTime()));
            if (!lastAtReleaseTime) {
                continue;
            }
            int delaySeconds = this.delaySeconds(
                    input.getOriginalStartTime(), releaseTime);
            int remainingSeconds = Math.max(0,
                    input.getRemainingSeconds() - delaySeconds);
            BigDecimal capacityQuantity = this.calculateCapacity(
                    input, remainingSeconds, resourceUpperQuantity)
                    .getCapacityQuantity();
            BigDecimal maturePlanQuantity = bigRollMeterCalculator
                    .calculatePlanQuantityForBigRollMeters(
                            cumulativeBigRollMeters,
                            input.getUnitConsumeMillimeter(),
                            input.getCraftWidth(), input.getCordWidth());
            BigDecimal candidateQuantity = resourceUpperQuantity
                    .min(maturePlanQuantity)
                    .min(capacityQuantity);
            if (candidateQuantity.compareTo(maximumQuantity) > 0) {
                maximumQuantity = candidateQuantity;
            }
        }
        return maximumQuantity;
    }

    /** 按候选机台尾态计算给定剩余时间和请求量下的产能。 */
    private Cd15MachineCapacityTrial calculateCapacity(
            Cd15CandidateMachineTrialInput input,
            int remainingSeconds,
            BigDecimal requestedQuantity) {
        return input.getCurrentTail() == null
                ? capacityCalculator.calculateWithRemainingSeconds(
                        input.getShiftCapacity(), input.getShiftHours(), remainingSeconds,
                        input.getPreviousSpec(), input.getCurrentSpec(),
                        input.getSpecChangeMinutes(), requestedQuantity)
                : capacityCalculator.calculateWithRemainingSeconds(
                        input.getShiftCapacity(), input.getShiftHours(), remainingSeconds,
                        input.getPreviousTail(), input.getCurrentTail(),
                        input.getSameRollDiffSpecChangeMinutes(),
                        input.getDiffRollSameSpecChangeMinutes(),
                        input.getDiffRollDiffSpecChangeMinutes(), requestedQuantity);
    }

    /** 取得单条流水相对于机台原预计开工时间的有效释放时点。 */
    private LocalDateTime effectiveReleaseTime(
            Cd15BigRollAgingAllocationItem item,
            LocalDateTime originalStartTime) {
        LocalDateTime releaseTime = item.getStock().getReleaseTime();
        return releaseTime.isAfter(originalStartTime)
                ? releaseTime : originalStartTime;
    }

    /** 计算释放时点导致的等待秒数。 */
    private int delaySeconds(LocalDateTime originalStartTime,
                             LocalDateTime releaseTime) {
        long seconds = Duration.between(originalStartTime, releaseTime).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return seconds >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    /** 构造不可排的候选机台试算结果。 */
    private Cd15MachineTrial zeroTrial(
            Cd15CandidateMachineTrialInput input,
            Cd15LossRateSelection lossRate,
            BigDecimal actualQuantity,
            boolean equalShareApplied,
            BigDecimal equalShareRemainderQuantity,
            Cd15ToolingTrial tooling,
            BigDecimal capacityQuantity,
            Cd15BigRollAgingAllocation agingAllocation,
            String limitReason) {
        return Cd15MachineTrial.builder()
                .machineCode(input.getMachineCode())
                .lossRatePercent(lossRate.getLossRatePercent())
                .lossRateLevel(lossRate.getMatchedLevel())
                .actualQuantity(actualQuantity)
                .equalShareApplied(equalShareApplied)
                .equalShareRemainderQuantity(equalShareRemainderQuantity)
                .remainingSpecShiftQuantity(input.getRemainingSpecShiftQuantity())
                .vehiclePlanQuantity(input.getVehiclePlanQuantity())
                .toolingQuantity(tooling.getSchedulableQuantity())
                .capacityQuantity(capacityQuantity)
                .finalSchedulableQuantity(BigDecimal.ZERO)
                .fullyAccommodated(false)
                .preferredMachine(input.isPreferredMachine())
                .historyMachine(input.isHistoryMachine())
                .priorityOrder(input.getPriorityOrder())
                .remainingSeconds(input.getRemainingSeconds())
                .taskStartTime(input.getOriginalStartTime())
                .agingAllocation(agingAllocation)
                .limitReason(limitReason)
                .build();
    }

    /** 按稳定优先级识别本次部分排产的主要受限资源。 */
    private String limitReason(BigDecimal actualQuantity,
                               BigDecimal toolingQuantity,
                               BigDecimal capacityQuantity,
                               BigDecimal agingQuantity,
                               BigDecimal finalQuantity) {
        if (actualQuantity == null || finalQuantity == null
                || finalQuantity.compareTo(actualQuantity) >= 0) {
            return null;
        }
        if (toolingQuantity != null
                && finalQuantity.compareTo(toolingQuantity) == 0
                && toolingQuantity.compareTo(actualQuantity) < 0) {
            return "TOOLING_LIMIT";
        }
        if (capacityQuantity != null
                && finalQuantity.compareTo(capacityQuantity) == 0
                && capacityQuantity.compareTo(actualQuantity) < 0) {
            return "CAPACITY_LIMIT";
        }
        if (agingQuantity != null
                && finalQuantity.compareTo(agingQuantity) == 0
                && agingQuantity.compareTo(actualQuantity) < 0) {
            return Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
        }
        return null;
    }
}
