package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqLossSetting;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ITqLossSettingService extends IDocService<TqLossSetting> {

    String checkUnique(TqLossSetting lossSetting);

    List<TqLossSetting> listLossSetting(TqLossSetting lossSetting);

    void deleteAll();
}
