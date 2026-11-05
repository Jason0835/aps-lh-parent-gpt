package com.zlt.aps.common.engine.schedule.engine;

/**
 * 两阶段内部模式访问契约；兼容实现默认关闭。
 */
public interface TwoPhaseScheduleContext {

    /**
     * 获取内部流水线模式。
     *
     * @return 仅真实自动排程入口显式启用
     */
    default boolean isTwoPhaseEnabled() {
        return false;
    }

    /**
     * 获取既有批次身份，不另造运行标识。
     *
     * @return 自动排程批次，兼容入口可为空
     */
    default String getBatchNo() {
        return null;
    }
}
