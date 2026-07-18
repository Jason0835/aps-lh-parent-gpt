package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程读取历史斜裁排程结果的只读Mapper。
 */
@Mapper
public interface Cd15EngineScheduleResultMapper extends BaseMapper<Cd15ScheduleResult> {
}