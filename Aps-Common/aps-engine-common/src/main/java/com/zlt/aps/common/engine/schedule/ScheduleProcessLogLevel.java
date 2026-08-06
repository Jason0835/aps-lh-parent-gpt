package com.zlt.aps.common.engine.schedule;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

/**
 * 自动排程过程日志级别。
 */
public enum ScheduleProcessLogLevel {

    /** 不收集、不保存过程日志。 */
    OFF,

    /** 保留现有步骤边界和关键公式摘要。 */
    SUMMARY,

    /** 记录可供测试人员独立还原计算过程的完整中文事件。 */
    FULL;

    /** 默认过程日志级别。 */
    public static final ScheduleProcessLogLevel DEFAULT_LEVEL = SUMMARY;

    /**
     * 解析配置值，空值和非法值均回退到摘要级别。
     *
     * @param configuredValue 配置值
     * @return 有效日志级别
     */
    public static ScheduleProcessLogLevel parse(String configuredValue) {
        if (StrUtil.isBlank(configuredValue)) {
            return DEFAULT_LEVEL;
        }
        try {
            return ScheduleProcessLogLevel.valueOf(configuredValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DEFAULT_LEVEL;
        }
    }

    /**
     * 判断配置值是否为支持的日志级别。
     *
     * @param configuredValue 配置值
     * @return 支持返回 true，否则返回 false
     */
    public static boolean isSupported(String configuredValue) {
        if (StrUtil.isBlank(configuredValue)) {
            return false;
        }
        try {
            ScheduleProcessLogLevel.valueOf(configuredValue.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
