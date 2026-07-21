package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程读取GDYY计划库存的只读Mapper。
 */
@Mapper
public interface Cd15EngineGdyyScheduleResultMapper extends BaseMapper<GdyyScheduleResult> {
}
