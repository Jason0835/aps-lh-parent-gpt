package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import org.apache.ibatis.annotations.Mapper;

/** 自动排程大卷机台绑定只读Mapper。 */
@Mapper
public interface Cd15EngineMachineRollMapper extends BaseMapper<Cd15MachineRollMapping> {
}
