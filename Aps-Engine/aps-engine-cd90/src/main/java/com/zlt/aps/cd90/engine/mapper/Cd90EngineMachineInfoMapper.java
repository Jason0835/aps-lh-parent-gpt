package com.zlt.aps.cd90.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import org.apache.ibatis.annotations.Mapper;

/** 自动排程机台档案只读Mapper。 */
@Mapper
public interface Cd90EngineMachineInfoMapper extends BaseMapper<Cd90MachineInfo> {
}
