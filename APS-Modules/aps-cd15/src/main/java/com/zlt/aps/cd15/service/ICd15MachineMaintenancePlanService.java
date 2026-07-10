package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd15MachineMaintenancePlanService extends IDocService<Cd15MachineMaintenancePlan> {

    String checkUnique(Cd15MachineMaintenancePlan entity);

    String checkOverlap(Cd15MachineMaintenancePlan entity);

    AjaxResult validateForSave(Cd15MachineMaintenancePlan entity);

    AjaxResult importData(List<Cd15MachineMaintenancePlan> list, boolean updateSupport, Long importLogId);
}