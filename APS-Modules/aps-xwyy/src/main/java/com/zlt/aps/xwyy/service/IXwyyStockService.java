package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface IXwyyStockService extends IDocService<XwyyStock> {
    String checkUnique(XwyyStock entity);
    AjaxResult importData(List<XwyyStock> list, boolean updateSupport, Long importLogId);
}