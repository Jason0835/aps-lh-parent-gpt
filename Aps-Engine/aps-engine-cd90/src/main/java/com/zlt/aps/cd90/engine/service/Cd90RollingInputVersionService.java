package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;

/** 定时滚动复合输入版本服务。 */
public interface Cd90RollingInputVersionService {

    /** 计算目标批次、基础数据、参数和目标班次的统一版本指纹。 */
    String fingerprint(Cd90RollingTarget target);
}
