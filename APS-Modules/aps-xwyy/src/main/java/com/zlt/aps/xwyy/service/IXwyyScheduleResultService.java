package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.domain.vo.XwyyScheduleResultTemplateImportVO;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface IXwyyScheduleResultService extends IDocService<XwyyScheduleResult> {

    AjaxResult autoSchedule(XwyyScheduleResult entity);

    AjaxResult insert(XwyyScheduleResult entity);

    AjaxResult changeMachine(XwyyScheduleResult entity);

    AjaxResult adjustQty(XwyyScheduleResult entity);

    AjaxResult publish(XwyyScheduleResult entity);

    AjaxResult importData(List<XwyyScheduleResult> list, boolean updateSupport, Long importLogId);

    /**
     * 按固定生产计划模板整体覆盖导入。
     *
     * @param rows 导入明细
     * @param condition 导入范围
     * @param updateSupport 是否更新
     * @return 导入结果
     */
    AjaxResult importScheduleTemplate(List<XwyyScheduleResultTemplateImportVO> rows,
                                      XwyyScheduleResult condition,
                                      boolean updateSupport);

    byte[] exportData(List<XwyyScheduleResult> currentResults,
                      XwyyScheduleResult query);
}
