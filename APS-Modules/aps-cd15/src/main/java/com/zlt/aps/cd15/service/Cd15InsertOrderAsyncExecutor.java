package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;

/**
 * 斜裁插单异步执行入口。
 */
public interface Cd15InsertOrderAsyncExecutor {

    /**
     * 异步执行插单滚动重排。
     *
     * @param taskId 异步任务ID
     * @param request 插单请求快照
     */
    void execute(String taskId, Cd15InsertOrderRequest request);

    /**
     * 异步执行转机台滚动重排。
     *
     * @param taskId 异步任务ID
     * @param request 转机台请求快照
     */
    void executeTransfer(String taskId, Cd15TransferMachineRequest request);

    /**
     * 异步执行调量滚动重排。
     *
     * @param taskId 异步任务ID
     * @param request 调量请求快照
     */
    void executeChangeQty(String taskId, Cd15ChangeQtyRequest request);
}
