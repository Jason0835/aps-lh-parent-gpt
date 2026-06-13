package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90CandidateMachineTrialInput;
import com.zlt.aps.cd90.engine.model.Cd90LossRateSelection;
import com.zlt.aps.cd90.engine.model.Cd90MachineCapacityTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import com.zlt.aps.cd90.engine.model.Cd90ToolingTrial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 单个候选机台组合试算器。
 *
 * <p>依次执行损耗率解析、实际排产量、工装和机台产能试算。整个过程不修改资源快照。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Cd90CandidateMachineTrialCalculator {

    private final Cd90LossRateResolver lossRateResolver;
    private final Cd90ScheduleQuantityCalculator quantityCalculator;
    private final Cd90ToolingCalculator toolingCalculator;
    private final Cd90MachineCapacityCalculator capacityCalculator;

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

        Cd90LossRateSelection lossRate = lossRateResolver.resolve(
                input.getClothCode(), input.getMachineCode(), input.getLossRateRules());
        BigDecimal actualQuantity = quantityCalculator.calculateActualQuantity(
                input.getNetDemandQuantity(), input.isCloseOut(), lossRate.getLossRatePercent(),
                input.getMinimumStartQuantity(), input.getCoilMeter());
        Cd90ToolingTrial tooling = toolingCalculator.calculate(
                actualQuantity, input.getTotalToolingCount(), input.getOccupiedVehicleCount(), input.getCoilMeter());
        Cd90MachineCapacityTrial capacity = capacityCalculator.calculateWithRemainingSeconds(
                input.getQuota(), input.getShiftHours(), input.getRemainingSeconds(),
                input.getPreviousSpec(), input.getCurrentSpec(), input.getSpecChangeMinutes(), actualQuantity);
        BigDecimal finalQuantity = actualQuantity
                .min(tooling.getSchedulableQuantity())
                .min(capacity.getCapacityQuantity());

        Cd90MachineTrial result = Cd90MachineTrial.builder()
                .machineCode(input.getMachineCode())
                .lossRatePercent(lossRate.getLossRatePercent())
                .lossRateLevel(lossRate.getMatchedLevel())
                .actualQuantity(actualQuantity)
                .toolingQuantity(tooling.getSchedulableQuantity())
                .capacityQuantity(capacity.getCapacityQuantity())
                .finalSchedulableQuantity(finalQuantity)
                .fullyAccommodated(finalQuantity.compareTo(actualQuantity) >= 0)
                .preferredMachine(input.isPreferredMachine())
                .priorityOrder(input.getPriorityOrder())
                .sameTailSpec(input.getPreviousSpec() != null
                        && input.getPreviousSpec().equals(input.getCurrentSpec()))
                .remainingSeconds(capacity.getRemainingSeconds())
                .build();
        log.debug("[直裁自动排程] 候选机台试算完成, clothCode={}, machineCode={}, lossRateLevel={}, "
                        + "actualQuantity={}, toolingQuantity={}, capacityQuantity={}, finalQuantity={}",
                input.getClothCode(), input.getMachineCode(), lossRate.getMatchedLevel(), actualQuantity,
                tooling.getSchedulableQuantity(), capacity.getCapacityQuantity(), finalQuantity);
        return result;
    }
}
