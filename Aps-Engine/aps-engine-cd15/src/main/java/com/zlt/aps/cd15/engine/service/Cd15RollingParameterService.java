package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingParameters;

/** 定时滚动参数服务。 */
public interface Cd15RollingParameterService {

    /** 按工厂加载滚动窗口参数，缺失时使用业务默认值。 */
    Cd15RollingParameters load(String factoryCode);
}
