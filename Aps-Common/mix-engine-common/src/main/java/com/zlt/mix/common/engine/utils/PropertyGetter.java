package com.zlt.mix.common.engine.utils;

/**
 * @author Gim
 */
public interface PropertyGetter<T, K> {

    public K getProperty(T obj);
}