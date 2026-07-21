package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动排程班次配置读取Mapper。
 */
@Mapper
public interface Cd15AutoScheduleShiftMapper extends BaseMapper<Cd15ShiftConfig> {
}
