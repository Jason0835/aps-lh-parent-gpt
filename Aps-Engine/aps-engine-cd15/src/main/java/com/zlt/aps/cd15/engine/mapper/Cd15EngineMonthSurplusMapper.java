package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import org.apache.ibatis.annotations.Mapper;

/** 自动排程胎胚月计划余量只读Mapper。 */
@Mapper
public interface Cd15EngineMonthSurplusMapper extends BaseMapper<MdmMonthSurplus> {
}
