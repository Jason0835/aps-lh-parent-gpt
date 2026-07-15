package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 斜裁大卷与机台映射业务接口。
 */
public interface ICd15MachineRollMappingService extends IDocService<Cd15MachineRollMapping> {

    AjaxResult saveWithConfirm(Cd15MachineRollMapping entity);

    String checkUnique(Cd15MachineRollMapping entity);

    AjaxResult importData(List<Cd15MachineRollMapping> list, boolean updateSupport, Long importLogId);
}
