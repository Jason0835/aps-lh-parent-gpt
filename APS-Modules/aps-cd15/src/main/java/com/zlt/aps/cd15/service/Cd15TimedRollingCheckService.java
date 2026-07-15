package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.vo.Cd15RollingCheckRequest;

/** CD15定时滚动排程检查服务。 */
public interface Cd15TimedRollingCheckService {

    /**
     * 检查当前是否进入滚动窗口，必要时创建滚动任务。
     *
     * @param request 检查请求
     * @return 检查结果
     */
    AjaxResult check(Cd15RollingCheckRequest request);
}