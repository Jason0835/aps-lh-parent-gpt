package com.zlt.aps.cd90.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheduleResultServiceImpl extends AbstractDocService<Cd90ScheduleResult> implements ICd90ScheduleResultService {

    /**
     * 接收自动排程请求。
     * 当前阶段只打通调用链路，后续排程算法在此方法内实现。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 接口调用成功
     */
    @Override
    public AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult) {
        return AjaxResult.success();
    }

    @Override
    protected String getDocTypeCode() { return "CD90_SCHEDULE_RESULT"; }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_SCHEDULE_RESULT");
        return sysDocType;
    }
}
