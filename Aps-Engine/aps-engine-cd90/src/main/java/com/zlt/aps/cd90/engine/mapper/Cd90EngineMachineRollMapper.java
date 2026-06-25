package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import org.apache.ibatis.annotations.Mapper;

/** 自动排程大卷机台绑定只读Mapper。 */
@Mapper
public interface Cd90EngineMachineRollMapper extends BaseMapper<Cd90MachineRollMapping> {
}
