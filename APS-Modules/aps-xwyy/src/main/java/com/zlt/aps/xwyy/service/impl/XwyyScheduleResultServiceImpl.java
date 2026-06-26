package com.zlt.aps.xwyy.service.impl;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.mapper.XwyyScheduleResultMapper;
import com.zlt.aps.xwyy.service.IXwyyScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class XwyyScheduleResultServiceImpl extends AbstractDocService<XwyyScheduleResult> implements IXwyyScheduleResultService {

    @Resource
    private XwyyScheduleResultMapper xwyyScheduleResultMapper;

    @Override
    protected String getDocTypeCode() {
        return "XWYY_SCHEDULE_RESULT";
    }

    @Override
    public AjaxResult autoSchedule(XwyyScheduleResult entity) {
        if (PubUtil.isEmpty(entity.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.required.factoryCode"));
        }
        if (entity.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.required.scheduleDate"));
        }
        // 检查是否已有排程结果
        // TODO: 实现自动排程算法，创建异步任务，返回 taskId
        return AjaxResult.success(I18nUtil.getMessage("ui.message.xwyyScheduleResult.autoSchedule.success"));
    }

    @Override
    public AjaxResult insert(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult changeMachine(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult adjustQty(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult publish(XwyyScheduleResult entity) {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult importData(List<XwyyScheduleResult> list, boolean updateSupport, Long importLogId) {
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success"));
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("XWYY_SCHEDULE_RESULT");
        return t;
    }
}
