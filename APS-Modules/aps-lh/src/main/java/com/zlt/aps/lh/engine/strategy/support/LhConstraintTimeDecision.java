package com.zlt.aps.lh.engine.strategy.support;

import java.util.Date;
import java.util.List;

/**
 * 兼容原时间接口的约束返回附件，原落点与只读证据独立消费。
 *
 * @author APS
 * @date 2026/10/23
 */
public final class LhConstraintTimeDecision {

    /**
     * 原内核落点，空值保持原拒绝语义。
     */
    private final Date startTime;
    /**
     * 当前局部实际命中的拒绝附件。
     */
    private final List<LhShiftDelayEvidence> delayEvidence;

    /**
     * 冻结时间决策及附件。
     *
     * @param startTime 原内核落点
     * @param delayEvidence 局部不可变附件
     */
    public LhConstraintTimeDecision(Date startTime, List<LhShiftDelayEvidence> delayEvidence) {
        this.startTime = startTime;
        this.delayEvidence = delayEvidence;
    }

    /**
     * 读取原时间返回值。
     *
     * @return 原内核落点或空值
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * 读取局部证据。
     *
     * @return 不可变拒绝附件
     */
    public List<LhShiftDelayEvidence> getDelayEvidence() {
        return delayEvidence;
    }
}
