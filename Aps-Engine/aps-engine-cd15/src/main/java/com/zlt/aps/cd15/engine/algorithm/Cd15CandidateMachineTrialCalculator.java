package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15CandidateMachineTrialInput;
import com.zlt.aps.cd15.engine.model.Cd15LossRateSelection;
import com.zlt.aps.cd15.engine.model.Cd15MachineCapacityTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15ToolingTrial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 单个候选机台组合试算器。
 *
 * <p>依次执行损耗率解析、实际排产量、工装和机台产能试算。整个过程不修改资源快照。</p>
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

        // 解析该机台对该规格的损耗率：按四层优先级匹配，未命中时使用兜底损耗率
        Cd15LossRateSelection lossRate = lossRateResolver.resolve(
                input.getSteelStripCode(), input.getMachineCode(), input.getLossRateRules(),
                input.getFallbackLossRatePercent());
        // 计算含损耗的实际排产量，并按当前班该钢带剩余额度封顶。
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
        // 工装试算：根据实际排产量和工装总数（卷轴）计算每台机可同时上机数量
        Cd15ToolingTrial tooling = input.isSingleSpecSplit()
                ? toolingCalculator.calculateSingleSpecSplit(
                        actualQuantity, input.getTotalToolingCount(),
                        input.getOccupiedVehicleCount(), input.getVehiclePlanQuantity())
                : toolingCalculator.calculate(
                        actualQuantity, input.getTotalToolingCount(),
                        input.getOccupiedVehicleCount(), input.getVehiclePlanQuantity());
        // 大卷静置时效分配：按大卷释放时间排序，判断是否满足本班次用量，返回延迟秒数或失败
        Cd15BigRollAgingAllocation agingAllocation = agingAllocation(input, actualQuantity);
        // 大卷时效分配失败（如时效期不足），产能直接置零，标记为大卷时效限制
        if (agingAllocation != null && !agingAllocation.isSuccess()) {
            return Cd15MachineTrial.builder()
                    .machineCode(input.getMachineCode())
                    .lossRatePercent(lossRate.getLossRatePercent())
                    .lossRateLevel(lossRate.getMatchedLevel())
                    .actualQuantity(actualQuantity)
                    .equalShareApplied(equalShareApplied)
                    .equalShareRemainderQuantity(equalShareRemainderQuantity)
                    .remainingSpecShiftQuantity(
                            input.getRemainingSpecShiftQuantity())
                    .vehiclePlanQuantity(input.getVehiclePlanQuantity())
                    .toolingQuantity(tooling.getSchedulableQuantity())
                    .capacityQuantity(BigDecimal.ZERO)
                    .finalSchedulableQuantity(BigDecimal.ZERO)
                    .fullyAccommodated(false)
                    .preferredMachine(input.isPreferredMachine())
                    .historyMachine(input.isHistoryMachine())
                    .priorityOrder(input.getPriorityOrder())
                    .remainingSeconds(input.getRemainingSeconds())
                    .taskStartTime(input.getOriginalStartTime())
                    .agingAllocation(agingAllocation)
                    .limitReason(Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT)
                    .build();
        }
        // 时效分配成功或无需分配：计算因大卷时效产生的延迟秒数，从机台剩余时间中扣除
        // 扣除时效延迟后的机台剩余可用秒数
        int agingDelaySeconds = agingAllocation == null ? 0 : agingAllocation.getDelaySeconds();
        int remainingSeconds = Math.max(0, input.getRemainingSeconds() - agingDelaySeconds);
        // 产能试算：无尾匹时按规格切换耗时计算，有尾匹时按尾匹切换耗时计算
        Cd15MachineCapacityTrial capacity = input.getCurrentTail() == null
                ? capacityCalculator.calculateWithRemainingSeconds(
                        input.getShiftCapacity(), input.getShiftHours(), remainingSeconds,
                        input.getPreviousSpec(), input.getCurrentSpec(), input.getSpecChangeMinutes(), actualQuantity)
                : capacityCalculator.calculateWithRemainingSeconds(
                        input.getShiftCapacity(), input.getShiftHours(), remainingSeconds,
                        input.getPreviousTail(), input.getCurrentTail(),
                        input.getSameRollDiffSpecChangeMinutes(), input.getDiffRollSameSpecChangeMinutes(),
                        input.getDiffRollDiffSpecChangeMinutes(), actualQuantity);
        // 最终可排量 = min(实际排产量, 工装限制量, 产能限制量)，三者取最小
        BigDecimal finalQuantity = actualQuantity
                .min(tooling.getSchedulableQuantity())
                .min(capacity.getCapacityQuantity());
        if (input.isSingleSpecSplit()) {
            finalQuantity = quantityCalculator.roundSingleSpecSplitDown(
                    finalQuantity, input.getCraftWidth());
        }
        int productionSeconds = capacity.getProductionSeconds();
        int trialRemainingSeconds = capacity.getRemainingSeconds();
        if (input.isSingleSpecSplit()
                && finalQuantity.compareTo(capacity.getCapacityQuantity()) < 0) {
            int fullShiftSeconds = Math.multiplyExact(
                    input.getShiftHours(), 3600);
            productionSeconds = finalQuantity.signum() <= 0 ? 0
                    : finalQuantity.multiply(BigDecimal.valueOf(fullShiftSeconds))
                            .divide(input.getShiftCapacity(), 0,
                                    RoundingMode.CEILING)
                            .intValueExact();
            trialRemainingSeconds = Math.max(0,
                    remainingSeconds - capacity.getChangeSeconds()
                            - productionSeconds);
        }
        // 判断瓶颈原因：优先级 工装限制 > 产能限制 > 大卷时效限制
        String limitReason = limitReason(actualQuantity, tooling.getSchedulableQuantity(),
                capacity.getCapacityQuantity(), finalQuantity);
        // 若有时效延迟且产能未能满足实际排产量，以时效限制覆盖其他瓶颈原因
        if (agingDelaySeconds > 0 && capacity.getCapacityQuantity().compareTo(actualQuantity) < 0) {
            limitReason = Cd15BigRollAgingAllocator.AGING_PERIOD_LIMIT;
        }

        // 组装候选机台试算结果
        Cd15MachineTrial result = Cd15MachineTrial.builder()
                .machineCode(input.getMachineCode())
                .lossRatePercent(lossRate.getLossRatePercent())
                .lossRateLevel(lossRate.getMatchedLevel())
                .actualQuantity(actualQuantity)
                .equalShareApplied(equalShareApplied)
                .equalShareRemainderQuantity(equalShareRemainderQuantity)
                .remainingSpecShiftQuantity(
                        input.getRemainingSpecShiftQuantity())
                .vehiclePlanQuantity(input.getVehiclePlanQuantity())
                .toolingQuantity(tooling.getSchedulableQuantity())
                .capacityQuantity(capacity.getCapacityQuantity())
                .finalSchedulableQuantity(finalQuantity)
                .fullyAccommodated(finalQuantity.compareTo(actualQuantity) >= 0)
                .preferredMachine(input.isPreferredMachine())
                .priorityOrder(input.getPriorityOrder())
                .changeSeconds(capacity.getChangeSeconds())
                .productionSeconds(productionSeconds)
                .sameTailSpec(input.getCurrentTail() == null
                            ? input.getPreviousSpec() != null && input.getPreviousSpec().equals(input.getCurrentSpec())
                            : input.getPreviousTail() != null
                                    && input.getPreviousTail().getSteelStripCode() != null
                                    && input.getPreviousTail().getSteelStripCode()
                                            .equals(input.getCurrentTail().getSteelStripCode()))
                .historyMachine(input.isHistoryMachine())
                .remainingSeconds(trialRemainingSeconds)
                    .taskStartTime(agingAllocation == null ? input.getOriginalStartTime()
                            : agingAllocation.getTaskStartTime())
                .agingDelaySeconds(agingDelaySeconds)
                .agingAllocation(agingAllocation)
                .limitReason(limitReason)
                .build();
        log.debug("[斜裁自动排程] 候选机台试算完成, steelStripCode={}, machineCode={}, lossRateLevel={}, "
                        + "actualQuantity={}, toolingQuantity={}, capacityQuantity={}, finalQuantity={}",
                input.getSteelStripCode(), input.getMachineCode(), lossRate.getMatchedLevel(), actualQuantity,
                tooling.getSchedulableQuantity(), capacity.getCapacityQuantity(), finalQuantity);
        return result;
    }

/**
     * 调用大卷时效分配器进行试算；无库存流水或原开工时间为空时不分配。
     */
    private Cd15BigRollAgingAllocation agingAllocation(Cd15CandidateMachineTrialInput input,
                                                       BigDecimal actualQuantity) {
        if (input.getBigRollAgingStocks() == null
                || input.getBigRollAgingStocks().isEmpty()
                || input.getOriginalStartTime() == null) {
            return null;
        }
        BigDecimal bigRollConsumeQuantity = bigRollMeterCalculator.calculateForPlanQuantity(
                actualQuantity, input.getUnitConsumeMillimeter(),
                input.getCraftWidth(), input.getCordWidth(),
                input.getSteelStripCode(), input.getBigRollCode());
        return agingAllocator.preview(input.getBigRollAgingStocks(), input.getBigRollCode(),
                bigRollConsumeQuantity, input.getOriginalStartTime());
    }
    private String limitReason(BigDecimal actualQuantity, BigDecimal toolingQuantity,
                               BigDecimal capacityQuantity, BigDecimal finalQuantity) {
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
        return null;
    }
}
