package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingAllocation;
import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingAllocationItem;
import com.zlt.aps.cd90.engine.model.Cd90CandidateMachineTrialInput;
import com.zlt.aps.cd90.engine.model.Cd90LossRateSelection;
import com.zlt.aps.cd90.engine.model.Cd90MachineCapacityTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import com.zlt.aps.cd90.engine.model.Cd90ToolingTrial;
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
 * <p>依次执行损耗率解析、实际排产量、工装、机台产能和成熟大卷可供量试算。整个过程不修改资源快照。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cd90CandidateMachineTrialCalculator {

    private final Cd90LossRateResolver lossRateResolver;
    private final Cd90ScheduleQuantityCalculator quantityCalculator;
    private final Cd90ToolingCalculator toolingCalculator;
    private final Cd90MachineCapacityCalculator capacityCalculator;
    private final Cd90BigRollAgingAllocator agingAllocator;

    /**
     * 计算单台候选机台方案。
     *
     * @param input 候选机台试算输入
     * @return 候选机台试算结果
     */
    public Cd90MachineTrial calculate(Cd90CandidateMachineTrialInput input) {
        if (input == null) {
            throw new IllegalArgumentException("候选机台试算输入不能为空");
        }

        // 解析该机台对该规格的损耗率：按四层优先级匹配，未命中时使用兜底损耗率
        Cd90LossRateSelection lossRate = lossRateResolver.resolve(
                input.getClothCode(), input.getMachineCode(), input.getLossRateRules(),
                input.getFallbackLossRatePercent());
        // 计算含损耗的实际排产量：在净需求基础上上浮损耗量，同时受起排量门槛和均分阈值约束
        BigDecimal actualQuantity = quantityCalculator.calculateActualQuantity(
                input.getNetDemandQuantity(), input.isCloseOut(), lossRate.getLossRatePercent(),
                input.getMinimumStartQuantity(), input.getStandardCurlLength(), input.getEqualShareThreshold());
        // 工装试算：根据实际排产量和工装总数（卷轴）计算每台机可同时上机数量
        Cd90ToolingTrial tooling = toolingCalculator.calculate(
                actualQuantity, input.getTotalToolingCount(), input.getOccupiedVehicleCount(),
                input.getStandardCurlLength());

        // 先按机台原预计开工时间计算理论产能，大卷试算只需覆盖实际、工装和理论产能共同允许的上限。
        Cd90MachineCapacityTrial initialCapacity = this.calculateCapacity(
                input, input.getRemainingSeconds(), actualQuantity);
        BigDecimal resourceUpperQuantity = actualQuantity
                .min(tooling.getSchedulableQuantity())
                .min(initialCapacity.getCapacityQuantity());
        if (resourceUpperQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            String limitReason = this.limitReason(actualQuantity, tooling.getSchedulableQuantity(),
                    initialCapacity.getCapacityQuantity(), null, BigDecimal.ZERO);
            return this.zeroTrial(input, lossRate, actualQuantity, tooling,
                    initialCapacity.getCapacityQuantity(), null, limitReason);
        }

        // 大卷静置时效试算允许返回部分可供量；完全无可用流水时才判定本班不可排。
        Cd90BigRollAgingAllocation agingAllocation = this.agingAllocation(input, resourceUpperQuantity);
        if (agingAllocation != null && !agingAllocation.isSuccess()) {
            return this.zeroTrial(input, lossRate, actualQuantity, tooling,
                    BigDecimal.ZERO, agingAllocation,
                    Cd90BigRollAgingAllocator.AGING_PERIOD_LIMIT);
        }

        // 在各成熟释放时点中选择本班可提交量最大的方案，再按最终量重新绑定准确流水。
        BigDecimal agingSchedulableQuantity = agingAllocation == null
                ? resourceUpperQuantity
                : this.maximumAgingSchedulableQuantity(input, resourceUpperQuantity, agingAllocation);
        if (agingSchedulableQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return this.zeroTrial(input, lossRate, actualQuantity, tooling,
                    BigDecimal.ZERO, agingAllocation,
                    Cd90BigRollAgingAllocator.AGING_PERIOD_LIMIT);
        }
        if (agingAllocation != null) {
            agingAllocation = this.agingAllocation(input, agingSchedulableQuantity);
        }

        // 最终任务只等待本次实际使用的全部流水成熟，不等待未进入本次任务的剩余需求。
        int agingDelaySeconds = agingAllocation == null ? 0 : agingAllocation.getDelaySeconds();
        int remainingSeconds = Math.max(0, input.getRemainingSeconds() - agingDelaySeconds);
        Cd90MachineCapacityTrial capacity = this.calculateCapacity(input, remainingSeconds, actualQuantity);
        BigDecimal agingQuantity = agingAllocation == null
                ? resourceUpperQuantity : agingAllocation.getAllocatedQuantity();
        // 最终可排量 = min(实际排产量, 工装限制量, 产能限制量, 大卷成熟流水可供量)。
        BigDecimal finalQuantity = actualQuantity
                .min(tooling.getSchedulableQuantity())
                .min(capacity.getCapacityQuantity())
                .min(agingQuantity);
        if (finalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return this.zeroTrial(input, lossRate, actualQuantity, tooling,
                    capacity.getCapacityQuantity(), agingAllocation,
                    Cd90BigRollAgingAllocator.AGING_PERIOD_LIMIT);
        }

        // 用最终提交量重算生产耗时，避免成熟量或工装限制导致少排时仍扣除完整需求的机台秒数。
        Cd90MachineCapacityTrial executionCapacity = this.calculateCapacity(
                input, remainingSeconds, finalQuantity);
        // 判断瓶颈原因：优先级 工装限制 > 产能限制 > 大卷时效限制
        String limitReason = this.limitReason(actualQuantity, tooling.getSchedulableQuantity(),
                capacity.getCapacityQuantity(), agingQuantity, finalQuantity);
        // 若有时效延迟且产能未能满足实际排产量，以时效限制覆盖其他瓶颈原因
        if (agingDelaySeconds > 0 && capacity.getCapacityQuantity().compareTo(actualQuantity) < 0) {
            limitReason = Cd90BigRollAgingAllocator.AGING_PERIOD_LIMIT;
        }

        // 组装候选机台试算结果
        Cd90MachineTrial result = Cd90MachineTrial.builder()
                .machineCode(input.getMachineCode())
                .lossRatePercent(lossRate.getLossRatePercent())
                .lossRateLevel(lossRate.getMatchedLevel())
                .actualQuantity(actualQuantity)
                .standardCurlLength(input.getStandardCurlLength())
                .toolingQuantity(tooling.getSchedulableQuantity())
                .capacityQuantity(capacity.getCapacityQuantity())
                .finalSchedulableQuantity(finalQuantity)
                .fullyAccommodated(finalQuantity.compareTo(actualQuantity) >= 0)
                .preferredMachine(input.isPreferredMachine())
                .priorityOrder(input.getPriorityOrder())
                .changeSeconds(executionCapacity.getChangeSeconds())
                .productionSeconds(executionCapacity.getProductionSeconds())
                .sameTailSpec(input.getCurrentTail() == null
                            ? input.getPreviousSpec() != null && input.getPreviousSpec().equals(input.getCurrentSpec())
                            : input.getPreviousTail() != null
                                    && input.getPreviousTail().getClothCode() != null
                                    && input.getPreviousTail().getClothCode()
                                            .equals(input.getCurrentTail().getClothCode()))
                .historyMachine(input.isHistoryMachine())
                .remainingSeconds(executionCapacity.getRemainingSeconds())
                    .taskStartTime(agingAllocation == null ? input.getOriginalStartTime()
                            : agingAllocation.getTaskStartTime())
                .agingDelaySeconds(agingDelaySeconds)
                .agingAllocation(agingAllocation)
                .limitReason(limitReason)
                .build();
        log.debug("[直裁自动排程] 候选机台试算完成, clothCode={}, machineCode={}, lossRateLevel={}, "
                        + "actualQuantity={}, toolingQuantity={}, capacityQuantity={}, finalQuantity={}",
                input.getClothCode(), input.getMachineCode(), lossRate.getMatchedLevel(), actualQuantity,
                tooling.getSchedulableQuantity(), capacity.getCapacityQuantity(), finalQuantity);
        return result;
    }

/**
     * 调用大卷时效分配器进行试算；无库存流水或原开工时间为空时不分配。
     */
    private Cd90BigRollAgingAllocation agingAllocation(Cd90CandidateMachineTrialInput input,
                                                       BigDecimal requestedQuantity) {
        if (input.getBigRollAgingStocks() == null || input.getOriginalStartTime() == null) {
            return null;
        }
        return agingAllocator.preview(input.getBigRollAgingStocks(), input.getBigRollCode(),
                requestedQuantity, input.getOriginalStartTime());
    }

    /**
     * 在成熟流水各释放时点中选择本班可排量最大的方案。
     * 同一释放时点的流水整体累计后再比较，最终仍由分配器按稳定顺序绑定准确米数。
     */
    private BigDecimal maximumAgingSchedulableQuantity(Cd90CandidateMachineTrialInput input,
                                                        BigDecimal resourceUpperQuantity,
                                                        Cd90BigRollAgingAllocation allocation) {
        List<Cd90BigRollAgingAllocationItem> items = allocation.getItems();
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal cumulativeQuantity = BigDecimal.ZERO;
        BigDecimal maximumQuantity = BigDecimal.ZERO;
        for (int index = 0; index < items.size(); index++) {
            Cd90BigRollAgingAllocationItem item = items.get(index);
            cumulativeQuantity = cumulativeQuantity.add(item.getQuantity());
            LocalDateTime releaseTime = this.effectiveReleaseTime(item, input.getOriginalStartTime());
            boolean lastAtReleaseTime = index == items.size() - 1
                    || !releaseTime.equals(this.effectiveReleaseTime(items.get(index + 1),
                            input.getOriginalStartTime()));
            if (!lastAtReleaseTime) {
                continue;
            }
            int delaySeconds = this.delaySeconds(input.getOriginalStartTime(), releaseTime);
            int remainingSeconds = Math.max(0, input.getRemainingSeconds() - delaySeconds);
            BigDecimal capacityQuantity = this.calculateCapacity(input, remainingSeconds,
                    resourceUpperQuantity).getCapacityQuantity();
            BigDecimal candidateQuantity = resourceUpperQuantity
                    .min(cumulativeQuantity)
                    .min(capacityQuantity);
            if (candidateQuantity.compareTo(maximumQuantity) > 0) {
                maximumQuantity = candidateQuantity;
            }
        }
        return maximumQuantity;
    }

    /** 按候选机台尾态计算给定剩余时间和请求量下的产能。 */
    private Cd90MachineCapacityTrial calculateCapacity(Cd90CandidateMachineTrialInput input,
                                                        int remainingSeconds,
                                                        BigDecimal requestedQuantity) {
        return input.getCurrentTail() == null
                ? capacityCalculator.calculateWithRemainingSeconds(
                        input.getQuota(), input.getShiftHours(), remainingSeconds,
                        input.getPreviousSpec(), input.getCurrentSpec(), input.getSpecChangeMinutes(),
                        requestedQuantity)
                : capacityCalculator.calculateWithRemainingSeconds(
                        input.getQuota(), input.getShiftHours(), remainingSeconds,
                        input.getPreviousTail(), input.getCurrentTail(),
                        input.getSameRollDiffSpecChangeMinutes(), input.getDiffRollSameSpecChangeMinutes(),
                        input.getDiffRollDiffSpecChangeMinutes(), requestedQuantity);
    }

    /** 取得单条流水相对于机台原预计开工时间的有效释放时点。 */
    private LocalDateTime effectiveReleaseTime(Cd90BigRollAgingAllocationItem item,
                                               LocalDateTime originalStartTime) {
        LocalDateTime releaseTime = item.getStock().getReleaseTime();
        return releaseTime.isAfter(originalStartTime) ? releaseTime : originalStartTime;
    }

    /** 计算释放时点导致的等待秒数，超长等待统一压到整型上限。 */
    private int delaySeconds(LocalDateTime originalStartTime, LocalDateTime releaseTime) {
        long seconds = Duration.between(originalStartTime, releaseTime).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return seconds >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    /** 构造不可排的候选机台试算结果。 */
    private Cd90MachineTrial zeroTrial(Cd90CandidateMachineTrialInput input,
                                       Cd90LossRateSelection lossRate,
                                       BigDecimal actualQuantity,
                                       Cd90ToolingTrial tooling,
                                       BigDecimal capacityQuantity,
                                       Cd90BigRollAgingAllocation agingAllocation,
                                       String limitReason) {
        return Cd90MachineTrial.builder()
                .machineCode(input.getMachineCode())
                .lossRatePercent(lossRate.getLossRatePercent())
                .lossRateLevel(lossRate.getMatchedLevel())
                .actualQuantity(actualQuantity)
                .standardCurlLength(input.getStandardCurlLength())
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

    private String limitReason(BigDecimal actualQuantity, BigDecimal toolingQuantity,
                               BigDecimal capacityQuantity, BigDecimal agingQuantity,
                               BigDecimal finalQuantity) {
        if (actualQuantity == null || finalQuantity == null || finalQuantity.compareTo(actualQuantity) >= 0) {
            return null;
        }
        // 工装是跨机台共享资源；当工装与机台产能同时成为最小瓶颈时，优先暴露工装不足，便于补充大卷工装数量。
        if (toolingQuantity != null && finalQuantity.compareTo(toolingQuantity) == 0
                && toolingQuantity.compareTo(actualQuantity) < 0) {
            return "TOOLING_LIMIT";
        }
        if (capacityQuantity != null && finalQuantity.compareTo(capacityQuantity) == 0
                && capacityQuantity.compareTo(actualQuantity) < 0) {
            return "CAPACITY_LIMIT";
        }
        if (agingQuantity != null && finalQuantity.compareTo(agingQuantity) == 0
                && agingQuantity.compareTo(actualQuantity) < 0) {
            return Cd90BigRollAgingAllocator.AGING_PERIOD_LIMIT;
        }
        return null;
    }
}
