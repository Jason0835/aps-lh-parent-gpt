package com.zlt.aps.lh.util;

/**
 * 首检时间轴模式。
 *
 * <p>用显式模式替代正向/倒推裸布尔值，统一普通切换、试制和量试的首检落点语义。</p>
 */
public enum FirstInspectionTimingMode {

    /**
     * 普通换模、换活字块：首检包含在切换总时长尾段。
     */
    INCLUDED_IN_CHANGEOVER,

    /**
     * 试制、量试或明确的生产门禁场景：首检从生产就绪时间开始。
     */
    START_AT_PRODUCTION_READY
}
