package com.zlt.sync.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.sync.domain.AuxReqSyncDataLogsHis;

/**
 * 添加请求数据历史
 */
@DS(DataSource.MASTER)
@Mapper
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
