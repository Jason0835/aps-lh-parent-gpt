package com.zlt.aps.tm.engine.mapper;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面排程结果Mapper接口（引擎专用）
 */
@Mapper
public interface TmEngineScheduleResultMapper extends CommBaseMapper<TmScheduleResult> {

}
