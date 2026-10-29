package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 单次硫化排程的未排运行态。
 *
 * <p>全部集合均归属于一个 {@code LhScheduleContext}，禁止跨批次共享。</p>
 *
 * @author APS
 */
@Data
public class UnscheduledResultRuntime {

    /** SKU对象身份到原始需求标识。 */
    private Map<SkuScheduleDTO, String> demandKeyBySku =
            new IdentityHashMap<SkuScheduleDTO, String>();
    /** 原始需求标识到需求快照。 */
    private Map<String, UnscheduledDemandSnapshot> demandSnapshotMap =
            new LinkedHashMap<String, UnscheduledDemandSnapshot>(64);
    /** 原始需求标识到真实分支原因事件。 */
    private Map<String, List<UnscheduledReasonEvent>> reasonEventMap =
            new LinkedHashMap<String, List<UnscheduledReasonEvent>>(64);
    /**
     * SKU 与原因到真实命中快照；列表按登记顺序保存来源，撤销时只移除对应来源。
     */
    private Map<String, List<EndingDiscardCandidate>> endingDiscardCandidateMap =
            new LinkedHashMap<String, List<EndingDiscardCandidate>>(16);
    /**
     * 保存前最终数量收口已经开始，此后诊断更新不得登记或覆盖规则命中量。
     */
    private boolean endingDiscardRegistrationClosed;
    /** 原因事件递增序号。 */
    private long eventSequence;

    /**
     * 深复制运行态，供单候选原子试算失败后恢复。
     *
     * @return 独立运行态副本
     */
    public UnscheduledResultRuntime copy() {
        UnscheduledResultRuntime target = new UnscheduledResultRuntime();
        target.setDemandKeyBySku(new IdentityHashMap<SkuScheduleDTO, String>(demandKeyBySku));
        Map<String, UnscheduledDemandSnapshot> snapshotMap =
                new LinkedHashMap<String, UnscheduledDemandSnapshot>(demandSnapshotMap.size());
        demandSnapshotMap.forEach((key, value) -> snapshotMap.put(
                key, Objects.isNull(value) ? null : value.copy()));
        target.setDemandSnapshotMap(snapshotMap);
        Map<String, List<UnscheduledReasonEvent>> eventMap =
                new LinkedHashMap<String, List<UnscheduledReasonEvent>>(reasonEventMap.size());
        reasonEventMap.forEach((key, value) -> {
            List<UnscheduledReasonEvent> copiedEvents = new ArrayList<UnscheduledReasonEvent>(value.size());
            value.forEach(event -> copiedEvents.add(event.copy()));
            eventMap.put(key, copiedEvents);
        });
        target.setReasonEventMap(eventMap);
        Map<String, List<EndingDiscardCandidate>> discardMap =
                new LinkedHashMap<String, List<EndingDiscardCandidate>>(endingDiscardCandidateMap.size());
        // 候选值不可变；复制列表即可隔离撤销操作，来源身份与既有未排回滚保留相同对象。
        endingDiscardCandidateMap.forEach((key, value) -> discardMap.put(
                key, new ArrayList<EndingDiscardCandidate>(value)));
        target.setEndingDiscardCandidateMap(discardMap);
        target.setEndingDiscardRegistrationClosed(endingDiscardRegistrationClosed);
        target.setEventSequence(eventSequence);
        return target;
    }
}
