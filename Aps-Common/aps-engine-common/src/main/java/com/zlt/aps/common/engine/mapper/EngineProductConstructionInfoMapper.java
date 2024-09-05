package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 投产施工信息Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-12-02
 */
public interface EngineProductConstructionInfoMapper 
{
    /**
     * 查询施工信息
     * 
     * @param id 施工信息ID
     * @return 施工信息
     */
    public EngineProductConstructionInfo selectEngineProductConstructionById(Long id);

    /**
     * 查询施工信息列表
     * 
     * @param engineProductConstructionInfo 施工信息
     * @return 施工信息集合
     */
    public List<EngineProductConstructionInfo> selectEngineProductConstructionInfoList(EngineProductConstructionInfo engineProductConstructionInfo);

}
