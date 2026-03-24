package com.zlt.aps.sync.mapper;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import com.zlt.aps.domain.SyncDataLogs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chen
 * @date 2025/4/27
 */
@Mapper
public interface SyncDataLogsMapper {

    /**
     * 查询同步日志，通过数据版本查询只会查询到一条记录
     *
     * @param dataVersion
     * @return
     */
    SyncDataLogs getSyncDataLogs(@Param("dataVersion") String dataVersion);

    /**
     * 查询请求日志，通过数据版本查询只会查询到一条记录
     *
     * @param dataVersion
     * @return
     */
    SyncDataLogs getReqDataLogs(@Param("dataVersion") String dataVersion);

    /**
     * 查询参数配置信息
     *
     * @param config 参数配置信息
     * @return 参数配置信息
     */
    public SysConfig selectConfig(SysConfig config);
}
