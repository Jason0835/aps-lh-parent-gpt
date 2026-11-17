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

    /**
     * 主胶料连续匹配标识（1 表示匹配，0 表示不匹配）。
     */
    private final int mainGlueMatched;
    /** 基部胶连续匹配数量（匹配度越高值越大）。 */
    private final int baseGlueMatched;
    /** 口型板连续匹配标识（1 表示匹配，0 表示不匹配）。 */
    private final int mouthPlateMatched;
    /** 业务维度汇总得分（产能评分 + 切换成本评分 + 定点机台评分）。 */
    private final BigDecimal businessScore;
    /** 剩余产能适配维度得分。 */
    private final BigDecimal capacityScore;
    /** 主胶料连续性维度得分。 */
    private final BigDecimal mainGlueScore;
    /** 基部胶相似性维度得分。 */
    private final BigDecimal baseGlueScore;
    /** 口型板连续性维度得分。 */
    private final BigDecimal mouthPlateScore;
    /** 换型切换成本维度得分。 */
    private final BigDecimal switchCostScore;
    /** 定点机台维度得分。 */
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
