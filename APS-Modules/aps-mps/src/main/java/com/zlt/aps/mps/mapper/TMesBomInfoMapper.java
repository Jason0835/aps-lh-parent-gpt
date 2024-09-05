package com.zlt.aps.mps.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.domain.TMesBomInfo;

/**
 * @Entity com.zlt.aps.mps.domain.TMesBomInfo
 */
public interface TMesBomInfoMapper {

    List<TMesBomInfo> getBomByDataVersion(@Param("dataVersion") String dataVersion);

    /**
     * 这是业务库表，非mes中间表
     */
    public void mergeSql(@Param("dataVersion") String dataVersion);

}




