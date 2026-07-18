package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingParameters;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

import java.time.LocalDateTime;
import java.util.Optional;

/** 交班滚动目标解析服务。 */
public interface Cd15RollingTargetResolver {

    /** 根据触发时间、班次配置和有效批次解析本次滚动目标。 */
    Optional<Cd15RollingTarget> resolve(String factoryCode, LocalDateTime triggerTime,
                                        Cd15RollingParameters parameters);
}
