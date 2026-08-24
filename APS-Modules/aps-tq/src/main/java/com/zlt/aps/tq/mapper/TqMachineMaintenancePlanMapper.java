package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqMachineMaintenancePlan;

import java.util.List;

public interface TqMachineMaintenancePlanMapper extends BaseMapper<TqMachineMaintenancePlan> {

    List<TqMachineMaintenancePlan> listMachineMaintenancePlan(TqMachineMaintenancePlan entity);

    int checkUnique(TqMachineMaintenancePlan entity);

    void deleteAllMachineMaintenancePlan();
}
