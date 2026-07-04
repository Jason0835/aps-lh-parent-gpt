package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingParameters;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;

import java.time.LocalDateTime;
import java.util.Optional;

/** 交班滚动目标解析服务。 */
public interface Cd90RollingTargetResolver {

    /** 根据触发时间、班次配置和有效批次解析本次滚动目标。 */
    Optional<Cd90RollingTarget> resolve(String factoryCode, LocalDateTime triggerTime,
                                        Cd90RollingParameters parameters);
}
