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
