package com.zlt.aps.tc.engine.strategy;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.TcMachineCandidate;
import com.zlt.aps.tc.engine.domain.TcMachineRuleContext;
import com.zlt.aps.tc.engine.domain.TcParamValue;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.util.TcGlueSimilarityUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎侧默认机台评分策略。
 *
 * <p>仅对未过滤候选机台评分，按剩余产能适配(10)、主胶料连续(10)、基部胶相似(8)、
 * 同口型连续(10)、切换成本(10)和定点生产(10)加权求和。方法会修改候选机台评分，不修改任务链。
 * 通过 {@link Component} 注册为 Spring Bean，由 {@link TcStrategyRegistry}
 * 按 {@link TcScheduleStrategyEnum#DEFAULT} 编码收集。</p>
 */
@Component
public class TcDefaultMachineScoreStrategy implements ITcMachineScoreStrategy {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    @Override
    public String getStrategyCode() {
        return TcScheduleStrategyEnum.DEFAULT.getCode();
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
    public ScheduleScoreResult score(TcMachineCandidate candidate, TcMachineRuleContext context) {
        if (candidate == null || context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_MACHINE_CANDIDATE_EMPTY.getDefaultMessage());
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
        TcTaskDraft task = context.getTaskDraft();
        BigDecimal remainCapacityWeight = this.resolveWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_REMAIN_CAP,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_REMAIN_CAP);
        BigDecimal mainGlueWeight = this.resolveWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_GLUE_CONT,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_GLUE_CONT);
        BigDecimal baseGlueWeight = this.resolveWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_BASE_GLUE,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_BASE_GLUE);
        BigDecimal mouthPlateWeight = this.resolveWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_MOUTH_CONT,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_MOUTH_CONT);
        BigDecimal switchCostWeight = this.resolveWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_SWITCH_COST,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_SWITCH_COST);
        BigDecimal fixedMachineWeight = this.resolveWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_FIXED_MACHINE,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_FIXED_MACHINE);
        // 1. 剩余产能适配分
        BigDecimal capacityScore = this.capacityFitScore(task, candidate, remainCapacityWeight);
        // 2. 主胶料连续分
        BigDecimal mainGlueScore = TcGlueSimilarityUtils.isSameNonBlank(
                task.getGlueCode(), candidate.getTailMainGlueCode())
                ? mainGlueWeight : BigDecimal.ZERO;
        // 3. 基部胶相似分：主胶料相同时不再计基部胶分，按基部胶交集元素数量折算分值。
        BigDecimal baseGlueScore = mainGlueScore.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.ZERO
                : TcGlueSimilarityUtils.calculateSimilarityScore(task.getBaseGlueCode(),
                        candidate.getTailBaseGlueCode(), baseGlueWeight);
        // 4. 同口型连续分
        BigDecimal mouthPlateScore = TcGlueSimilarityUtils.isSameNonBlank(
                task.getMouthPlateCode(), candidate.getTailMouthPlateCode())
                ? mouthPlateWeight : BigDecimal.ZERO;
        // 5. 切换成本分：切换时长越短分越高
        BigDecimal switchCostScore = switchCostWeight.subtract(
                this.nvl(candidate.getSwitchCostHours())).max(BigDecimal.ZERO);
        // 6. 定点生产分
        BigDecimal fixedScore = Boolean.TRUE.equals(candidate.getFixedMachineMatched())
                ? fixedMachineWeight : BigDecimal.ZERO;
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
        result.setDescription("按当前 TC_SCORE_WEIGHT_* 参数完成机台评分");
        candidate.setScore(totalScore);
        candidate.getEvidence().putAll(scoreItems);
        candidate.applyScore(result);
        return result;
    }

    /**
     * 计算剩余产能适配分。
     *
     * @param task      胎侧任务草稿
     * @param candidate 候选机台
     * @param weight 当前评分项权重
     * @return 产能适配分，最高为当前权重
     */
    private BigDecimal capacityFitScore(TcTaskDraft task, TcMachineCandidate candidate, BigDecimal weight) {
        if (task == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal remainCapacity = this.nvl(candidate.getRemainCapacity());
        BigDecimal planQty = this.nvl(task.getPlanQty());
        if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0 || planQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // 当前任务量超过单机剩余产能时仍可同班拆分，按本机实际可排量计算填充率。
        BigDecimal assignedQty = planQty.min(remainCapacity);
        BigDecimal fillRatio = assignedQty.divide(remainCapacity,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
        return weight.multiply(fillRatio)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 从本次排程参数快照读取评分权重，非法值回退为详设默认值。
     *
     * @param context 机台规则上下文
     * @param paramCode 参数编码
     * @param defaultValue 详设默认值
     * @return 非负评分权重
     */
    private BigDecimal resolveWeight(TcMachineRuleContext context, String paramCode, String defaultValue) {
        String effectiveValue = defaultValue;
        if (context.getScheduleContext() != null) {
            TcParamValue paramValue = context.getScheduleContext().getParamMap().get(paramCode);
            if (paramValue != null && paramValue.getEffectiveValue() != null
                    && !paramValue.getEffectiveValue().trim().isEmpty()) {
                effectiveValue = paramValue.getEffectiveValue().trim();
            }
        }
        try {
            return new BigDecimal(effectiveValue).max(BigDecimal.ZERO);
        } catch (NumberFormatException exception) {
            return new BigDecimal(defaultValue);
        }
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
