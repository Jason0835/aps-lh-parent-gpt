package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * CD15 排程引擎班次配置读取 Mapper。
 */
@Mapper
public interface Cd15EngineShiftConfigMapper extends BaseMapper<Cd15ShiftConfig> {
}
