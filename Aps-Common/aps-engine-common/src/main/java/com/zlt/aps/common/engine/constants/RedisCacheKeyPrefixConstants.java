package com.zlt.aps.common.engine.constants;

/**
 * redis缓存key前缀维护
 */
public class RedisCacheKeyPrefixConstants {

    /**
     * 成型工序定额缓存key
     */
    public static final String CX_QUOTA_SETTING_MAP="cx:engine:common:quota";

    /**
     * 施工信息缓存key
     */
    public static final String CONSTRUCTION_INFO_MAP="cx:engine:common:construction";

    /**
     * 胎胚代码对应施工信息缓存key
     */
    public static final String ENGINE_CONSTRUCTION_MAP="cx:engine:construction:common";

    /**
     * 成型机台信息缓存
     */
    public static final String CX_MACHINE_INFO_MAP="cx:engine:common:mainchine";
}
