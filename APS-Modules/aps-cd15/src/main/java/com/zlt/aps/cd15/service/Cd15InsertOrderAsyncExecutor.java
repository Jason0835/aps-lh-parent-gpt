package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;

/** CD15斜裁插单、转机台、调量异步执行入口。 */
public interface Cd15InsertOrderAsyncExecutor {

    /**
     * 异步执行插单。
     *
     * @param taskId 任务ID
     * @param request 插单请求
     */
    void execute(String taskId, Cd15InsertOrderRequest request);

    /**
     * 异步执行转机台。
     *
     * @param taskId 任务ID
     * @param request 转机台请求
     */
    void executeTransfer(String taskId, Cd15TransferMachineRequest request);

    /**
     * 异步执行调量。
     *
     * @param taskId 任务ID
     * @param request 调量请求
     */
    void executeChangeQty(String taskId, Cd15ChangeQtyRequest request);
}