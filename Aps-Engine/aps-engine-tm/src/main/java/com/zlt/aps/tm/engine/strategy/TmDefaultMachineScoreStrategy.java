package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面默认机台评分策略。
 *
 * <p>仅对未过滤候选机台评分，按剩余产能适配、主胶料连续、基部胶相似、同口型连续、
 * 切换成本和定点生产加权求和。方法会修改候选机台评分，不修改任务链。</p>
 */
public class TmDefaultMachineScoreStrategy {

    /**
     * 计算候选机台总分。
     *
     * @param task      胎面任务草稿
     * @param candidate 候选机台
     * @return 机台总分
     * @throws IllegalArgumentException 任务或候选机台为空时抛出
     */
    public BigDecimal score(TmTaskDraft task, TmMachineCandidate candidate) {
        if (task == null || candidate == null) {
            throw new IllegalArgumentException("机台评分入参不能为空");
        }
        if (Boolean.TRUE.equals(candidate.getFiltered())) {
            candidate.setScore(BigDecimal.ZERO);
            return BigDecimal.ZERO;
        }
        BigDecimal capacityScore = capacityFitScore(task, candidate);
        BigDecimal mainGlueScore = same(task.getGlueCode(), candidate.getTailMainGlueCode()) ? BigDecimal.valueOf(20) : BigDecimal.ZERO;
        BigDecimal baseGlueScore = mainGlueScore.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.ZERO : (same(task.getBaseGlueCode(), candidate.getTailBaseGlueCode()) ? BigDecimal.valueOf(15) : BigDecimal.ZERO);
        BigDecimal mouthPlateScore = same(task.getMouthPlateCode(), candidate.getTailMouthPlateCode()) ? BigDecimal.TEN : BigDecimal.ZERO;
        BigDecimal switchCostScore = BigDecimal.TEN.subtract(nvl(candidate.getSwitchCostHours())).max(BigDecimal.ZERO);
        BigDecimal fixedScore = Boolean.TRUE.equals(candidate.getFixedMachineMatched()) ? BigDecimal.TEN : BigDecimal.ZERO;
        BigDecimal totalScore = capacityScore.add(mainGlueScore).add(baseGlueScore)
                .add(mouthPlateScore).add(switchCostScore).add(fixedScore);
        candidate.setScore(totalScore);
        candidate.getEvidence().put("capacityScore", capacityScore);
        candidate.getEvidence().put("mainGlueScore", mainGlueScore);
        candidate.getEvidence().put("baseGlueScore", baseGlueScore);
        candidate.getEvidence().put("mouthPlateScore", mouthPlateScore);
        candidate.getEvidence().put("switchCostScore", switchCostScore);
        candidate.getEvidence().put("fixedScore", fixedScore);
        return totalScore;
    }

    /**
     * 计算剩余产能适配分。
     *
     * @param task      胎面任务草稿
     * @param candidate 候选机台
     * @return 产能适配分，最高 35
     */
    private BigDecimal capacityFitScore(TmTaskDraft task, TmMachineCandidate candidate) {
        BigDecimal remainCapacity = nvl(candidate.getRemainCapacity());
        BigDecimal planQty = nvl(task.getPlanQty());
        if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0 || planQty.compareTo(BigDecimal.ZERO) <= 0
                || remainCapacity.compareTo(planQty) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal wasteRatio = remainCapacity.subtract(planQty).divide(remainCapacity, 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(35).multiply(BigDecimal.ONE.subtract(wasteRatio)).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判断两个编码是否非空且相同。
     *
     * @param left  左侧编码
     * @param right 右侧编码
     * @return true 表示相同
     */
    private boolean same(String left, String right) {
        return left != null && !left.trim().isEmpty() && left.equals(right);
    }

    /**
     * 空值转 0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
