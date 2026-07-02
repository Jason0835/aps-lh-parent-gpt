package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.vo.Cd90InsertOrderRequest;
import com.zlt.aps.cd90.engine.model.Cd90InsertRollingOutput;
import org.redisson.api.RLock;

/**
 * 直裁插单滚动结果原子持久化服务。
 */
public interface Cd90InsertRollingPersistService {

    /**
     * 在原批次内一次性提交插单和受影响结果。
     *
     * @param taskId 异步任务ID
     * @param request 插单请求
     * @param output 内存滚动输出
     * @param lock 当前线程持有的执行锁
     */
    void persist(String taskId, Cd90InsertOrderRequest request,
                 Cd90InsertRollingOutput output, RLock lock);
}
