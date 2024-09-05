package com.zlt.aps.controller.tq;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.tq.api.domain.entity.TqAssistSchedule;
import com.zlt.aps.tq.api.service.ITqAssistScheduleService;
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
 * 胎圈外协排程结果Controller
 * @author chen
 * @date 2022-02-16
 */
@Api(tags = "胎圈外协排程结果")
@Controller
@RequestMapping("/tq/assistSchedule")
public class TqAssistScheduleController extends BaseController {

    @Autowired
    private ITqAssistScheduleService iTqAssistScheduleService;
    @Autowired
    private IExportLogService iExportLogService;

    private final String prefix = "tq/assistSchedule";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tq:assistSchedule:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/assistSchedule";
    }

    /**
     * 根据条件查询胎圈外协排程结果列表
     */
    @ApiOperation("根据条件查询胎圈外协排程结果列表")
    @RequiresPermissions("tq:assistSchedule:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqAssistSchedule entity) {
        //设置默认排程日期,这里在后端设置会有问题
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iTqAssistScheduleService.list(entity);
    }

    /**
     * 导出胎圈外协排程结果
     */
    @ApiOperation("导出胎圈外协排程结果")
    @RequiresPermissions("tq:assistSchedule:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqAssistSchedule assistSchedule) throws IOException {
        //若是没传日期则默认查询当日排程
        if (assistSchedule.getScheduleDate() == null) {
            assistSchedule.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        assistSchedule.setYear(DateFormatUtils.format(assistSchedule.getScheduleDate(), "yyyy"));
        assistSchedule.setMonth(DateFormatUtils.format(assistSchedule.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iTqAssistScheduleService.export(assistSchedule);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.tq.assistSchedule.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, assistSchedule.toString(), ApsConstant.PROCEDURE_CODE_TQ);
        iExportLogService.add(exportLog);
    }
}
