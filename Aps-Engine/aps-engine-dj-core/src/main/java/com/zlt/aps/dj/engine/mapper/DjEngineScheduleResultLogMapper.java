package com.zlt.aps.dj.engine.mapper;

import com.zlt.aps.dj.api.domain.entity.DjScheduleResultLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 垫胶排程结果日志 Mapper
 */
public interface DjEngineScheduleResultLogMapper extends CommBaseMapper<DjScheduleResultLog> {

    /**
     * 将指定日期的排程数据归档到日志表
     * @param scheduleDate 排程日期
     * @return 插入行数
     */
    int syncDjScheduleToLog(@Param("scheduleDate") String scheduleDate);
}
