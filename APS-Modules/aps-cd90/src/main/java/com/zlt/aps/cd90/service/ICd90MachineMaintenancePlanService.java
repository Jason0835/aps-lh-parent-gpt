package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd90MachineMaintenancePlanService extends IDocService<Cd90MachineMaintenancePlan> {

    String checkUnique(Cd90MachineMaintenancePlan entity);
    AjaxResult validateForSave(Cd90MachineMaintenancePlan entity);
    AjaxResult importData(List<Cd90MachineMaintenancePlan> list, boolean updateSupport, Long importLogId);
}