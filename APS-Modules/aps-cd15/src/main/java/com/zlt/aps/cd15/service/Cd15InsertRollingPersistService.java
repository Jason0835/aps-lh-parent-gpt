package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import com.zlt.aps.cd15.engine.model.Cd15InsertRollingOutput;
import org.redisson.api.RLock;

/**
 * 斜裁插单滚动结果原子持久化服务。
 */
public interface Cd15InsertRollingPersistService {

    /**
     * 在原批次内一次性提交插单和受影响结果。
     *
     * @param taskId 异步任务ID
     * @param request 插单请求
     * @param output 内存滚动输出
     * @param lock 当前线程持有的执行锁
     */
    void persist(String taskId, Cd15InsertOrderRequest request,
                 Cd15InsertRollingOutput output, RLock lock);

    /**
     * 在原批次内一次性提交转机台和受影响结果。
     *
     * @param taskId 异步任务ID
     * @param request 转机台请求
     * @param output 内存滚动输出
     * @param lock 当前线程持有的执行锁
     */
    void persistTransfer(String taskId, Cd15TransferMachineRequest request,
                         Cd15InsertRollingOutput output, RLock lock);

    /**
     * 在原批次内一次性提交调量和受影响结果。
     *
     * @param taskId 异步任务ID
     * @param request 调量请求
     * @param output 内存滚动输出
     * @param lock 当前线程持有的执行锁
     */
    void persistChangeQty(String taskId, Cd15ChangeQtyRequest request,
                          Cd15InsertRollingOutput output, RLock lock);
}
