package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import org.apache.ibatis.annotations.Mapper;

/** 自动排程机台档案只读Mapper。 */
@Mapper
public interface Cd15EngineMachineInfoMapper extends BaseMapper<Cd15MachineInfo> {
}
