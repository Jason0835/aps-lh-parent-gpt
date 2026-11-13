package com.zlt.aps.common.core.utils;

/**
 * @author Gim
 */
public interface PropertyGetter<T, K> {

    public K getProperty(T obj);
}