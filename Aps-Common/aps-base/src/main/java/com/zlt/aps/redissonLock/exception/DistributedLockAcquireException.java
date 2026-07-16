package com.zlt.aps.redissonLock.exception;

/**
 * 分布式锁获取失败异常。
 */
public class DistributedLockAcquireException extends RuntimeException {

    public DistributedLockAcquireException(String message) {
        super(message);
    }
}
