package com.zlt.aps.cd90.engine.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 直裁自动排程任务状态及合法转换规则。
 */
public final class Cd90ScheduleTaskStatus {

    /** 等待执行 */
    public static final String PENDING = "PENDING";
    /** 执行中 */
    public static final String RUNNING = "RUNNING";
    /** 执行成功 */
    public static final String SUCCESS = "SUCCESS";
    /** 执行失败 */
    public static final String FAILED = "FAILED";

    private static final Map<String, Set<String>> TRANSITIONS;

    static {
        Map<String, Set<String>> transitions = new HashMap<>();
        transitions.put(PENDING, new HashSet<>(Arrays.asList(RUNNING, FAILED)));
        transitions.put(RUNNING, new HashSet<>(Arrays.asList(SUCCESS, FAILED)));
        transitions.put(SUCCESS, Collections.emptySet());
        transitions.put(FAILED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private Cd90ScheduleTaskStatus() {
    }

    /**
     * 判断任务状态转换是否合法。
     *
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @return 是否允许转换
     */
    public static boolean canTransition(String sourceStatus, String targetStatus) {
        return TRANSITIONS.getOrDefault(sourceStatus, Collections.emptySet()).contains(targetStatus);
    }
}
