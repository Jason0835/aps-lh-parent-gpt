package com.zlt.aps.tm.service;

/**
 * 胎面自动排程本地短期缓存条目。
 *
 * @param <T> 缓存值类型
 */
public class TmLocalCacheEntry<T> {

    /** 缓存值 */
    private final T value;

    /** 过期时间戳，单位毫秒 */
    private final long expireAt;

    /**
     * 创建本地缓存条目。
     *
     * @param value    缓存值
     * @param expireAt 过期时间戳，单位毫秒
     */
    public TmLocalCacheEntry(T value, long expireAt) {
        this.value = value;
        this.expireAt = expireAt;
    }

    /**
     * 获取缓存值。
     *
     * @return 缓存值
     */
    public T getValue() {
        return value;
    }

    /**
     * 判断缓存是否已过期。
     *
     * @param now 当前时间戳，单位毫秒
     * @return true 表示已过期
     */
    public boolean isExpired(long now) {
        return now >= expireAt;
    }
}
