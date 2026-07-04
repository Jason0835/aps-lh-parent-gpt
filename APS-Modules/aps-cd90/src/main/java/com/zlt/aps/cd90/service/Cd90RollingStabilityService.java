package com.zlt.aps.cd90.service;

import java.time.Instant;

/** 定时滚动输入版本稳定期服务。 */
public interface Cd90RollingStabilityService {

    /** 观察输入版本，达到连续稳定分钟数时返回true。 */
    boolean observe(String stateKey, String inputVersion, Instant observedAt,
                    int stableMinutes);
}
