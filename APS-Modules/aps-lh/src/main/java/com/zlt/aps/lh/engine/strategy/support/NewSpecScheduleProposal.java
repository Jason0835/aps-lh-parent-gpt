package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
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

    /** 仅55寸机台允许在匹配并列后按硫化余量选择SKU。 */
    private static final BigDecimal CURING_SURPLUS_PRIORITY_DIMENSION = BigDecimalUtils.valueOf(55);

    /** 机台驱动分配声明 */
    private final NewSpecMachineAssignmentPlan assignmentPlan;
    /** 候选原始日期池 */
    private final LocalDate poolDate;
    /** 完整无副作用真实可开产计划 */
    private final NewSpecMachineAvailabilityPlan availabilityPlan;
    /** true-资源按实际可开产时间归班；false-按机台收尾时间归班 */
    private final boolean actualAvailableTimeMode;
    /** 当前提案是否来自标准S4.5的55寸资源；固定指令由比较入口继续隔离。 */
    private final boolean fiftyFiveDimension;
    /** 形成提案时的统一硫化余量，只在本轮比较和日志中使用。 */
    private final int competitionSurplusQty;
    /** 形成提案时既有SKU全局名次，非正数保持既有未排名语义。 */
    private final int competitionSortRank;

    public NewSpecScheduleProposal(
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            Integer resourceShiftIndex,
            LocalDate poolDate,
            NewSpecMachineAvailabilityPlan availabilityPlan,
            boolean actualAvailableTimeMode) {
        this(candidate, matchResult, resourceShiftIndex, poolDate,
                availabilityPlan, actualAvailableTimeMode, null);
    }

    /**
     * 冻结当前轮55寸并列选择指标，不复制或扣减SKU业务账本。
     *
     * @param candidate 当前日期池候选
     * @param matchResult 已通过校验的机台匹配结果
     * @param resourceShiftIndex 资源归属班次
     * @param poolDate 原始日期池
     * @param availabilityPlan 完整真实可开产计划
     * @param actualAvailableTimeMode 是否按实际可开产时间归班
     * @param dimensionSize 标准S4.5已解析的资源尺寸；辅助入口不传尺寸
     */
    public NewSpecScheduleProposal(
            DailyNewSpecCandidate candidate,
            MachineSkuMatchResult matchResult,
            Integer resourceShiftIndex,
            LocalDate poolDate,
            NewSpecMachineAvailabilityPlan availabilityPlan,
            boolean actualAvailableTimeMode,
            BigDecimal dimensionSize) {
        this.assignmentPlan = new NewSpecMachineAssignmentPlan(
                candidate, matchResult, resourceShiftIndex, poolDate,
                availabilityPlan, actualAvailableTimeMode);
        this.poolDate = poolDate;
        this.availabilityPlan = Objects.requireNonNull(
                availabilityPlan, "新增排产真实可开产计划不能为空");
        this.actualAvailableTimeMode = actualAvailableTimeMode;
        this.fiftyFiveDimension = Objects.nonNull(dimensionSize)
                && CURING_SURPLUS_PRIORITY_DIMENSION.compareTo(dimensionSize) == 0;
        this.competitionSurplusQty = candidate.getSku().getSurplusQty();
        this.competitionSortRank = candidate.getSku().getSortRank();
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

    /** @return 当前提案是否属于标准S4.5的55寸资源 */
    public boolean isFiftyFiveDimension() {
        return fiftyFiveDimension;
    }

    /** @return 本轮冻结的统一硫化余量 */
    public int getCompetitionSurplusQty() {
        return competitionSurplusQty;
    }

    /** @return 本轮冻结的既有SKU全局名次 */
    public int getCompetitionSortRank() {
        return competitionSortRank;
    }
}
