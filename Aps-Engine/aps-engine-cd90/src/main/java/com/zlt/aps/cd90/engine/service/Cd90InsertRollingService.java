package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;

/**
 * 直裁插单滚动重排服务。
 */
public interface Cd90InsertRollingService {

    /**
     * 在内存中完成固定顺位插入和跨班顺延。
     *
     * @param request 插单请求
     * @return 可供最终短事务提交的输出
     */
    Cd90InsertRollingOutput execute(Cd90InsertOrderRequest request);
}
