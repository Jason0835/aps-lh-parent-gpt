package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;
import com.zlt.bill.common.service.IDocService;

public interface ITqSpecifyMachineService extends IDocService<TqSpecifyMachine> {

    String checkUnique(TqSpecifyMachine specifyMachine);

    void deleteAllSpecifyMachine();
}
