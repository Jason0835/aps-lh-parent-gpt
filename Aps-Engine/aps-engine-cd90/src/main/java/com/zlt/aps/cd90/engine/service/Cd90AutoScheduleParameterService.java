package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;

/**
 * 直裁自动排程参数服务。
 */
public interface Cd90AutoScheduleParameterService {

    /**
     * 按工厂和PARAM_CODE加载并校验自动排程参数。
     *
     * @param factoryCode 工厂编码
     * @param enabledShiftCount 当前启用的直裁班次数
     * @return 强类型参数快照
     */
    Cd90AutoScheduleParameters load(String factoryCode, int enabledShiftCount);
}
