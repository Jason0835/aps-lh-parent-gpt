package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/**
 * 自动排程质量摘要领域差异策略。
 *
 * @param <C> 上下文类型
 * @param <P> 落库摘要类型
 * @param <M> 候选机台类型
 */
public interface ScheduleQualityPolicy<C, P extends ScheduleQualityPersistSummary,
        M extends ScheduleQualityMachineCandidate> {

    void validate(C context, P persistSummary);

    int getMaxShiftOrder();

    boolean isAssignedTaskRequiredForUtilization();

    boolean isMouthPlateSwitchCounted();

    BigDecimal resolveMachineMaxCapacity(M candidate);
}

