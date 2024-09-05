package com.zlt.aps.mps.mapper;

import com.zlt.aps.mps.domain.TServiceSyncLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.zlt.aps.mps.domain.TServiceSyncLog
 */
public interface TServiceSyncLogMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TServiceSyncLog record);

    int insertSelective(TServiceSyncLog record);

    TServiceSyncLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TServiceSyncLog record);

    int updateByPrimaryKey(TServiceSyncLog record);

    void mergeSql(List<TServiceSyncLog> list);

    int checkMpsExist(@Param("year") Integer year, @Param("month") Integer month, @Param("productionVersion") String productionVersion);
}




