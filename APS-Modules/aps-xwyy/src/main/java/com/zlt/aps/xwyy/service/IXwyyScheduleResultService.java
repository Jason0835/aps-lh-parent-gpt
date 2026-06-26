package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface IXwyyScheduleResultService extends IDocService<XwyyScheduleResult> {

    AjaxResult autoSchedule(XwyyScheduleResult entity);

    AjaxResult insert(XwyyScheduleResult entity);

    AjaxResult changeMachine(XwyyScheduleResult entity);

    AjaxResult adjustQty(XwyyScheduleResult entity);

    AjaxResult publish(XwyyScheduleResult entity);

    AjaxResult importData(List<XwyyScheduleResult> list, boolean updateSupport, Long importLogId);
}
