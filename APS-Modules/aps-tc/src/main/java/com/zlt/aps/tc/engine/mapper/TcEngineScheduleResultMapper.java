package com.zlt.aps.tc.engine.mapper;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎侧排程结果Mapper接口（引擎专用）
 */
@Mapper
public interface TcEngineScheduleResultMapper extends CommBaseMapper<TcScheduleResult> {

}
