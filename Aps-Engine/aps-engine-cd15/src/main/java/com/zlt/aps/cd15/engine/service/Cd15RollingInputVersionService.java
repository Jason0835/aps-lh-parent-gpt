package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

/** 定时滚动复合输入版本服务。 */
public interface Cd15RollingInputVersionService {

    /** 计算目标批次、基础数据、参数和目标班次的统一版本指纹。 */
    String fingerprint(Cd15RollingTarget target);
}
