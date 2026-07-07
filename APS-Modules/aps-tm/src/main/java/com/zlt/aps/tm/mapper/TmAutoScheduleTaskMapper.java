package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面自动排程异步任务 Mapper。
 */
@Mapper
public interface TmAutoScheduleTaskMapper extends CommBaseMapper<TmAutoScheduleTask> {
}