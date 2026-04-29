package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqMachineMaintenancePlan;
import com.zlt.bill.common.service.IDocService;

public interface ITqMachineMaintenancePlanService extends IDocService<TqMachineMaintenancePlan> {

    String checkUnique(TqMachineMaintenancePlan entity);

    void deleteAllMachineMaintenancePlan();
}
