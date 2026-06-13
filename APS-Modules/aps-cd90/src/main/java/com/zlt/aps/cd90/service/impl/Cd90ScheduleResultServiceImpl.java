package com.zlt.aps.cd90.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleEngineService;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheduleResultServiceImpl extends AbstractDocService<Cd90ScheduleResult> implements ICd90ScheduleResultService {

    @Resource
    private Cd90AutoScheduleEngineService cd90AutoScheduleEngineService;

    /**
     * 接收自动排程请求。
     * 排程算法统一由Aps-Engine中的直裁引擎实现，本服务只负责业务接口转发。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 接口调用成功
     */
    @Override
    public AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return AjaxResult.error("自动排程请求不能为空");
        }
        Cd90AutoScheduleContext context = cd90AutoScheduleEngineService.prepare(
                scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate());
        return AjaxResult.success("自动排程基础数据校验完成", context);
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
