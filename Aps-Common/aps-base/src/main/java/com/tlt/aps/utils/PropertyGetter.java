package com.tlt.aps.utils;

/**
 * @author Gim
 */
public interface PropertyGetter<T, K> {

    public K getProperty(T obj);
}