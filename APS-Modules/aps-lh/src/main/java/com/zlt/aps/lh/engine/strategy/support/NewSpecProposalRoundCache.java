package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 新增排产同一运行态版本内的提案只读缓存。
 *
 * <p>缓存只保存 SKU 级收尾判断、结构×班次有效机台统计、日志指纹和扫描统计，不保存
 * Machine×SKU反向匹配或真实时间轴计划，只读准入失败最多保存512条轻量原因。
 * 正式结果、终局未排、候选池或业务日阶段变化时
 * 清空快照；失败组合由主循环保存轻量AssignmentKey，避免构造完整匹配矩阵。</p>
 *
 * <p>缓存对象只在单次业务日阶段内存活，不进入排程上下文、不跨批次共享。</p>
 *
 * @author APS
 * @date 2026/10/17
 */
public final class NewSpecProposalRoundCache {

    /**
     * 同一资源版本最多保存的只读准入失败数，满额时仅清除此类缓存，避免形成匹配矩阵。
     */
    private static final int MAX_READ_ONLY_FAILURE_COUNT = 512;

    /**
     * 按候选对象身份及机台、日期池、班次隔离的轻量只读准入失败原因。
     */
    private final Map<DailyNewSpecCandidate, Map<String, String>> readOnlyFailureMap = new IdentityHashMap<>();

    /**
     * 当前资源版本已保存的只读失败条数。
     */
    private int readOnlyFailureCount;

    /**
     * 当前业务日阶段通过失败原因缓存避免的重复准入次数。
     */
    private int readOnlyFailureCacheHitCount;

    /** 当前运行态版本下SKU窗口收尾判断 */
    private final Map<DailyNewSpecCandidate, Boolean> endingFlagMap;
    /** 当前运行态版本下日志展示使用的预计收尾判断 */
    private final Map<SkuScheduleDTO, Boolean> expectedEndingTraceMap;
    /** 当前运行态版本下结构×正式班次的有效物理机台统计 */
    private final Map<String, StructureEarlyProductionAdmission>
            structureMachineStatisticsMap;
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
    /** 本轮缓存对应的设备约束版本，回滚或重新安排后必须重新比较候选。 */
    private long equipmentPlanConstraintVersion = -1L;

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
        this.structureMachineStatisticsMap =
                new LinkedHashMap<String, StructureEarlyProductionAdmission>(16);
    }

    /**
     * 正式运行态发生变化后清空全部只读快照。
     */
    public void clearAfterStateChanged() {
        endingFlagMap.clear();
        expectedEndingTraceMap.clear();
        structureMachineStatisticsMap.clear();
        readOnlyFailureMap.clear();
        readOnlyFailureCount = 0;
        lastQueueTraceFingerprint = null;
    }

    /**
     * 读取同一候选、机台、日期池和竞争班次已经确定的只读拒绝原因。
     *
     * @param candidate 当前候选对象
     * @param machineCode 当前被扫描的运行态机台
     * @param poolDate 当前日期池
     * @param shiftIndex 当前竞争班次
     * @return 已冻结拒绝原因；尚未计算或资源版本已失效时为空
     */
    public String getReadOnlyFailure(DailyNewSpecCandidate candidate, String machineCode,
            LocalDate poolDate, Integer shiftIndex) {
        Map<String, String> failures = readOnlyFailureMap.get(candidate);
        return failures == null ? null : failures.get(this.buildReadOnlyFailureKey(machineCode, poolDate, shiftIndex));
    }

    /**
     * 保存无副作用准入失败，不保存完整匹配或时间轴；容量满后重新计算，不能无界增长。
     *
     * @param candidate 当前候选对象
     * @param machineCode 当前被扫描的运行态机台
     * @param poolDate 当前日期池
     * @param shiftIndex 当前竞争班次
     * @param reason 非空拒绝原因
     */
    public void putReadOnlyFailure(DailyNewSpecCandidate candidate, String machineCode,
            LocalDate poolDate, Integer shiftIndex, String reason) {
        if (StringUtils.isEmpty(reason)) {
            return;
        }
        if (readOnlyFailureCount >= MAX_READ_ONLY_FAILURE_COUNT) {
            readOnlyFailureMap.clear();
            readOnlyFailureCount = 0;
        }
        Map<String, String> failures = readOnlyFailureMap.computeIfAbsent(candidate, key -> new LinkedHashMap<>());
        if (failures.put(this.buildReadOnlyFailureKey(machineCode, poolDate, shiftIndex), reason) == null) {
            readOnlyFailureCount++;
        }
    }

    /**
     * 构建单阶段内机台、日期池和竞争班次的失败键，候选身份由外层IdentityHashMap隔离。
     *
     * @param machineCode 运行态机台
     * @param poolDate 候选日期池
     * @param shiftIndex 竞争班次
     * @return 当前候选内的轻量失败键
     */
    private String buildReadOnlyFailureKey(String machineCode, LocalDate poolDate, Integer shiftIndex) {
        return machineCode + "|" + poolDate + "|" + shiftIndex;
    }

    /**
     * 记录一次无需重跑硬匹配和只读准入的失败缓存命中。
     */
    public void recordReadOnlyFailureCacheHit() {
        readOnlyFailureCacheHitCount++;
    }

    /**
     * 获取当前阶段被缓存阻止的重复准入次数。
     *
     * @return 当前阶段累计缓存命中数
     */
    public int getReadOnlyFailureCacheHitCount() {
        return readOnlyFailureCacheHitCount;
    }

    /** @param version 当前设备约束版本 @return 是否已失效旧提案缓存 */
    public boolean invalidateEquipmentPlanVersion(long version) {
        if (equipmentPlanConstraintVersion == version) {
            return false;
        }
        equipmentPlanConstraintVersion = version;
        this.clearAfterStateChanged();
        return true;
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
     * 获取当前运行态版本的结构班次有效机台统计。
     *
     * @param statisticsKey 结构与正式班次复合键
     * @return 已冻结统计；尚未计算时返回null
     */
    public StructureEarlyProductionAdmission getStructureMachineStatistics(
            String statisticsKey) {
        return structureMachineStatisticsMap.get(statisticsKey);
    }

    /**
     * 保存当前运行态版本的结构班次有效机台统计。
     *
     * @param statisticsKey 结构与正式班次复合键
     * @param statistics 只读统计快照
     */
    public void putStructureMachineStatistics(
            String statisticsKey,
            StructureEarlyProductionAdmission statistics) {
        if (StringUtils.isNotEmpty(statisticsKey) && statistics != null) {
            structureMachineStatisticsMap.put(statisticsKey, statistics);
        }
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
