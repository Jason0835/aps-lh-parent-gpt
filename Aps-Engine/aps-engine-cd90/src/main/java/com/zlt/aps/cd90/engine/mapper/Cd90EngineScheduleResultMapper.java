package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直裁自动排程读取历史直裁排程结果的只读Mapper。
 */
@Mapper
public interface Cd90EngineScheduleResultMapper extends BaseMapper<Cd90ScheduleResult> {
}