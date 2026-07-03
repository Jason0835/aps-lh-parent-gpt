package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleRollingAdjustLog;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 直裁定时滚动调整日志Mapper。 */
@Mapper
public interface Cd90ScheduleRollingAdjustLogMapper
        extends CommBaseMapper<Cd90ScheduleRollingAdjustLog> {
}
