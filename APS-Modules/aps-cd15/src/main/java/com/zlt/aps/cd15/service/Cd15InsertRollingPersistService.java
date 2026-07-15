package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.vo.Cd15ChangeQtyRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15InsertOrderRequest;
import com.zlt.aps.cd15.api.domain.vo.Cd15TransferMachineRequest;
import org.redisson.api.RLock;

/** CD15斜裁插单、转机台、调量最终短事务服务。 */
public interface Cd15InsertRollingPersistService {

    /**
     * 提交插单结果。
     *
     * @param taskId 任务ID
     * @param request 插单请求
     * @param lock 执行锁
     */
    void persist(String taskId, Cd15InsertOrderRequest request, RLock lock);

    /**
     * 提交转机台结果。
     *
     * @param taskId 任务ID
     * @param request 转机台请求
     * @param lock 执行锁
     */
    void persistTransfer(String taskId, Cd15TransferMachineRequest request, RLock lock);

    /**
     * 提交调量结果。
     *
     * @param taskId 任务ID
     * @param request 调量请求
     * @param lock 执行锁
     */
    void persistChangeQty(String taskId, Cd15ChangeQtyRequest request, RLock lock);
}