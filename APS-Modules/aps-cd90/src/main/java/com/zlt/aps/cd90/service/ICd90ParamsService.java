package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd90ParamsService extends IDocService<Cd90Params> {
    String checkUnique(Cd90Params entity);
    AjaxResult importData(List<Cd90Params> list, boolean updateSupport, Long importLogId);
}