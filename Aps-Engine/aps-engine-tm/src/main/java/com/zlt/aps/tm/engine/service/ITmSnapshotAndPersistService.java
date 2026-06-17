package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.engine.domain.TmScheduleContext;

/**
 * 胎面解释快照和落库步骤服务。
 *
 * <p>负责构建解释快照并统一调用落库能力。事务边界由外层业务入口控制，不在策略或规则中开启。</p>
 */
public interface ITmSnapshotAndPersistService {

    /**
     * 执行解释快照构建和落库。
     *
     * @param context 胎面排程上下文，方法会按实现写入结果和解释
     */
    void snapshotAndPersist(TmScheduleContext context);
}
