package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.api.domain.vo.Cd90ChangeQtyRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.api.domain.vo.Cd90TransferMachineRequest;
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

    /**
     * 在内存中完成转机台清源、目标机台滚动和跨班顺延。
     *
     * @param request 转机台请求
     * @return 可供最终短事务提交的输出
     */
    Cd90InsertRollingOutput executeTransfer(Cd90TransferMachineRequest request);

    /**
     * 在内存中完成指定机台、指定帘布的调量滚动重排。
     *
     * @param request 调量请求
     * @return 可供最终短事务提交的输出
     */
    Cd90InsertRollingOutput executeChangeQty(Cd90ChangeQtyRequest request);
}
