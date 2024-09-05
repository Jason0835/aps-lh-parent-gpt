package com.zlt.aps.common.engine.service.impl;

import java.util.List;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.StorageLocationCast;
import com.zlt.aps.common.engine.mapper.StorageLocationCastMapper;
import com.zlt.aps.common.engine.service.StorageLocationCastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 库存地点映射(没有功能界面，库配置)Service业务层处理
 * 
 * @author zlt
 * @date 2021-09-28
 */
@Service("storageLocationCastService")
public class StorageLocationCastServiceImpl implements StorageLocationCastService
{

    @Autowired
    private StorageLocationCastMapper storageLocationCastMapper;

    /**
     * 根据生产排程的库存地点信息获取是否为配套胎验证逻辑
     * @param apsStorageLoaction 生产排程对应的库存地点
     * @return
     */
    @Override
    public boolean isAssort(String apsStorageLoaction) {
        //如果传入库存地点为空则默认不是配套胎
        if(StringUtils.isNotEmpty(apsStorageLoaction)){
            StorageLocationCast condition =new StorageLocationCast();
            condition.setApsStorageLocation(apsStorageLoaction);
            List<StorageLocationCast> castList=this.storageLocationCastMapper.selectStorageLocationCastList(condition);
            //如果存在多条则取最新创建的记录
            if(StringUtils.isNotEmpty(castList)){
                StorageLocationCast cast =castList.get(0);
                if(EngineConstants.ASSORT_TYPE_YES.equals(cast.getTireStoreType())){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据生产排程库存地点进行主计划库区转换
     * @param apsStorageLocation 生产排程库存地点编码
     * @return
     */
    @Override
    public String getMpsStorageLocationByApsStorageLoaction(String apsStorageLocation) {
        StorageLocationCast condition =new StorageLocationCast();
        condition.setApsStorageLocation(apsStorageLocation);
        List<StorageLocationCast> castList=this.storageLocationCastMapper.selectStorageLocationCastList(condition);
        //如果存在多条则取最新创建的记录
        if(StringUtils.isNotEmpty(castList)){
            StorageLocationCast cast =castList.get(0);
            return cast.getMpsStorageLocation();
        }
        return null;
    }

    /**
     * 根据主计划库区编码转换生产排程库存地点编码转换
     * @param mpsStorageLocation 主计划库存地点编码
     * @return
     */
    @Override
    public String getApsStorageLocationByMpsStorageLocation(String mpsStorageLocation) {
        StorageLocationCast condition =new StorageLocationCast();
        condition.setMpsStorageLocation(mpsStorageLocation);
        List<StorageLocationCast> castList=this.storageLocationCastMapper.selectStorageLocationCastList(condition);
        //如果存在多条则取最新创建的记录
        if(StringUtils.isNotEmpty(castList)){
            StorageLocationCast cast =castList.get(0);
            return cast.getApsStorageLocation();
        }
        return null;
    }

    /**
     * 根据传入的库存地点获取维护的数据
     * @param apsStorageLocation
     * @param mpsStorageLoaction
     * @return
     */
    @Override
    public StorageLocationCast getStorageLocationCastByStorageLocation(String apsStorageLocation, String mpsStorageLoaction) {
        StorageLocationCast condition =new StorageLocationCast();
        if(StringUtils.isNotEmpty(apsStorageLocation)){
            condition.setApsStorageLocation(apsStorageLocation);
        }
        if(StringUtils.isNotEmpty(mpsStorageLoaction)){
            condition.setMpsStorageLocation(mpsStorageLoaction);
        }
        List<StorageLocationCast> castList=this.storageLocationCastMapper.selectStorageLocationCastList(condition);
        //如果存在多条则取最新创建的记录
        if(StringUtils.isNotEmpty(castList)){
            StorageLocationCast cast =castList.get(0);
            return cast;
        }
        return null;
    }
}
