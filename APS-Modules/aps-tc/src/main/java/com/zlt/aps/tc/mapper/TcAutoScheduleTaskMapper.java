package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎侧自动排程异步任务 Mapper。
 */
@Mapper
public interface TcAutoScheduleTaskMapper extends CommBaseMapper<TcAutoScheduleTask> {
}