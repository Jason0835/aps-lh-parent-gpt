package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleAssist;
import com.zlt.aps.xwyy.api.service.IXwyyScheduleAssistService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 纤维压延外协排程结果Controller
 *
 * @author chen
 * @date 2022-02-16
 */
@Api(tags = "纤维压延外协排程结果")
@Controller
@RequestMapping("/xwyy/assistSchedule")
public class XwyyScheduleAssistController extends BaseController {

    @Autowired
    private IXwyyScheduleAssistService iXwyyScheduleAssistService;
    @Autowired
    private IExportLogService iExportLogService;

    private final String prefix = "xwyy/assistSchedule";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("xwyy:assistSchedule:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr( "yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/assistSchedule";
    }

    /**
     * 根据条件查询纤维压延外协排程结果列表
     */
    @ApiOperation("根据条件查询纤维压延外协排程结果列表")
    @RequiresPermissions("xwyy:assistSchedule:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyScheduleAssist entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iXwyyScheduleAssistService.list(entity);
    }

    /**
     * 导出纤维压延外协排程结果
     */
    @ApiOperation("导出纤维压延外协排程结果")
    @RequiresPermissions("xwyy:assistSchedule:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyScheduleAssist scheduleAssist) throws IOException {
        //若是没传日期则默认查询当日排程
        if (scheduleAssist.getScheduleDate() == null) {
            scheduleAssist.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        scheduleAssist.setYear(DateFormatUtils.format(scheduleAssist.getScheduleDate(), "yyyy"));
        scheduleAssist.setMonth(DateFormatUtils.format(scheduleAssist.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iXwyyScheduleAssistService.export(scheduleAssist);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.xwyy.assistSchedule.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data,fileName, scheduleAssist.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }
}
