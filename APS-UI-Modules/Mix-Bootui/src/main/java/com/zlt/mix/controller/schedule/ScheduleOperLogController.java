package com.zlt.mix.controller.schedule;

import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.api.domain.dto.MaterialScheduleResultExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.ScheduleOperLogDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import com.zlt.mix.common.utils.ExportUtil;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.mix.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.util.stream.Collectors;

import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;
import com.zlt.mix.schedule.api.service.IScheduleOperLogService;

/**
 * 排程操作日志Controller
 * @author chen
 * @date 2022-07-13
 */
@Api(tags = "排程操作日志")
@Controller
@RequestMapping("/schedule/scheduleOperLog")
public class ScheduleOperLogController extends BaseController {

    @Resource
    private IScheduleOperLogService iScheduleOperLogService;
    @Resource
    private IExportLogService iExportLogService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    private final String prefix = "schedule/scheduleOperLog";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:scheduleOperLog:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/scheduleOperLog";
    }

    @ApiOperation("根据条件查询排程操作日志列表")
    @RequiresPermissions("schedule:scheduleOperLog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listScheduleOperLog(ScheduleOperLog entity) {
        return iScheduleOperLogService.listScheduleOperLog(entity);
    }

    /**
     * 导出排程操作日志
     */
    @ApiOperation("导出排程操作日志")
    @RequiresPermissions("schedule:scheduleOperLog:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,ScheduleOperLog scheduleOperLog) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.scheduleOperLog.modelName");
        ScheduleOperLogDto dictDto = new ScheduleOperLogDto();
        HashMap<String, String> mixAreaDictMap = iSysDictDataCacheService.getType("MIX_AREA").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setMixAreaDictMap(mixAreaDictMap);
        HashMap<String, String> operTypeDictMap = iSysDictDataCacheService.getType("MIX_DISPATCHER_OPER_TYPE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setOperTypeDictMap(operTypeDictMap);
        HashMap<String, String> scheduleTypeDictMap = iSysDictDataCacheService.getType("SCHEDULE_TYPE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setScheduleTypeDictMap(scheduleTypeDictMap);
        HashMap<String, String> recipeStageDictMap = iSysDictDataCacheService.getType("PRODUCT_STAGE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setRecipeStageDictMap(recipeStageDictMap);
        BeanUtils.copyProperties(scheduleOperLog, dictDto);
        byte[] data = iScheduleOperLogService.exportData(dictDto);
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dictDto.toString(), ZltConstant.PROCEDURE_CODE_MIX);
        iExportLogService.add(exportLog);
    }
}
