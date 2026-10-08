package com.zlt.aps.lh.engine.strategy.support;

/**
 * 设备计划重叠解析结果。未相交、已支持及未接入必须显式区分，
 * 后续场景接入时不能把空扩展当成无冲突；候选结果不登记任何运行态资源。
 */
public final class EquipmentPlanOverlapResult {

    /** 重叠处理状态。 */
    public enum Status { NO_OVERLAP, SUPPORTED, UNSUPPORTED }

    /** 当前组合状态。 */
    private final Status status;
    /** 已支持组合的只读时间轴，其他状态为空。 */
    private final EquipmentPlanTimeline timeline;
    /** 未接入组合的明确原因。 */
    private final String reason;

    private EquipmentPlanOverlapResult(Status status, EquipmentPlanTimeline timeline, String reason) {
        this.status = status;
        this.timeline = timeline;
        this.reason = reason;
    }

    /** @return 无重叠，调用方沿用原单场景逻辑 */
    public static EquipmentPlanOverlapResult noOverlap() {
        return new EquipmentPlanOverlapResult(Status.NO_OVERLAP, null, null);
    }

    /** @param timeline 并行后公共时间轴 @return 已支持组合 */
    public static EquipmentPlanOverlapResult supported(EquipmentPlanTimeline timeline) {
        return new EquipmentPlanOverlapResult(Status.SUPPORTED, timeline, null);
    }

    /** @param reason 尚未接入的原因 @return 不可提交组合 */
    public static EquipmentPlanOverlapResult unsupported(String reason) {
        return new EquipmentPlanOverlapResult(Status.UNSUPPORTED, null, reason);
    }

    /** @return 是否为已支持的并行组合 */
    public boolean isSupported() {
        return status == Status.SUPPORTED;
    }

    /** @return 是否需要明确拒绝 */
    public boolean isUnsupported() {
        return status == Status.UNSUPPORTED;
    }

    /** @return 只读候选时间轴 */
    public EquipmentPlanTimeline getTimeline() {
        return timeline;
    }

    /** @return 拒绝原因 */
    public String getReason() {
        return reason;
    }
}
