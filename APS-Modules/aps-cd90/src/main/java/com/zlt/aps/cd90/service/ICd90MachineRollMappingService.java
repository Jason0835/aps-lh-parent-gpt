package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd90MachineRollMappingService extends IDocService<Cd90MachineRollMapping> {
    String checkUnique(Cd90MachineRollMapping entity);
    AjaxResult importData(List<Cd90MachineRollMapping> list, boolean updateSupport, Long importLogId);
}