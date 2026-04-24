package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.bill.common.service.IDocService;

public interface ITqMachineSpecSpeedService extends IDocService<TqMachineSpecSpeed> {

    String checkUnique(TqMachineSpecSpeed machineSpecSpeed);

    void deleteAll();
}
