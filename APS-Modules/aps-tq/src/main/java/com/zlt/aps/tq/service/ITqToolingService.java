package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqTooling;
import com.zlt.bill.common.service.IDocService;

public interface ITqToolingService extends IDocService<TqTooling> {

    String checkUnique(TqTooling tooling);

    void deleteAllTooling();
}
