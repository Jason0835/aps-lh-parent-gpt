package com.zlt.aps.common.engine.schedule.engine;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * 备库准入主因；显式优先级与枚举声明顺序无关。
 */
public enum ScheduleExtraPolicy {
    ALLOW(Integer.MAX_VALUE), FORBID_BLOCKED(10), FORBID_RETAINED_FACT(20),
    FORBID_BASE_CARRY(30), FORBID_POSTLOSS_PRESET(40), FORBID_SHUTDOWN(50),
    FORBID_TAIL(60), FORBID_RAW_TAIL(70), FORBID_PRESET(80),
    FORBID_COMPAT_ADVANCE(90), FORBID_FUTURE_SUPPLEMENT(100);

    private final int rank;

    ScheduleExtraPolicy(int rank) {
        this.rank = rank;
    }

    /**
     * 从完整命中集合解析唯一展示主因，调用者仍保留所有命中原因。
     *
     * @param policies 全部命中策略
     * @return 优先级最高的禁入原因，无禁入时允许
     */
    public static ScheduleExtraPolicy primary(Collection<ScheduleExtraPolicy> policies) {
        return policies == null ? ALLOW : policies.stream().filter(Objects::nonNull)
                .min(Comparator.comparingInt(policy -> policy.rank)).orElse(ALLOW);
    }
}
