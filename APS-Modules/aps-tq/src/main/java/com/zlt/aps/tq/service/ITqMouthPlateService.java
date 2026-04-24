package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqMouthPlate;
import com.zlt.bill.common.service.IDocService;

public interface ITqMouthPlateService extends IDocService<TqMouthPlate> {

    String checkUnique(TqMouthPlate mouthPlate);

    void deleteAll();
}
