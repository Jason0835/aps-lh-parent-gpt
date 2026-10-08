package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** 排程接受的设备计划安排，不表示车间实际完成。状态变化返回新对象，允许快照只读共享。 */
public final class EquipmentPlanArrangement {

    /** 安排生命周期，候选时间计划本身不进入本记录。 */
    public enum Status { ACCEPTED, REVOKED, FINALIZED }

    /** 已接受的只读时间约束。 */
    private final EquipmentPlanTimeline timeline;
    /** 当前安排状态。 */
    private final Status status;
    /** 撤销或收口原因。 */
    private final String reason;
    /** 接受时已有生产结果的身份集合；维护后新生成的结果不受原生产截止约束。 */
    private final Set<LhScheduleResult> sourceResults;

    /** @param timeline 时间计划 @param status 安排状态 @param reason 原因 */
    public EquipmentPlanArrangement(EquipmentPlanTimeline timeline, Status status, String reason) {
        this(timeline, status, reason, Collections.emptySet());
    }

    /** @param timeline 时间计划 @param status 状态 @param reason 原因 @param sourceResults 原生产结果身份 */
    public EquipmentPlanArrangement(EquipmentPlanTimeline timeline, Status status, String reason,
                                    Collection<LhScheduleResult> sourceResults) {
        this.timeline = timeline;
        this.status = status;
        this.reason = reason;
        Set<LhScheduleResult> identities = Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>());
        identities.addAll(sourceResults);
        this.sourceResults = Collections.unmodifiableSet(identities);
    }

    /** @param status 新状态 @param reason 状态变化原因 @return 新安排记录 */
    public EquipmentPlanArrangement withStatus(Status status, String reason) {
        return new EquipmentPlanArrangement(timeline, status, reason, sourceResults);
    }

    /** @param timeline 已正式接受的组合时间轴 @param reason 变化原因 @return 保留原生产约束的安排副本 */
    public EquipmentPlanArrangement withTimeline(EquipmentPlanTimeline timeline, String reason) {
        return new EquipmentPlanArrangement(timeline, status, reason, sourceResults);
    }

    /** @param result 当前结果 @return 是否为接受安排时已存在的原生产结果 */
    public boolean constrainsSourceResult(LhScheduleResult result) {
        return sourceResults.contains(result);
    }

    /** @return 只读时间计划 */
    public EquipmentPlanTimeline getTimeline() {
        return timeline;
    }
    /** @return 当前安排状态 */
    public Status getStatus() {
        return status;
    }
    /** @return 当前状态原因 */
    public String getReason() {
        return reason;
    }
    /** @return 安排是否仍然有效 */
    public boolean isEffective() {
        return status != Status.REVOKED;
    }
}
