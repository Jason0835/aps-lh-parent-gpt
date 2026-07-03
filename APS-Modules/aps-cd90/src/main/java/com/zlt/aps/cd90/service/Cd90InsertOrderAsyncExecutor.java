package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;

/**
 * 直裁插单异步执行入口。
 */
public interface Cd90InsertOrderAsyncExecutor {

    /**
     * 异步执行插单滚动重排。
     *
     * @param taskId 异步任务ID
     * @param request 插单请求快照
     */
    void execute(String taskId, Cd90InsertOrderRequest request);
}
