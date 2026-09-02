package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/**
 * TM/TC 同机台任务链排序分公共模型。
 *
 */
public class ScheduleChainSortScoreModel implements Comparable<ScheduleChainSortScoreModel> {

    /** 无连续性加分时的默认排序分。 */
    public static final ScheduleChainSortScoreModel ZERO = new ScheduleChainSortScoreModel(0, 0, 0,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    private final int mainGlueMatched;
    private final int baseGlueMatched;
    private final int mouthPlateMatched;
    private final BigDecimal businessScore;
    private final BigDecimal capacityScore;
    private final BigDecimal mainGlueScore;
    private final BigDecimal baseGlueScore;
    private final BigDecimal mouthPlateScore;
    private final BigDecimal switchCostScore;
    private final BigDecimal fixedScore;

    /**
     * 创建同机台任务链排序分。
     *
     * @param mainGlueMatched 主胶匹配标识
     * @param baseGlueMatched 基部胶匹配数量
     * @param mouthPlateMatched 口型板匹配标识
     * @param capacityScore 产能评分
     * @param mainGlueScore 主胶评分
     * @param baseGlueScore 基部胶评分
     * @param mouthPlateScore 口型板评分
     * @param switchCostScore 切换成本评分
     * @param fixedScore 定点机台评分
     */
    public ScheduleChainSortScoreModel(int mainGlueMatched, int baseGlueMatched, int mouthPlateMatched,
                                       BigDecimal capacityScore, BigDecimal mainGlueScore,
                                       BigDecimal baseGlueScore, BigDecimal mouthPlateScore,
                                       BigDecimal switchCostScore, BigDecimal fixedScore) {
        this.mainGlueMatched = mainGlueMatched;
        this.baseGlueMatched = baseGlueMatched;
        this.mouthPlateMatched = mouthPlateMatched;
        this.capacityScore = this.nvl(capacityScore);
        this.mainGlueScore = this.nvl(mainGlueScore);
        this.baseGlueScore = this.nvl(baseGlueScore);
        this.mouthPlateScore = this.nvl(mouthPlateScore);
        this.switchCostScore = this.nvl(switchCostScore);
        this.fixedScore = this.nvl(fixedScore);
        this.businessScore = this.capacityScore.add(this.switchCostScore).add(this.fixedScore);
    }

    @Override
    public int compareTo(ScheduleChainSortScoreModel other) {
        if (other == null) {
            return 1;
        }
        int mainGlueCompare = Integer.compare(this.mainGlueMatched, other.getMainGlueMatched());
        if (mainGlueCompare != 0) {
            return mainGlueCompare;
        }
        int baseGlueCompare = Integer.compare(this.baseGlueMatched, other.getBaseGlueMatched());
        if (baseGlueCompare != 0) {
            return baseGlueCompare;
        }
        int mouthPlateCompare = Integer.compare(this.mouthPlateMatched, other.getMouthPlateMatched());
        if (mouthPlateCompare != 0) {
            return mouthPlateCompare;
        }
        return this.businessScore.compareTo(other.getBusinessScore());
    }

    public BigDecimal getBusinessScore() {
        return businessScore;
    }

    public int getMainGlueMatched() {
        return mainGlueMatched;
    }

    public int getBaseGlueMatched() {
        return baseGlueMatched;
    }

    public int getMouthPlateMatched() {
        return mouthPlateMatched;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public String toString() {
        return "{mainGlueMatched=" + mainGlueMatched
                + ", baseGlueMatched=" + baseGlueMatched
                + ", mouthPlateMatched=" + mouthPlateMatched
                + ", capacityScore=" + capacityScore
                + ", mainGlueScore=" + mainGlueScore
                + ", baseGlueScore=" + baseGlueScore
                + ", mouthPlateScore=" + mouthPlateScore
                + ", switchCostScore=" + switchCostScore
                + ", fixedScore=" + fixedScore
                + ", businessScore=" + businessScore
                + "}";
    }
}
