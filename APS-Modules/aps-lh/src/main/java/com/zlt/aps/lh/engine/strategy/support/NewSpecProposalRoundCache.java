package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 新增排产同一运行态版本内的提案只读缓存。
 *
 * <p>缓存只保存 SKU 级收尾判断、日志指纹和扫描统计，不保存 Machine×SKU 反向匹配、
 * 结构准入或真实时间轴计划。正式结果、终局未排、候选池或业务日阶段变化时清空 SKU 级
 * 快照；失败组合由主循环保存轻量 AssignmentKey，避免构造完整匹配矩阵。</p>
 *
 * <p>缓存对象只在单次业务日阶段内存活，不进入排程上下文、不跨批次共享。</p>
 *
 * @author APS
 */
public final class NewSpecProposalRoundCache {

    /** 当前运行态版本下SKU窗口收尾判断 */
    private final Map<DailyNewSpecCandidate, Boolean> endingFlagMap;
    /** 当前运行态版本下日志展示使用的预计收尾判断 */
    private final Map<SkuScheduleDTO, Boolean> expectedEndingTraceMap;
    /** 上一次已经输出的待排队列指纹 */
    private String lastQueueTraceFingerprint;
    /** 当前业务日阶段累计被只读准入拒绝的Machine×SKU数量 */
    private int eligibilityRejectedCount;
    /** 当前业务日阶段累计执行反向硬匹配的Machine×SKU数量 */
    private int evaluatedPairCount;
    /** 当前业务日阶段累计未形成合法真实时间轴的Machine×SKU数量 */
    private int timelineRejectedCount;
    /** 当前业务日阶段单轮最多保留的机台最佳提案数量 */
    private int maxRetainedBestProposalCount;

    /**
     * 创建业务日阶段缓存。
     *
     * @param candidateCount 候选SKU数量
     */
    public NewSpecProposalRoundCache(int candidateCount) {
        int normalizedCandidateCount = Math.max(1, candidateCount);
        this.endingFlagMap =
                new IdentityHashMap<DailyNewSpecCandidate, Boolean>(normalizedCandidateCount * 2);
        this.expectedEndingTraceMap =
                new IdentityHashMap<SkuScheduleDTO, Boolean>(normalizedCandidateCount * 2);
    }

    /**
     * 正式运行态发生变化后清空全部只读快照。
     */
    public void clearAfterStateChanged() {
        endingFlagMap.clear();
        expectedEndingTraceMap.clear();
        lastQueueTraceFingerprint = null;
    }

    /**
     * 记录一次 Machine×SKU 反向硬匹配。
     */
    public void recordEvaluatedPair() {
        evaluatedPairCount++;
    }

    /**
     * 判断待排队列是否发生变化，并登记最新指纹。
     *
     * @param fingerprint 当前待排队列指纹
     * @return true-需要输出新快照；false-与上次完全一致
     */
    public boolean shouldTraceQueue(String fingerprint) {
        if (StringUtils.isEmpty(fingerprint)
                || StringUtils.equals(fingerprint, lastQueueTraceFingerprint)) {
            return false;
        }
        lastQueueTraceFingerprint = fingerprint;
        return true;
    }

    /**
     * 记录一次 Machine×SKU 只读准入拒绝。
     */
    public void recordEligibilityRejected() {
        eligibilityRejectedCount++;
    }

    /**
     * 记录一次 Machine×SKU 真实时间轴拒绝。
     */
    public void recordTimelineRejected() {
        timelineRejectedCount++;
    }

    /**
     * 记录单轮实际保留的机台最佳提案数量。
     *
     * @param proposalCount 当前轮机台最佳提案数量
     */
    public void recordRetainedBestProposalCount(int proposalCount) {
        maxRetainedBestProposalCount = Math.max(
                maxRetainedBestProposalCount, Math.max(0, proposalCount));
    }

    /** @return 当前运行态版本下 SKU 窗口收尾判断缓存 */
    public Map<DailyNewSpecCandidate, Boolean> getEndingFlagMap() {
        return endingFlagMap;
    }

    /** @return 当前运行态版本下日志预计收尾判断缓存 */
    public Map<SkuScheduleDTO, Boolean> getExpectedEndingTraceMap() {
        return expectedEndingTraceMap;
    }

    /**
     * 获取当前运行态版本被只读准入拒绝的Machine×SKU数量。
     *
     * @return 拒绝组合数量
     */
    public int getEligibilityRejectedCount() {
        return eligibilityRejectedCount;
    }

    /** @return 当前阶段累计执行反向硬匹配的组合数 */
    public int getEvaluatedPairCount() {
        return evaluatedPairCount;
    }

    /** @return 当前阶段累计真实时间轴拒绝组合数 */
    public int getTimelineRejectedCount() {
        return timelineRejectedCount;
    }

    /** @return 单轮最多保留的机台最佳提案数 */
    public int getMaxRetainedBestProposalCount() {
        return maxRetainedBestProposalCount;
    }
}
