package com.zlt.aps.controller.tm;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.tm.api.domain.dto.TmAssistScheduleDto;
import com.zlt.aps.tm.api.service.ITmAssistScheduleService;
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
 * 胎面外协排程结果Controller
 * @author chen
 * @date 2022-02-15
 */
@Api(tags = "胎面外协排程结果")
@Controller
@RequestMapping("/tm/assistSchedule")
public class TmAssistScheduleController extends BaseController {

    @Autowired
    private ITmAssistScheduleService iTmAssistScheduleService;
    @Autowired
    private IExportLogService iExportLogService;

    private final String prefix = "tm/assistSchedule";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tm:assistSchedule:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/assistSchedule";
    }

    /**
     * 根据条件查询胎面外协排程结果列表
     */
    @ApiOperation("根据条件查询胎面外协排程结果列表")
    @RequiresPermissions("tm:assistSchedule:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmAssistScheduleDto entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        entity.setYear(DateFormatUtils.format(entity.getScheduleDate(), "yyyy"));
        entity.setMonth(DateFormatUtils.format(entity.getScheduleDate(), "MM"));
        return iTmAssistScheduleService.list(entity);
    }

    /**
     * 导出胎面外协排程结果
     */
    @ApiOperation("导出胎面外协排程结果")
    @RequiresPermissions("tm:assistSchedule:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmAssistScheduleDto assistScheduleDto) throws Exception {
        //若是没传日期则默认查询当日排程
        if (assistScheduleDto.getScheduleDate() == null) {
            assistScheduleDto.setScheduleDate(DateUtils.addDays(new Date(), 1));
        }
        assistScheduleDto.setYear(DateFormatUtils.format(assistScheduleDto.getScheduleDate(), "yyyy"));
        assistScheduleDto.setMonth(DateFormatUtils.format(assistScheduleDto.getScheduleDate(), "MM"));
        //获取字节流数据
        byte[] data = iTmAssistScheduleService.export(assistScheduleDto);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.tm.assistSchedule.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, assistScheduleDto.toString(), ApsConstant.PROCEDURE_CODE_TM);
        iExportLogService.add(exportLog);
    }
}
