package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎侧调度员排程操作日志 Mapper接口
 */
@Mapper
public interface TcDispatcherLogMapper extends CommBaseMapper<TcDispatcherLog> {
}