package com.zlt.aps.lh.engine.strategy.support;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 单个Machine×SKU无副作用试算通过后形成的不可变排产提案。
 *
 * <p>提案冻结日期池、反向硬匹配、机台声明范围、资源归属班次和完整真实可开产计划；
 * 只有提案通过后才允许进入正式提交链。</p>
 *
 * @author APS
 */
public final class NewSpecScheduleProposal {

    /** 机台驱动分配声明 */
    private final NewSpecMachineAssignmentPlan assignmentPlan;
    /** 候选原始日期池 */
    private final LocalDate poolDate;
    /** 完整无副作用真实可开产计划 */
    private final NewSpecMachineAvailabilityPlan availabilityPlan;
    /** true-资源按实际可开产时间归班；false-按机台收尾时间归班 */
    private final boolean actualAvailableTimeMode;

    public NewSpecScheduleProposal(
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            Integer resourceShiftIndex,
            LocalDate poolDate,
            NewSpecMachineAvailabilityPlan availabilityPlan,
            boolean actualAvailableTimeMode) {
        this.assignmentPlan = new NewSpecMachineAssignmentPlan(
                candidate, matchResult, resourceShiftIndex, poolDate,
                availabilityPlan, actualAvailableTimeMode);
        this.poolDate = poolDate;
        this.availabilityPlan = Objects.requireNonNull(
                availabilityPlan, "新增排产真实可开产计划不能为空");
        this.actualAvailableTimeMode = actualAvailableTimeMode;
    }

    public NewSpecMachineAssignmentPlan getAssignmentPlan() {
        return assignmentPlan;
    }

    public DailyNewSpecCandidate getCandidate() {
        return assignmentPlan.getCandidate();
    }

    public MachineSkuMatchResult getMatchResult() {
        return assignmentPlan.getMatchResult();
    }

    public LocalDate getPoolDate() {
        return poolDate;
    }

    public NewSpecMachineAvailabilityPlan getAvailabilityPlan() {
        return availabilityPlan;
    }

    public boolean isActualAvailableTimeMode() {
        return actualAvailableTimeMode;
    }
}
