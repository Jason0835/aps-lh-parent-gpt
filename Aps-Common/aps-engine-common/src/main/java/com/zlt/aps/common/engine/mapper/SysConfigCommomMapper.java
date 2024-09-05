package com.zlt.aps.common.engine.mapper;

import com.ruoyi.api.gateway.system.domain.SysConfig;

/**
 * 参数配置 数据层
 * 
 */
public interface SysConfigCommomMapper
{
    /**
     * 查询参数配置信息
     * 
     * @param config 参数配置信息
     * @return 参数配置信息
     */
    public SysConfig selectConfig(SysConfig config);
}