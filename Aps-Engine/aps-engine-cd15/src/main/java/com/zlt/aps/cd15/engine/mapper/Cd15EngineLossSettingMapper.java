package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import org.apache.ibatis.annotations.Mapper;

/** 自动排程损耗率配置只读Mapper。 */
@Mapper
public interface Cd15EngineLossSettingMapper extends BaseMapper<Cd15LossSetting> {
}
