package com.zlt.aps.cd15.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 斜裁自动排程机台检修计划只读 Mapper。
 */
@Mapper
public interface Cd15EngineMaintenanceMapper extends BaseMapper<Cd15MachineMaintenancePlan> {
}