package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程班次配置读取Mapper。
 */
@Mapper
public interface Cd90AutoScheduleShiftMapper extends BaseMapper<Cd90ShiftConfig> {
}
