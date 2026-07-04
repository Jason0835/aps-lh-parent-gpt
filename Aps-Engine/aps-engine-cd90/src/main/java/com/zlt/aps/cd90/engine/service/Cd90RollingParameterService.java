package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingParameters;

/** 定时滚动参数服务。 */
public interface Cd90RollingParameterService {

    /** 按工厂加载滚动窗口参数，缺失时使用业务默认值。 */
    Cd90RollingParameters load(String factoryCode);
}
