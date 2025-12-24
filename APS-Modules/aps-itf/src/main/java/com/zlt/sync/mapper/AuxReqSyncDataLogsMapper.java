package com.zlt.sync.mapper;

import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 请求数据历史记录
 */
public interface AuxReqSyncDataLogsMapper {

    List<AuxReqSyncDataLogs> queryReqSyncDataLogs(Map<String, Object> params);

    /**
     * 插入请求状态数据
     * @param auxReqSyncDataLogs
     * @return
     */
    int insert(@Param("auxReqSyncDataLogs") AuxReqSyncDataLogs auxReqSyncDataLogs);

    /**
     * 更新请求状态数据
     * @param auxReqSyncDataLogs
     * @return
     */
    int update(@Param("auxReqSyncDataLogs") AuxReqSyncDataLogs auxReqSyncDataLogs);

    /**
     * 根据主键删除
     * @param params
     * @return
     */
    int deleteByParams(Map<String, Object> params);
}
