package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyShiftConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface IXwyyShiftConfigService extends IDocService<XwyyShiftConfig> {

    String checkUnique(XwyyShiftConfig entity);

    AjaxResult changeStatus(XwyyShiftConfig entity);

    AjaxResult importData(List<XwyyShiftConfig> list, boolean updateSupport, Long importLogId);
}
