package com.zlt.aps.common.engine.schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次候选机台过滤、评分和排序的公共结果。
 *
 * @param <M> 候选机台类型
 */
public class ScheduleCandidateEvaluationResult<M> {

    /** 本次评估使用的全部候选机台，包含被过滤候选。 */
    private final List<M> allCandidates;

    /** 通过过滤并完成评分、排序的候选机台。 */
    private final List<M> passedCandidates;

    /**
     * 创建候选评估结果。
     *
     * @param allCandidates 全部候选机台
     * @param passedCandidates 通过过滤的候选机台
     */
    public ScheduleCandidateEvaluationResult(List<M> allCandidates, List<M> passedCandidates) {
        this.allCandidates = this.copyList(allCandidates);
        this.passedCandidates = this.copyList(passedCandidates);
    }

    /**
     * 获取全部候选机台。
     *
     * @return 可变候选机台列表
     */
    public List<M> getAllCandidates() {
        return new ArrayList<>(this.allCandidates);
    }

    /**
     * 获取通过过滤的候选机台。
     *
     * @return 可变且已完成领域排序的候选机台列表
     */
    public List<M> getPassedCandidates() {
        return new ArrayList<>(this.passedCandidates);
    }

    /**
     * 复制结果列表。
     *
     * @param source 原始列表
     * @return 非空可变列表
     */
    private List<M> copyList(List<M> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
