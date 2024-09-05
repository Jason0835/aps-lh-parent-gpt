package com.zlt.aps.controller.cd15;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleAssist;
import com.zlt.aps.cd15.api.service.ICd15ScheduleAssistService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
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
import java.util.Date;

/**
 * 15度裁断外协排程结果Controller
 * @author chen
 * @date 2022-02-16
 */
@Api(tags = "15度裁断外协排程结果")
@Controller
@RequestMapping("/cd15/assistSchedule")
public class Cd15ScheduleAssistController extends BaseController {

    @Autowired
    private ICd15ScheduleAssistService iCd15ScheduleAssistService;
    @Autowired
    private IExportLogService iExportLogService;

    private final String prefix = "cd15/assistSchedule";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd15:assistSchedule:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/assistSchedule";
    }

    /**
     * 根据条件查询15度裁断外协排程结果列表
     */
    @ApiOperation("根据条件查询15度裁断外协排程结果列表")
    @RequiresPermissions("cd15:assistSchedule:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15ScheduleAssist entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iCd15ScheduleAssistService.list(entity);
    }

    /**
     * 导出15度裁断外协排程结果
     */
    @ApiOperation("导出15度裁断外协排程结果")
    @RequiresPermissions("cd15:assistSchedule:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15ScheduleAssist scheduleAssist) throws Exception {
        //若是没传日期则默认查询当日排程
        if (scheduleAssist.getScheduleDate() == null) {
            scheduleAssist.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        scheduleAssist.setYear(DateFormatUtils.format(scheduleAssist.getScheduleDate(), "yyyy"));
        scheduleAssist.setMonth(DateFormatUtils.format(scheduleAssist.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iCd15ScheduleAssistService.export(scheduleAssist);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.cd15.assistSchedule.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, scheduleAssist.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }
}
