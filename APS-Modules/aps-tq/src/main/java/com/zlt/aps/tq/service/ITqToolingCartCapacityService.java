package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;
import com.zlt.bill.common.service.IDocService;

public interface ITqToolingCartCapacityService extends IDocService<TqToolingCartCapacity> {

    String checkUnique(TqToolingCartCapacity entity);

    void deleteAllToolingCartCapacity();
}
