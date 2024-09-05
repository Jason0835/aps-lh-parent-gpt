package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.StorageLocationCast;

/**
 * 库存地点映射(没有功能界面，库配置)Service接口
 * 
 * @author zlt
 * @date 2021-09-28
 */
public interface StorageLocationCastService
{

    /**
     * 根据生产排程的库存地点获取是否为配套胎
     * @param apsStorageLoaction
     * @return 是配套胎 返回true 不是配套胎返回false
     */
    boolean isAssort(String apsStorageLoaction);

    /**
     * 根据生产排程的库存地点获取主计划库存地点映射
     * @param apsStorageLocation
     * @return
     */
    String getMpsStorageLocationByApsStorageLoaction(String apsStorageLocation);

    /**
     * 根据主计划的库存地点键值获取生产排程对应的库存地点键值
     * @param mpsStorageLocation
     * @return
     */
    String getApsStorageLocationByMpsStorageLocation(String mpsStorageLocation);

    /**
     * 根据条件获取对应的 库存转换设置信息
     * @param apsStorageLocation
     * @param mpsStorageLoaction
     * @return
     */
    StorageLocationCast getStorageLocationCastByStorageLocation(String apsStorageLocation,String mpsStorageLoaction);


}
