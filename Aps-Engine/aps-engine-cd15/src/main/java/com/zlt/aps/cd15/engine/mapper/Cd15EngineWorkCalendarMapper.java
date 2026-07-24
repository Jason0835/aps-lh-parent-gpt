package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import org.apache.ibatis.annotations.Mapper;

/** 斜裁自动排程工作日历只读Mapper。 */
@Mapper
public interface Cd15EngineWorkCalendarMapper extends BaseMapper<MdmWorkCalendar> {
}
