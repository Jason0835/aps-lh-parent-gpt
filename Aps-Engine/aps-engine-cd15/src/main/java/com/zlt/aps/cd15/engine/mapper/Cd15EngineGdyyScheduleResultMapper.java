package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程 GDYY 排程结果只读 Mapper。
 */
@Mapper
public interface Cd15EngineGdyyScheduleResultMapper extends BaseMapper<GdyyScheduleResult> {
}