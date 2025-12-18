package com.zlt.aps.controller.raw;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;


import com.zlt.aps.maindata.api.IRawWarningRecordRemoteService;
import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawWarningRecordUIController.java
 * 描    述：原材料预警记录 UI控制层类：....
 *@author zlt
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "原材料预警记录")
@Controller
@RequestMapping("/maindata/rawWarningRecord")
public class RawWarningRecordUIController extends BaseUIController<RawWarningRecord> {

    @Autowired
    private IRawWarningRecordRemoteService iRawWarningRecordService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:rawWarningRecord:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RawWarningRecord rawWarningRecord) {
        return iRawWarningRecordService.list(rawWarningRecord);
    }

    @Override
    public String getExportTemplateFileName(){
        return this.getFunctionName();
    }

    /**
     * 继承时重写方法。
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.rawWarningRecord.modelName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, RawWarningRecord entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iRawWarningRecordService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("maindata:rawWarningRecord:executeUsageWarning")
    @PostMapping("/execute-usage-warning")
    @ResponseBody
    @ApiOperation("执行用量偏差预警")
    public AjaxResult executeUsageWarning(@RequestParam("factoryCode") String factoryCode,
                                          @RequestParam("year") Integer year,
                                          @RequestParam("week") Integer week) {
        return iRawWarningRecordService.executeUsageWarning(factoryCode, year, week);
    }

    @RequiresPermissions("maindata:rawWarningRecord:executeNewMaterialWarning")
    @PostMapping("/execute-new-material-warning")
    @ResponseBody
    @ApiOperation("执行新材料预警")
    public AjaxResult executeNewMaterialWarning(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("year") Integer year,
                                                @RequestParam("month") Integer month) {
        return iRawWarningRecordService.executeNewMaterialWarning(factoryCode, year, month);
    }

    @RequiresPermissions("maindata:rawWarningRecord:syncActualUsage")
    @PostMapping("/sync-actual-usage")
    @ResponseBody
    @ApiOperation("同步实际用量数据")
    public AjaxResult syncActualUsage(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("year") Integer year,
                                      @RequestParam("week") Integer week) {
        return iRawWarningRecordService.syncActualUsage(factoryCode, year, week);
    }

    @RequiresPermissions("maindata:rawWarningRecord:handleWarning")
    @PostMapping("/handle-warning")
    @ResponseBody
    @ApiOperation("处理预警记录")
    public AjaxResult handleWarning(@RequestParam("id") Long id,
                                    @RequestParam("handler") String handler,
                                    @RequestParam("opinion") String opinion) {
        return iRawWarningRecordService.handleWarning(id, handler, opinion);
    }

    @RequiresPermissions("maindata:rawWarningRecord:statistics")
    @GetMapping("/statistics")
    @ResponseBody
    @ApiOperation("获取预警统计")
    public AjaxResult getStatistics(@RequestParam("factoryCode") String factoryCode,
                                    @RequestParam(value = "warningType", required = false) String warningType,
                                    @RequestParam(value = "days", required = false) Integer days) {
        Map<String, Object> statistics = iRawWarningRecordService.getStatistics(
                factoryCode, warningType, days);
        return AjaxResult.success(statistics);
    }
}