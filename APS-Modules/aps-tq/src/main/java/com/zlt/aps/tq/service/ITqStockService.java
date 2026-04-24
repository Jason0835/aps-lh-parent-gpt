package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.bill.common.service.IDocService;

public interface ITqStockService extends IDocService<TqStock> {

    String checkUnique(TqStock entity);
}
