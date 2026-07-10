package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程指定机台只读 Mapper。
 */
@Mapper
public interface Cd15EngineSpecifyMachineMapper extends BaseMapper<Cd15SpecifyMachine> {
}