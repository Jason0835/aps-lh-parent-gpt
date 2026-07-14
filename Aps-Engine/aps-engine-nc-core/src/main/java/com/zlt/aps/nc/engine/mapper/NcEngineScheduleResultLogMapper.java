package com.zlt.aps.nc.engine.mapper;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.nc.api.domain.entity.NcScheduleResultLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * 内衬排程结果日志 Mapper
 */
public interface NcEngineScheduleResultLogMapper extends CommBaseMapper<NcScheduleResultLog> {

    /**
     * 将指定日期的排程数据归档到日志表
     * @param scheduleDate 排程日期
     * @return 插入行数
     */
    int syncNcScheduleToLog(@Param("scheduleDate") String scheduleDate);
}
