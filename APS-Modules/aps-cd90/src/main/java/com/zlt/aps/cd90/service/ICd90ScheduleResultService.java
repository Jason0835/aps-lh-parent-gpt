package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.bill.common.service.IDocService;

public interface ICd90ScheduleResultService extends IDocService<Cd90ScheduleResult> {

    /**
     * 执行直裁自动排程。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 自动排程结果
     */
    AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult);
}
