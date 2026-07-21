package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.model.Cd15InsertRollingOutput;

/**
 * 斜裁插单滚动重排服务。
 */
public interface Cd15InsertRollingService {

    /**
     * 在内存中完成固定顺位插入和跨班顺延。
     *
     * @param request 插单请求
     * @return 可供最终短事务提交的输出
     */
    Cd15InsertRollingOutput execute(Cd15InsertOrderRequest request);

    /**
     * 在内存中完成转机台清源、目标机台滚动和跨班顺延。
     *
     * @param request 转机台请求
     * @return 可供最终短事务提交的输出
     */
    Cd15InsertRollingOutput executeTransfer(Cd15TransferMachineRequest request);

    /**
     * 在内存中完成指定机台、指定钢带的调量滚动重排。
     *
     * @param request 调量请求
     * @return 可供最终短事务提交的输出
     */
    Cd15InsertRollingOutput executeChangeQty(Cd15ChangeQtyRequest request);
}
