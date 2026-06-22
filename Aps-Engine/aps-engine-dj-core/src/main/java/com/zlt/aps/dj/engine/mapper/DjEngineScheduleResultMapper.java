package com.zlt.aps.dj.engine.mapper;

import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 垫胶排程结果 Mapper
 */
public interface DjEngineScheduleResultMapper extends CommBaseMapper<DjScheduleResult> {

    /**
     * 物理删除指定日期的排产记录
     * @param scheduleDate 排程日期
     * @return 删除行数
     */
    int deleteDjSchedule(@Param("scheduleDate") String scheduleDate);
}
