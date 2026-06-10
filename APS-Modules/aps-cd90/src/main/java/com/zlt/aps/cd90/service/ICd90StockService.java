package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd90StockService extends IDocService<Cd90Stock> {
    String checkUnique(Cd90Stock entity);
    AjaxResult importData(List<Cd90Stock> list, boolean updateSupport, Long importLogId);
}