package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.vo.TmRollingRecalcResponseVO;

/**
 * 胎面自动滚动重算服务。
 */
public interface ITmRollingUpdateService {

    /**
     * 手动执行滚动重算，自动开关关闭时仍允许调用。
     *
     * @param request 滚动重算请求
     * @return 滚动重算响应
     * @throws com.ruoyi.common.exception.ServiceException 参数、状态、锁或事务处理失败时抛出
     */
    TmRollingRecalcResponseVO rollingRecalc(TmRollingRecalcRequestDTO request);

    /**
     * 由定时任务执行滚动重算，调用前必须满足工厂开关和提前量窗口。
     *
     * @param request 滚动重算请求
     * @return 滚动重算响应
     * @throws com.ruoyi.common.exception.ServiceException 参数、状态、锁或事务处理失败时抛出
     */
    TmRollingRecalcResponseVO rollingRecalcAutomatically(TmRollingRecalcRequestDTO request);
}
