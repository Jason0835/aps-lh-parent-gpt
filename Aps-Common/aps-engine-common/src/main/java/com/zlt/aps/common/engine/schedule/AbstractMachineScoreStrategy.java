package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公共默认机台评分算法模板。
 *
 * <p>模板统一处理过滤候选短路、剩余产能、主胶料连续、基部胶相似、口型连续、切换成本和定点生产评分，
 * TM/TC 通过适配方法提供任务、候选机台和领域权重解析。</p>
 *
 * @param <C> 机台评分上下文类型
 * @param <T> 待排任务类型
 * @param <M> 候选机台类型
 */
public abstract class AbstractMachineScoreStrategy<C, T, M> {

    /**
     * 获取评分策略编码。
     *
     * @return 评分策略编码
     */
    public abstract String getStrategyCode();

    /**
     * 执行公共机台评分流程。
     *
     * @param candidate 候选机台
     * @param context   机台评分上下文
     * @return 评分结果
     * @throws RuntimeException 候选机台或上下文校验失败时由领域适配器抛出业务异常
     */
    public final ScheduleScoreResult score(M candidate, C context) {
        this.validateCandidateAndContext(candidate, context);
        ScheduleScoreResult result = new ScheduleScoreResult();
        result.setStrategyCode(this.getStrategyCode());
        if (this.isFiltered(candidate)) {
            BigDecimal zero = BigDecimal.ZERO;
            this.setCandidateScore(candidate, zero);
            result.setTotalScore(zero);
            result.setDescription("机台已被过滤，不参与评分");
            this.applyScore(candidate, result);
            return result;
        }

        T task = this.getTask(context);
        BigDecimal remainCapacityWeight = this.resolveWeight(context,
                this.getRemainCapacityWeightParamCode(), this.getRemainCapacityWeightDefault());
        BigDecimal mainGlueWeight = this.resolveWeight(context,
                this.getMainGlueWeightParamCode(), this.getMainGlueWeightDefault());
        BigDecimal baseGlueWeight = this.resolveWeight(context,
                this.getBaseGlueWeightParamCode(), this.getBaseGlueWeightDefault());
        BigDecimal mouthPlateWeight = this.resolveWeight(context,
                this.getMouthPlateWeightParamCode(), this.getMouthPlateWeightDefault());
        BigDecimal switchCostWeight = this.resolveWeight(context,
                this.getSwitchCostWeightParamCode(), this.getSwitchCostWeightDefault());
        BigDecimal fixedMachineWeight = this.resolveWeight(context,
                this.getFixedMachineWeightParamCode(), this.getFixedMachineWeightDefault());

        BigDecimal capacityScore = this.calculateCapacityFitScore(task, candidate, remainCapacityWeight);
        BigDecimal mainGlueScore = this.isSameNonBlank(this.getTaskMainGlue(task),
                this.getCandidateTailMainGlue(candidate)) ? mainGlueWeight : BigDecimal.ZERO;
        BigDecimal baseGlueScore = mainGlueScore.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.ZERO
                : this.calculateGlueSimilarityScore(this.getTaskBaseGlue(task),
                this.getCandidateTailBaseGlue(candidate), baseGlueWeight);
        BigDecimal mouthPlateScore = this.isSameNonBlank(this.getTaskMouthPlate(task),
                this.getCandidateTailMouthPlate(candidate)) ? mouthPlateWeight : BigDecimal.ZERO;
        BigDecimal switchCostScore = switchCostWeight.subtract(
                this.nvl(this.getCandidateSwitchCostHours(candidate))).max(BigDecimal.ZERO);
        BigDecimal fixedScore = this.isFixedMachineMatched(candidate)
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
        result.setDescription(this.getScoreDescription());
        this.setCandidateScore(candidate, totalScore);
        this.getCandidateEvidence(candidate).putAll(scoreItems);
        this.applyScore(candidate, result);
        return result;
    }

    /**
     * 校验候选机台和评分上下文。
     *
     * @param candidate 候选机台
     * @param context   评分上下文
     */
    protected abstract void validateCandidateAndContext(M candidate, C context);

    /**
     * 获取上下文中的待排任务。
     *
     * @param context 评分上下文
     * @return 待排任务
     */
    protected abstract T getTask(C context);

    /**
     * 判断候选机台是否已被过滤。
     *
     * @param candidate 候选机台
     * @return 是否已过滤
     */
    protected abstract boolean isFiltered(M candidate);

    /**
     * 写入候选机台总分。
     *
     * @param candidate 候选机台
     * @param score 总分
     */
    protected abstract void setCandidateScore(M candidate, BigDecimal score);

    /**
     * 将公共评分结果写回候选机台。
     *
     * @param candidate 候选机台
     * @param result 评分结果
     */
    protected abstract void applyScore(M candidate, ScheduleScoreResult result);

    /**
     * 获取候选机台证据映射。
     *
     * @param candidate 候选机台
     * @return 证据映射
     */
    protected abstract Map<String, Object> getCandidateEvidence(M candidate);

    /**
     * 获取任务主胶料。
     *
     * @param task 待排任务
     * @return 主胶料编码
     */
    protected abstract String getTaskMainGlue(T task);

    /**
     * 获取候选链尾主胶料。
     *
     * @param candidate 候选机台
     * @return 链尾主胶料编码
     */
    protected abstract String getCandidateTailMainGlue(M candidate);

    /**
     * 获取任务基部胶。
     *
     * @param task 待排任务
     * @return 基部胶编码
     */
    protected abstract String getTaskBaseGlue(T task);

    /**
     * 获取候选链尾基部胶。
     *
     * @param candidate 候选机台
     * @return 链尾基部胶编码
     */
    protected abstract String getCandidateTailBaseGlue(M candidate);

    /**
     * 获取任务口型板。
     *
     * @param task 待排任务
     * @return 口型板编码
     */
    protected abstract String getTaskMouthPlate(T task);

    /**
     * 获取候选链尾口型板。
     *
     * @param candidate 候选机台
     * @return 链尾口型板编码
     */
    protected abstract String getCandidateTailMouthPlate(M candidate);

    /**
     * 获取候选切换成本。
     *
     * @param candidate 候选机台
     * @return 切换成本小时数
     */
    protected abstract BigDecimal getCandidateSwitchCostHours(M candidate);

    /**
     * 判断候选机台是否命中定点生产。
     *
     * @param candidate 候选机台
     * @return 是否命中定点生产
     */
    protected abstract boolean isFixedMachineMatched(M candidate);

    /**
     * 计算剩余产能适配分，TM/TC 可通过此钩子保留不同的超产能口径。
     *
     * @param task      待排任务
     * @param candidate 候选机台
     * @param weight    产能评分权重
     * @return 产能适配分
     */
    protected abstract BigDecimal calculateCapacityFitScore(T task, M candidate, BigDecimal weight);

    /**
     * 判断两个胶料编码是否为相同非空值。
     *
     * @param first 第一个编码
     * @param second 第二个编码
     * @return 是否相同且非空
     */
    protected abstract boolean isSameNonBlank(String first, String second);

    /**
     * 计算基部胶相似度得分。
     *
     * @param taskBaseGlue 任务基部胶
     * @param tailBaseGlue 链尾基部胶
     * @param weight       基部胶评分权重
     * @return 基部胶相似度得分
     */
    protected abstract BigDecimal calculateGlueSimilarityScore(String taskBaseGlue, String tailBaseGlue,
                                                                BigDecimal weight);

    /**
     * 解析领域评分权重。
     *
     * @param context      评分上下文
     * @param paramCode    参数编码
     * @param defaultValue 默认权重
     * @return 有效权重
     */
    protected abstract BigDecimal resolveWeight(C context, String paramCode, String defaultValue);

    /**
     * 获取剩余产能评分权重参数编码。
     *
     * @return 参数编码
     */
    protected abstract String getRemainCapacityWeightParamCode();

    /**
     * 获取剩余产能默认权重。
     *
     * @return 默认权重
     */
    protected abstract String getRemainCapacityWeightDefault();

    /**
     * 获取主胶料连续评分权重参数编码。
     *
     * @return 参数编码
     */
    protected abstract String getMainGlueWeightParamCode();

    /**
     * 获取主胶料连续默认权重。
     *
     * @return 默认权重
     */
    protected abstract String getMainGlueWeightDefault();

    /**
     * 获取基部胶评分权重参数编码。
     *
     * @return 参数编码
     */
    protected abstract String getBaseGlueWeightParamCode();

    /**
     * 获取基部胶默认权重。
     *
     * @return 默认权重
     */
    protected abstract String getBaseGlueWeightDefault();

    /**
     * 获取口型连续评分权重参数编码。
     *
     * @return 参数编码
     */
    protected abstract String getMouthPlateWeightParamCode();

    /**
     * 获取口型连续默认权重。
     *
     * @return 默认权重
     */
    protected abstract String getMouthPlateWeightDefault();

    /**
     * 获取切换成本评分权重参数编码。
     *
     * @return 参数编码
     */
    protected abstract String getSwitchCostWeightParamCode();

    /**
     * 获取切换成本默认权重。
     *
     * @return 默认权重
     */
    protected abstract String getSwitchCostWeightDefault();

    /**
     * 获取定点生产评分权重参数编码。
     *
     * @return 参数编码
     */
    protected abstract String getFixedMachineWeightParamCode();

    /**
     * 获取定点生产默认权重。
     *
     * @return 默认权重
     */
    protected abstract String getFixedMachineWeightDefault();

    /**
     * 获取评分说明。
     *
     * @return 评分说明
     */
    protected abstract String getScoreDescription();

    /**
     * 将空数值归一为零。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    protected BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
