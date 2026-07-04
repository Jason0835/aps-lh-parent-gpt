package com.zlt.aps.tm.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmMachineRuleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 胎面默认机台评分策略。
 *
 * <p>仅对未过滤候选机台评分，按剩余产能适配(10)、主胶料连续(10)、基部胶相似(8)、
 * 同口型连续(10)、切换成本(10)和定点生产(10)加权求和。方法会修改候选机台评分，不修改任务链。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TmStrategyRegistry} 按编码 "DEFAULT" 收集。</p>
 */
@Component
public class TmDefaultMachineScoreStrategy implements ITmMachineScoreStrategy {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    @Override
    public String getStrategyCode() {
        return "DEFAULT";
    }

    /**
     * 执行候选机台评分。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 评分结果
     * @throws ServiceException 候选机台或上下文为空时抛出
     */
    @Override
    public ScheduleScoreResult score(TmMachineCandidate candidate, TmMachineRuleContext context) {
        if (candidate == null || context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
        }
        ScheduleScoreResult result = new ScheduleScoreResult();
        result.setStrategyCode(getStrategyCode());
        // 已被过滤的机台不参与评分
        if (Boolean.TRUE.equals(candidate.getFiltered())) {
            candidate.setScore(BigDecimal.ZERO);
            result.setTotalScore(BigDecimal.ZERO);
            result.setDescription("机台已被过滤，不参与评分");
            candidate.applyScore(result);
            return result;
        }
        TmTaskDraft task = context.getTaskDraft();
        // 1. 剩余产能适配分（权重 10）
        BigDecimal capacityScore = capacityFitScore(task, candidate);
        // 2. 主胶料连续分（权重 10）
        BigDecimal mainGlueScore = same(task.getGlueCode(), candidate.getTailMainGlueCode())
                ? BigDecimal.TEN : BigDecimal.ZERO;
        // 3. 基部胶相似分（权重 8）：主胶料相同时不再计基部胶分，按基部胶交集元素数量折算分值。
        BigDecimal baseGlueScore = mainGlueScore.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.ZERO
                : calculateBaseGlueSimilarityScore(task.getBaseGlueCode(), candidate.getTailBaseGlueCode());
        // 4. 同口型连续分（权重 10）
        BigDecimal mouthPlateScore = same(task.getMouthPlateCode(), candidate.getTailMouthPlateCode())
                ? BigDecimal.TEN : BigDecimal.ZERO;
        // 5. 切换成本分（权重 10）：切换时长越短分越高
        BigDecimal switchCostScore = BigDecimal.TEN.subtract(nvl(candidate.getSwitchCostHours())).max(BigDecimal.ZERO);
        // 6. 定点生产分（权重 10）
        BigDecimal fixedScore = Boolean.TRUE.equals(candidate.getFixedMachineMatched())
                ? BigDecimal.TEN : BigDecimal.ZERO;
        BigDecimal totalScore = capacityScore.add(mainGlueScore).add(baseGlueScore)
                .add(mouthPlateScore).add(switchCostScore).add(fixedScore);

        Map<String, BigDecimal> scoreItems = new LinkedHashMap<>();
        scoreItems.put("capacityScore", capacityScore);
        scoreItems.put("mainGlueScore", mainGlueScore);
        scoreItems.put("baseGlueScore", baseGlueScore);
        scoreItems.put("mouthPlateScore", mouthPlateScore);
        scoreItems.put("switchCostScore", switchCostScore);
        scoreItems.put("fixedScore", fixedScore);
        result.setScoreItems(scoreItems);
        result.setTotalScore(totalScore);
        result.setDescription("默认评分：产能10/主胶料10/基部胶8/口型10/切换10/定点10");
        candidate.setScore(totalScore);
        candidate.getEvidence().putAll(scoreItems);
        candidate.applyScore(result);
        return result;
    }

    /**
     * 计算剩余产能适配分。
     *
     * @param task      胎面任务草稿
     * @param candidate 候选机台
     * @return 产能适配分，最高 10
     */
    private BigDecimal capacityFitScore(TmTaskDraft task, TmMachineCandidate candidate) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal remainCapacity = nvl(candidate.getRemainCapacity());
        BigDecimal planQty = nvl(task.getPlanQty());
        if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0 || planQty.compareTo(BigDecimal.ZERO) <= 0
                || remainCapacity.compareTo(planQty) < 0) {
            return BigDecimal.ZERO;
        }
        // 产能利用率越高分越高：产能完全利用得满分 10，剩余越多浪费越多分越低
        BigDecimal wasteRatio = remainCapacity.subtract(planQty)
                .divide(remainCapacity, 6, RoundingMode.HALF_UP);
        return BigDecimal.TEN.multiply(BigDecimal.ONE.subtract(wasteRatio))
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
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
     * 按基部胶交集元素数量计算相似分。
     *
     * @param left  当前任务基部胶编码
     * @param right 链尾基部胶编码
     * @return 基部胶相似分，最高 8 分
     */
    private BigDecimal calculateBaseGlueSimilarityScore(String left, String right) {
        Set<String> leftSet = parseBaseGlueSet(left);
        Set<String> rightSet = parseBaseGlueSet(right);
        int intersectionCount = calculateIntersectionCount(leftSet, rightSet);
        if (leftSet.isEmpty() || intersectionCount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(8).multiply(BigDecimal.valueOf(intersectionCount))
                .divide(BigDecimal.valueOf(leftSet.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 将逗号分隔的基部胶编码拆分为元素集合，去除空白并忽略重复元素。
     *
     * @param baseGlueCode 基部胶编码串
     * @return 基部胶元素集合
     */
    private Set<String> parseBaseGlueSet(String baseGlueCode) {
        Set<String> baseGlueSet = new HashSet<>();
        if (baseGlueCode == null || baseGlueCode.trim().isEmpty()) {
            return baseGlueSet;
        }
        for (String item : baseGlueCode.split(",")) {
            String value = item == null ? "" : item.trim();
            if (!value.isEmpty()) {
                baseGlueSet.add(value);
            }
        }
        return baseGlueSet;
    }

    /**
     * 计算两个基部胶集合的交集元素数量。
     *
     * @param leftSet  左侧基部胶集合
     * @param rightSet 右侧基部胶集合
     * @return 交集元素数量
     */
    private int calculateIntersectionCount(Set<String> leftSet, Set<String> rightSet) {
        if (leftSet.isEmpty() || rightSet.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String item : leftSet) {
            if (rightSet.contains(item)) {
                count++;
            }
        }
        return count;
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
