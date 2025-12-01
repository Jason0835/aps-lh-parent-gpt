package com.zlt.mix.common.core.utils;

/**
 * @author Gim
 */
public interface PropertyGetter<T, K> {

    public K getProperty(T obj);
}