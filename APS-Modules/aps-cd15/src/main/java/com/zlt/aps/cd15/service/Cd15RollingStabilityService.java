package com.zlt.aps.cd15.service;

import java.time.Instant;

/** CD15滚动输入版本稳定性观察服务。 */
public interface Cd15RollingStabilityService {

    /**
     * 观察同一滚动目标的输入版本是否已稳定达到指定分钟数。
     *
     * @param stateKey 滚动目标状态键
     * @param inputVersion 输入版本
     * @param observedAt 本次观察时间
     * @param stableMinutes 稳定分钟数
     * @return 是否稳定
     */
    boolean observe(String stateKey, String inputVersion, Instant observedAt, int stableMinutes);
}