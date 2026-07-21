package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程参数读取Mapper。
 */
@Mapper
public interface Cd15AutoScheduleParamsMapper extends BaseMapper<Cd15Params> {
}
