package com.zlt.aps.mps.mapper;
import java.util.List;

import com.zlt.aps.mps.domain.TMpsConstructionInfo;

/**
 * @Entity com.zlt.aps.mps.domain.TMpsConstructionInfo
 */
public interface TMpsConstructionInfoMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TMpsConstructionInfo record);

    int insertSelective(TMpsConstructionInfo record);

    TMpsConstructionInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TMpsConstructionInfo record);

    int updateByPrimaryKey(TMpsConstructionInfo record);

    List<TMpsConstructionInfo> selectAll();
}




