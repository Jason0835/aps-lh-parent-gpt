package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 直裁损耗率设定业务接口。
 */
public interface ICd90LossSettingService extends IDocService<Cd90LossSetting> {

    String checkUnique(Cd90LossSetting entity);

    AjaxResult importData(List<Cd90LossSetting> list, boolean updateSupport, Long importLogId);
}