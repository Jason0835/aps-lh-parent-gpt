package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钢丝圈自动滚动异步任务 Mapper
 *
 * @author APS
 */
@Mapper
public interface GsqAutoScheduleTaskMapper extends BaseMapper<GsqAutoScheduleTask> {
}
