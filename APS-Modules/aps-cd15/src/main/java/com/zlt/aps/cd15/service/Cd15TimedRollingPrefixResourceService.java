package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

import java.util.List;

/** CD15定时滚动前缀已排资源占用加载服务。 */
public interface Cd15TimedRollingPrefixResourceService {

    /**
     * 加载目标班次之前保留结果占用的资源。
     *
     * @param target 定时滚动目标
     * @return 前缀已排资源占用
     */
    List<Cd15RollingPrefixResourceUsage> loadPrefixResourceUsages(Cd15RollingTarget target);
}