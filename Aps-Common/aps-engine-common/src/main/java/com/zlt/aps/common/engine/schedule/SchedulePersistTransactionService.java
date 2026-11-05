package com.zlt.aps.common.engine.schedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * 自动排程最终批次写入的声明式事务边界。
 */
@Service
public class SchedulePersistTransactionService {

    /**
     * 在同一事务中执行旧批次替换和新批次写入。
     *
     * @param persistence 最终持久化操作
     * @param <T>         持久化结果类型
     * @return 持久化结果
     * @throws RuntimeException 写入失败时传播原异常并回滚事务
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(Supplier<T> persistence) {
        return persistence.get();
    }
}
