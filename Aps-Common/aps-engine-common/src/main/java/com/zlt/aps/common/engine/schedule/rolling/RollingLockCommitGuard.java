package com.zlt.aps.common.engine.schedule.rolling;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.function.BooleanSupplier;

/**
 * 提交前在拥有者线程确认锁有效；分布式锁类型保留在业务模块。
 */
public class RollingLockCommitGuard implements TransactionSynchronization {
    private final BooleanSupplier ownsLocks;

    /**
     * 接收同线程锁所有权检查。
     *
     * @param ownsLocks 业务模块提供的日锁与机台锁所有权检查
     */
    public RollingLockCommitGuard(BooleanSupplier ownsLocks) {
        this.ownsLocks = ownsLocks;
    }

    /**
     * 提交前再次验证所有权，避免失效沙箱写入。
     *
     * @param readOnly 当前事务是否只读
     * @throws ServiceException 锁已失效时拒绝提交
     */
    @Override
    public void beforeCommit(boolean readOnly) {
        if (!this.ownsLocks.getAsBoolean()) {
            throw new ServiceException(I18nUtil.getMessage("ui.autoRolling.concurrentChange"));
        }
    }
}
