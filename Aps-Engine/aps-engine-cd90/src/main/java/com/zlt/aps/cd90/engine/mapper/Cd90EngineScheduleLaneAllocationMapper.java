package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import org.apache.ibatis.annotations.Mapper;

/** 插单滚动读取原排程库排明细的只读Mapper。 */
@Mapper
public interface Cd90EngineScheduleLaneAllocationMapper
        extends BaseMapper<Cd90ScheduleLaneAllocation> {
}
