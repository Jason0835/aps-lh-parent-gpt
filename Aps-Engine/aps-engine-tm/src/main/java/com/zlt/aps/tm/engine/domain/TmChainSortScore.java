package com.zlt.aps.tm.engine.domain;

import java.math.BigDecimal;

/**
 * 胎面同机台任务链排序分。
 *
 * <p>用于在同一班次内选择下一个待排任务。排序采用分层比较：先比较主胶料连续，
 * 再比较基部胶相同元素数量、口型连续，最后比较产能适配、切换成本、定点生产等业务分。
 * 该对象只承载排序结果，不修改任务、候选机台或任务链。</p>
 */
public class TmChainSortScore implements Comparable<TmChainSortScore> {

    /** 无连续性命中的默认排序分 */
    public static final TmChainSortScore ZERO = new TmChainSortScore(0, 0, 0, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    /** 是否匹配前置任务主胶料，1 表示匹配 */
    private final int mainGlueMatched;

    /** 匹配前置任务基部胶的元素数量，数量越大优先级越高 */
    private final int baseGlueMatched;

    /** 是否匹配前置任务口型板，1 表示匹配 */
    private final int mouthPlateMatched;

    /** 低优先级业务总分，用于连续性层级相同时继续排序 */
    private final BigDecimal businessScore;

    /** 产能适配分 */
    private final BigDecimal capacityScore;

    /** 主胶料连续分 */
    private final BigDecimal mainGlueScore;

    /** 基部胶连续分 */
    private final BigDecimal baseGlueScore;

    /** 口型板连续分 */
    private final BigDecimal mouthPlateScore;

    /** 切换成本分 */
    private final BigDecimal switchCostScore;

    /** 定点生产分 */
    private final BigDecimal fixedScore;

    /**
     * 创建胎面任务链排序分。
     *
     * @param mainGlueMatched 是否匹配前置任务主胶料
     * @param baseGlueMatched 匹配前置任务基部胶的元素数量
     * @param mouthPlateMatched 是否匹配前置任务口型板
     * @param capacityScore 产能适配分
     * @param mainGlueScore 主胶料连续分
     * @param baseGlueScore 基部胶连续分
     * @param mouthPlateScore 口型板连续分
     * @param switchCostScore 切换成本分
     * @param fixedScore 定点生产分
     */
    public TmChainSortScore(int mainGlueMatched, int baseGlueMatched, int mouthPlateMatched,
                            BigDecimal capacityScore, BigDecimal mainGlueScore, BigDecimal baseGlueScore,
                            BigDecimal mouthPlateScore, BigDecimal switchCostScore, BigDecimal fixedScore) {
        this.mainGlueMatched = mainGlueMatched;
        this.baseGlueMatched = baseGlueMatched;
        this.mouthPlateMatched = mouthPlateMatched;
        this.capacityScore = nvl(capacityScore);
        this.mainGlueScore = nvl(mainGlueScore);
        this.baseGlueScore = nvl(baseGlueScore);
        this.mouthPlateScore = nvl(mouthPlateScore);
        this.switchCostScore = nvl(switchCostScore);
        this.fixedScore = nvl(fixedScore);
        this.businessScore = this.capacityScore.add(this.switchCostScore).add(this.fixedScore);
    }

    /**
     * 空数值转为 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 比较两个链式排序分。
     *
     * @param other 另一个排序分
     * @return 大于 0 表示当前排序分优先级更高
     */
    @Override
    public int compareTo(TmChainSortScore other) {
        if (other == null) {
            return 1;
        }
        int mainGlueCompare = Integer.compare(this.mainGlueMatched, other.mainGlueMatched);
        if (mainGlueCompare != 0) {
            return mainGlueCompare;
        }
        int baseGlueCompare = Integer.compare(this.baseGlueMatched, other.baseGlueMatched);
        if (baseGlueCompare != 0) {
            return baseGlueCompare;
        }
        int mouthPlateCompare = Integer.compare(this.mouthPlateMatched, other.mouthPlateMatched);
        if (mouthPlateCompare != 0) {
            return mouthPlateCompare;
        }
        return this.businessScore.compareTo(other.businessScore);
    }

    /**
     * 获取低优先级业务总分。
     *
     * @return 业务总分
     */
    public BigDecimal getBusinessScore() {
        return businessScore;
    }

    /**
     * 获取主胶料连续命中标识。
     *
     * @return 1 表示匹配，0 表示不匹配
     */
    public int getMainGlueMatched() {
        return mainGlueMatched;
    }

    /**
     * 获取基部胶连续命中元素数量。
     *
     * @return 基部胶交集元素数量
     */
    public int getBaseGlueMatched() {
        return baseGlueMatched;
    }

    /**
     * 获取口型板连续命中标识。
     *
     * @return 1 表示匹配，0 表示不匹配
     */
    public int getMouthPlateMatched() {
        return mouthPlateMatched;
    }

    /**
     * 生成日志友好的排序分说明。
     *
     * @return 排序分说明
     */
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