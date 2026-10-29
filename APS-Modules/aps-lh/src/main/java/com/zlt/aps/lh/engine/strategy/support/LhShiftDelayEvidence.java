package com.zlt.aps.lh.engine.strategy.support;

import lombok.Getter;

/**
 * 一次真实约束拒绝的不可变附件，时间使用毫秒标量防止可变日期串改。
 * 班次范围只由原判断实际拒绝的区间形成，不能从最终首产时间倒推。
 *
 * @author APS
 * @date 2026/10/23
 */
@Getter
public final class LhShiftDelayEvidence {

    /**
     * 当前局部尝试身份，结果绑定后同时形成调度链关联。
     */
    private final long attemptId;
    /**
     * 稳定业务原因。
     */
    private final LhShiftDelayReason reason;
    /**
     * 原约束入口。
     */
    private final String source;
    /**
     * 真实作用机台或侧位。
     */
    private final String machineCode;
    /**
     * 当前后物料。
     */
    private final String materialCode;
    /**
     * 当前产品状态，空值沿用正式状态。
     */
    private final String productStatus;
    /**
     * 原请求或被拒绝班次内的起始时刻。
     */
    private final long blockedFrom;
    /**
     * 有依据的拒绝区间结束时刻，不包含此时刻。
     */
    private final long blockedUntil;
    /**
     * 原分支已经算出的额度、数量或资源子因快照，不参与决策。
     */
    private final String facts;

    /**
     * 冻结局部拒绝附件。
     *
     * @param attemptId 局部尝试序号
     * @param reason 稳定原因
     * @param source 原约束入口
     * @param machineCode 实际作用机台
     * @param materialCode 后物料
     * @param productStatus 产品状态
     * @param blockedFrom 拒绝开始毫秒
     * @param blockedUntil 拒绝结束毫秒
     */
    public LhShiftDelayEvidence(long attemptId, LhShiftDelayReason reason, String source,
            String machineCode, String materialCode, String productStatus, long blockedFrom, long blockedUntil) {
        this(attemptId, reason, source, machineCode, materialCode, productStatus, blockedFrom, blockedUntil, null);
    }

    /**
     * 冻结拒绝身份、区间及原数量事实。
     *
     * @param attemptId 局部尝试身份
     * @param reason 稳定原因
     * @param source 原判断入口
     * @param machineCode 实际作用机台
     * @param materialCode 后物料
     * @param productStatus 产品状态
     * @param blockedFrom 区间开始毫秒
     * @param blockedUntil 区间结束毫秒
     * @param facts 原分支数量或资源快照
     */
    public LhShiftDelayEvidence(long attemptId, LhShiftDelayReason reason, String source,
            String machineCode, String materialCode, String productStatus, long blockedFrom, long blockedUntil, String facts) {
        this.attemptId = attemptId;
        this.reason = reason;
        this.source = source;
        this.machineCode = machineCode;
        this.materialCode = materialCode;
        this.productStatus = productStatus;
        this.blockedFrom = blockedFrom;
        this.blockedUntil = blockedUntil;
        this.facts = facts;
    }
}
