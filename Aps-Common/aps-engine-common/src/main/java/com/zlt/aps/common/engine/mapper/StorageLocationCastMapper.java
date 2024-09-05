package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.StorageLocationCast;

import java.util.List;

/**
 * 库存地点映射(没有功能界面，库配置)Mapper接口
 * 
 * @author zlt
 * @date 2021-09-28
 */
public interface StorageLocationCastMapper 
{
    /**
     * 查询库存地点映射(没有功能界面，库配置)
     * 
     * @param id 库存地点映射(没有功能界面，库配置)ID
     * @return 库存地点映射(没有功能界面，库配置)
     */
    public StorageLocationCast selectStorageLocationCastById(Long id);

    /**
     * 查询库存地点映射(没有功能界面，库配置)列表
     * 
     * @param storageLocationCast 库存地点映射(没有功能界面，库配置)
     * @return 库存地点映射(没有功能界面，库配置)集合
     */
    public List<StorageLocationCast> selectStorageLocationCastList(StorageLocationCast storageLocationCast);
}
