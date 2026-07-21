package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 斜裁损耗率设定业务接口。
 */
public interface ICd15LossSettingService extends IDocService<Cd15LossSetting> {

    String checkUnique(Cd15LossSetting entity);

    AjaxResult importData(List<Cd15LossSetting> list, boolean updateSupport, Long importLogId);
}
