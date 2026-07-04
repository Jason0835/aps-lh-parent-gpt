package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.vo.Cd90RollingCheckRequest;

/** CD90定时滚动排程检查协调服务。 */
public interface Cd90TimedRollingCheckService {

    /** 检查当前交班窗口并在满足条件时创建异步滚动任务。 */
    AjaxResult check(Cd90RollingCheckRequest request);
}
