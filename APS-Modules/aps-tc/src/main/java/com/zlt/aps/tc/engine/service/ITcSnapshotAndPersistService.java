package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.engine.domain.TcScheduleContext;

/**
 * 胎侧解释快照和落库步骤服务。
 *
 * <p>负责构建解释快照并统一调用落库能力。事务边界由外层业务入口控制，不在策略或规则中开启。</p>
 */
public interface ITcSnapshotAndPersistService {

    /**
     * 执行解释快照构建和落库。
     *
     * @param context 胎侧排程上下文，方法会按实现写入结果和解释
     */
    void snapshotAndPersist(TcScheduleContext context);
}
