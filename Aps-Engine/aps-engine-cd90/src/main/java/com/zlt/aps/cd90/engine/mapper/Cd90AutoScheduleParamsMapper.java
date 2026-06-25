package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程参数读取Mapper。
 */
@Mapper
public interface Cd90AutoScheduleParamsMapper extends BaseMapper<Cd90Params> {
}
