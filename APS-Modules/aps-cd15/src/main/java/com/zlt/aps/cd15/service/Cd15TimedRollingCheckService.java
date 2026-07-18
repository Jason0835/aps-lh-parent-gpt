package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;

/** CD15定时滚动排程检查协调服务。 */
public interface Cd15TimedRollingCheckService {

    /** 检查当前交班窗口并在满足条件时创建异步滚动任务。 */
    AjaxResult check(Cd15RollingCheckRequest request);
}
