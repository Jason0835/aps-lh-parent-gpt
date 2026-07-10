package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程机台大卷映射只读 Mapper。
 */
@Mapper
public interface Cd15EngineMachineRollMappingMapper extends BaseMapper<Cd15MachineRollMapping> {
}