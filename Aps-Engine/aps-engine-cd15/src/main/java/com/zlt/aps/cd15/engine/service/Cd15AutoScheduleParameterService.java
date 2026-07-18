package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;

/**
 * 斜裁自动排程参数服务。
 */
public interface Cd15AutoScheduleParameterService {

    /**
     * 按工厂和PARAM_CODE加载并校验自动排程参数。
     *
     * @param factoryCode 工厂编码
     * @param enabledShiftCount 当前启用的斜裁班次数
     * @return 强类型参数快照
     */
    Cd15AutoScheduleParameters load(String factoryCode, int enabledShiftCount);
}
