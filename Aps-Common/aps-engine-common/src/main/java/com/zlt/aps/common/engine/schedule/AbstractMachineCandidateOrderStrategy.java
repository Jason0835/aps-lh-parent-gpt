package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 公共候选机台排序算法模板。
 *
 * <p>模板统一处理候选列表复制、默认评分降序、机台编码升序以及首选机台置顶。
 * 领域实现可以通过 {@code sortForTaskInternal} 保留“优先排满一台”等专有排序口径，
 * 但不能改变首选机台置顶和空列表处理方式。</p>
 *
 * @param <T> 待排任务类型
 * @param <M> 候选机台类型
 */
public abstract class AbstractMachineCandidateOrderStrategy<T, M> {

    /**
     * 按公共默认口径排序候选机台。
     *
     * @param candidates 候选机台列表
     * @return 新的可变排序列表；输入列表不会被修改
     */
    public final List<M> sort(List<M> candidates) {
        List<M> sortedCandidates = this.copyCandidates(candidates);
        sortedCandidates.sort(this.buildDefaultComparator());
        return sortedCandidates;
    }

    /**
     * 按任务和领域扩展口径排序候选机台。
     *
     * @param task 当前任务
     * @param candidates 候选机台列表
     * @return 新的可变排序列表；输入列表不会被修改
     */
    public final List<M> sortForTask(T task, List<M> candidates) {
        List<M> sortedCandidates = this.copyCandidates(candidates);
        this.sortForTaskInternal(task, sortedCandidates);
        return sortedCandidates;
    }

    /**
     * 在任务排序结果基础上将已选机台置于首位。
     *
     * @param task 当前任务
     * @param candidates 候选机台列表
     * @param selectedCandidate 已选机台
     * @return 新的可变排序列表；输入列表不会被修改
     */
    public final List<M> sortSelectedFirst(T task, List<M> candidates, M selectedCandidate) {
        List<M> sortedCandidates = this.sortForTask(task, candidates);
        if (selectedCandidate == null || this.isBlank(this.getMachineCode(selectedCandidate))) {
            return sortedCandidates;
        }
        sortedCandidates.sort((left, right) -> {
            boolean leftSelected = this.isSameMachine(selectedCandidate, left);
            boolean rightSelected = this.isSameMachine(selectedCandidate, right);
            if (leftSelected == rightSelected) {
                return 0;
            }
            return leftSelected ? -1 : 1;
        });
        return sortedCandidates;
    }

    /**
     * 执行领域任务排序扩展；默认复用公共评分排序。
     *
     * @param task 当前任务
     * @param candidates 可变候选机台列表
     */
    protected void sortForTaskInternal(T task, List<M> candidates) {
        candidates.sort(this.buildDefaultComparator());
    }

    /**
     * 构建公共默认排序比较器。
     *
     * @return 评分降序、机台编码升序比较器
     */
    protected Comparator<M> buildDefaultComparator() {
        return Comparator.comparing((M candidate) -> this.getScore(candidate),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(candidate -> this.getMachineCode(candidate),
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 获取候选机台评分。
     *
     * @param candidate 候选机台
     * @return 评分
     */
    protected abstract BigDecimal getScore(M candidate);

    /**
     * 获取候选机台编码。
     *
     * @param candidate 候选机台
     * @return 机台编码
     */
    protected abstract String getMachineCode(M candidate);

    /**
     * 判断两个候选机台是否为同一机台。
     *
     * @param selectedCandidate 已选机台
     * @param candidate 待比较机台
     * @return 编码一致返回 true
     */
    protected boolean isSameMachine(M selectedCandidate, M candidate) {
        return selectedCandidate != null && candidate != null
                && java.util.Objects.equals(this.getMachineCode(selectedCandidate), this.getMachineCode(candidate));
    }

    /**
     * 复制候选列表，避免排序改变领域上下文中的原始候选顺序。
     *
     * @param candidates 原始候选列表
     * @return 可变候选列表
     */
    protected List<M> copyCandidates(List<M> candidates) {
        return new ArrayList<>(candidates == null ? Collections.emptyList() : candidates);
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待判断字符串
     * @return 为空或全空白返回 true
     */
    protected boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
