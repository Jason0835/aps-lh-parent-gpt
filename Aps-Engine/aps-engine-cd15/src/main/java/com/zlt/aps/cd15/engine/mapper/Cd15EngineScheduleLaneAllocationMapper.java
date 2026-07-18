package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import org.apache.ibatis.annotations.Mapper;

/** 插单滚动读取原排程库排明细的只读Mapper。 */
@Mapper
public interface Cd15EngineScheduleLaneAllocationMapper
        extends BaseMapper<Cd15ScheduleLaneAllocation> {
}
