package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎面调度员排程操作日志 Mapper接口
 */
@Mapper
public interface TmDispatcherLogMapper extends CommBaseMapper<TmDispatcherLog> {
}
