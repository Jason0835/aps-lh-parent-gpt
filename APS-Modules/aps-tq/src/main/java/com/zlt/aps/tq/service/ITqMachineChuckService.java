package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import com.zlt.bill.common.service.IDocService;

public interface ITqMachineChuckService extends IDocService<TqMachineChuck> {

    String checkUnique(TqMachineChuck machineChuck);

    void deleteAllMachineChuck();
}
