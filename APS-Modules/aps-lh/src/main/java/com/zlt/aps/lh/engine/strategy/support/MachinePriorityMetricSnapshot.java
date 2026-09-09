/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.support;

import org.apache.commons.lang3.StringUtils;

/**
 * 选机时点单台候选机台的软排序指标快照。
 *
 * <p>新增排产会在候选选中后先分配目标模具并推进机台运行态，选机日志则延迟到结果提交后输出。
 * 因此，所有依赖机台前状态或模具绑定的指标必须在正式分配前一次性冻结，正式排序与日志展示
 * 统一读取同一套计算口径，避免把本轮刚分配的目标模具误认为选机前在机模具。</p>
 *
 * @author APS
 */
public class MachinePriorityMetricSnapshot {

    /** 单控机台软排序分。 */
    private final int singleControlScore;

    /** 是否为单控机台。 */
    private final boolean singleControlMachine;

    /** 同胎胚软排序分。 */
    private final int embryoMatchScore;

    /** 同胎胚实际命中值。 */
    private final String embryoMatchedValue;

    /** 同模壳软排序分。 */
    private final int mouldShellMatchScore;

    /** 同模壳实际命中值。 */
    private final String mouldShellMatchedValue;

    /** 当前候选预分配到的具体目标模具号。 */
    private final String targetMouldCodes;

    /** 当前候选预分配到的具体目标模壳型号。 */
    private final String targetMouldShellStandards;

    /** 选机前候选机台实际绑定模具号。 */
    private final String machineBoundMouldCodes;

    /** 选机前候选机台实际绑定模壳型号。 */
    private final String machineBoundMouldShellStandards;

    /** 同规格软排序分。 */
    private final int specMatchScore;

    /** 同规格实际命中值。 */
    private final String specMatchedValue;

    /** 胶囊共用性软排序分。 */
    private final int capsuleScore;

    /** 同英寸软排序分。 */
    private final int proSizeMatchScore;

    /** 同英寸实际命中值。 */
    private final String proSizeMatchedValue;

    /** 相近英寸距离。 */
    private final double inchDistance;

    /** 模套硬兼容结果。 */
    private final boolean mouldSetHardCompatible;

    /**
     * 构建候选机台软排序指标快照。
     *
     * @param singleControlScore 单控机台软排序分
     * @param singleControlMachine 是否为单控机台
     * @param embryoMatchScore 同胎胚软排序分
     * @param embryoMatchedValue 同胎胚实际命中值
     * @param mouldShellMatchScore 同模壳软排序分
     * @param mouldShellMatchedValue 同模壳实际命中值
     * @param targetMouldCodes 当前候选预分配到的具体目标模具号
     * @param targetMouldShellStandards 当前候选预分配到的具体目标模壳型号
     * @param machineBoundMouldCodes 选机前候选机台实际绑定模具号
     * @param machineBoundMouldShellStandards 选机前候选机台实际绑定模壳型号
     * @param specMatchScore 同规格软排序分
     * @param specMatchedValue 同规格实际命中值
     * @param capsuleScore 胶囊共用性软排序分
     * @param proSizeMatchScore 同英寸软排序分
     * @param proSizeMatchedValue 同英寸实际命中值
     * @param inchDistance 相近英寸距离
     * @param mouldSetHardCompatible 模套硬兼容结果
     */
    public MachinePriorityMetricSnapshot(
            int singleControlScore,
            boolean singleControlMachine,
            int embryoMatchScore,
            String embryoMatchedValue,
            int mouldShellMatchScore,
            String mouldShellMatchedValue,
            String targetMouldCodes,
            String targetMouldShellStandards,
            String machineBoundMouldCodes,
            String machineBoundMouldShellStandards,
            int specMatchScore,
            String specMatchedValue,
            int capsuleScore,
            int proSizeMatchScore,
            String proSizeMatchedValue,
            double inchDistance,
            boolean mouldSetHardCompatible) {
        this.singleControlScore = singleControlScore;
        this.singleControlMachine = singleControlMachine;
        this.embryoMatchScore = embryoMatchScore;
        this.embryoMatchedValue = embryoMatchedValue;
        this.mouldShellMatchScore = mouldShellMatchScore;
        this.mouldShellMatchedValue = mouldShellMatchedValue;
        this.targetMouldCodes = targetMouldCodes;
        this.targetMouldShellStandards = targetMouldShellStandards;
        this.machineBoundMouldCodes = machineBoundMouldCodes;
        this.machineBoundMouldShellStandards = machineBoundMouldShellStandards;
        this.specMatchScore = specMatchScore;
        this.specMatchedValue = specMatchedValue;
        this.capsuleScore = capsuleScore;
        this.proSizeMatchScore = proSizeMatchScore;
        this.proSizeMatchedValue = proSizeMatchedValue;
        this.inchDistance = inchDistance;
        this.mouldSetHardCompatible = mouldSetHardCompatible;
    }

    /**
     * 比较冻结的完整适配指标；未知距离沿用生成快照时的既有取值。
     *
     * @param left 左侧完整指标快照
     * @param right 右侧完整指标快照
     * @return 负数表示左侧更适配，全部指标相同返回0
     */
    public static int compareSoftMatch(MachinePriorityMetricSnapshot left,
                                       MachinePriorityMetricSnapshot right) {
        int compareResult = Integer.compare(
                left.getEmbryoMatchScore(), right.getEmbryoMatchScore());
        if (compareResult != 0) {
            return compareResult;
        }

        // 同模壳优先级高于同规格，避免同一窗口内规格命中机台抢占更匹配模壳能力的机台。
        compareResult = Integer.compare(
                left.getMouldShellMatchScore(), right.getMouldShellMatchScore());
        if (compareResult != 0) {
            return compareResult;
        }

        compareResult = Integer.compare(
                left.getSpecMatchScore(), right.getSpecMatchScore());
        if (compareResult != 0) {
            return compareResult;
        }

        compareResult = Integer.compare(
                left.getCapsuleScore(), right.getCapsuleScore());
        if (compareResult != 0) {
            return compareResult;
        }

        compareResult = Integer.compare(
                left.getProSizeMatchScore(), right.getProSizeMatchScore());
        if (compareResult != 0) {
            return compareResult;
        }

        return Double.compare(left.getInchDistance(), right.getInchDistance());
    }

    /**
     * 输出固定顺序的完整适配指标，前五项0表示命中；仅供胜出日志及已开启的调试日志使用。
     *
     * @return 同胎胚、同模壳、同规格、胶囊共用、同英寸、实际英寸差
     */
    public String describeSoftMatch() {
        return new StringBuilder(96)
                .append("同胎胚=").append(embryoMatchScore)
                .append(",同模壳=").append(mouldShellMatchScore)
                .append(",同规格=").append(specMatchScore)
                .append(",胶囊共用=").append(capsuleScore)
                .append(",同英寸=").append(proSizeMatchScore)
                .append(",英寸差=").append(inchDistance).toString();
    }

    /**
     * 解析首次决定完整适配胜负的维度，仅在调试日志启用时调用。
     *
     * @param other 对方冻结指标
     * @return 首个不同的指标名称，全部相同时返回完整适配同分
     */
    public String resolveSoftMatchDecisionDimension(MachinePriorityMetricSnapshot other) {
        if (embryoMatchScore != other.embryoMatchScore) {
            return "同胎胚";
        }
        if (mouldShellMatchScore != other.mouldShellMatchScore) {
            return "同模壳";
        }
        if (specMatchScore != other.specMatchScore) {
            return "同规格";
        }
        if (capsuleScore != other.capsuleScore) {
            return "胶囊共用";
        }
        if (proSizeMatchScore != other.proSizeMatchScore) {
            return "同英寸";
        }
        if (Double.compare(inchDistance, other.inchDistance) != 0) {
            return "实际英寸差";
        }
        return "完整适配同分";
    }

    public int getSingleControlScore() {
        return singleControlScore;
    }

    public boolean isSingleControlMachine() {
        return singleControlMachine;
    }

    public int getEmbryoMatchScore() {
        return embryoMatchScore;
    }

    public String getEmbryoMatchedValue() {
        return embryoMatchedValue;
    }

    public int getMouldShellMatchScore() {
        return mouldShellMatchScore;
    }

    public String getMouldShellMatchedValue() {
        return mouldShellMatchedValue;
    }

    public String getTargetMouldCodes() {
        return targetMouldCodes;
    }

    public String getTargetMouldShellStandards() {
        return targetMouldShellStandards;
    }

    public String getMachineBoundMouldCodes() {
        return machineBoundMouldCodes;
    }

    public String getMachineBoundMouldShellStandards() {
        return machineBoundMouldShellStandards;
    }

    public int getSpecMatchScore() {
        return specMatchScore;
    }

    public String getSpecMatchedValue() {
        return specMatchedValue;
    }

    public int getCapsuleScore() {
        return capsuleScore;
    }

    public int getProSizeMatchScore() {
        return proSizeMatchScore;
    }

    public String getProSizeMatchedValue() {
        return proSizeMatchedValue;
    }

    public double getInchDistance() {
        return inchDistance;
    }

    public boolean isMouldSetHardCompatible() {
        return mouldSetHardCompatible;
    }

    /**
     * 将空指标统一格式化为日志占位符。
     *
     * @param value 指标文本
     * @return 非空原值或“-”
     */
    public static String resolveTraceText(String value) {
        return StringUtils.isEmpty(value) ? "-" : value;
    }
}
