package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd90StorageLaneLimitService extends IDocService<Cd90StorageLaneLimit> {
    String checkUnique(Cd90StorageLaneLimit entity);
    AjaxResult importData(List<Cd90StorageLaneLimit> list, boolean updateSupport, Long importLogId);
}