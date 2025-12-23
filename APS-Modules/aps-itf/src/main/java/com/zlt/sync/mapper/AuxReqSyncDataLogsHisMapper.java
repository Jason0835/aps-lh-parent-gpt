package com.zlt.sync.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.zlt.sync.domain.AuxReqSyncDataLogsHis;

/**
 * 添加请求数据历史
 */
public interface AuxReqSyncDataLogsHisMapper {

    /**
     * 插入请求状态数据
     * @param auxReqSyncDataLogsHis
     * @return
     */
    int insert(@Param("auxReqSyncDataLogsHis") AuxReqSyncDataLogsHis auxReqSyncDataLogsHis);

    /**
     * 根据主键删除
     * @param params
     * @return
     */
    int deleteByParams(Map<String, Object> params);
}
