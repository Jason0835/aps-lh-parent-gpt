package com.zlt.aps.tq.mapper;

import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 胎圈排程滚动更新日志Mapper接口
 *
 * @author APS
 */
@Mapper
public interface TqRollingLogMapper extends CommBaseMapper<TqRollingLog> {

}
