package com.zlt.aps.controller.raw;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.RawWeekUsage;
import com.zlt.aps.monthplan.api.service.IRawWeekUsageRemoteService;
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
import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawWeekUsageUIController.java
 * 描    述：周维度原材料用量记录 UI控制层类：....
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
@Api(tags = "周维度原材料用量记录")
@Controller
@RequestMapping("/maindata/rawWeekUsage")
public class RawWeekUsageUIController extends BaseUIController<RawWeekUsage> {

    @Autowired
    private IRawWeekUsageRemoteService iRawWeekUsageService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("maindata:rawWeekUsage:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(RawWeekUsage rawWeekUsage) {
        return iRawWeekUsageService.list(rawWeekUsage);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     */
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
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, RawWeekUsage entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iRawWeekUsageService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("maindata:rawWeekUsage:generateByMonth")
    @PostMapping("/generate-by-month")
    @ResponseBody
    @ApiOperation("按照月份生成周维度原材料用量记录")
    public AjaxResult generateByMonth(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("year") Integer year,
                                      @RequestParam("month") Integer month) {
        return iRawWeekUsageService.generateByMonth(factoryCode, year, month);
    }

    @RequiresPermissions("maindata:rawWeekUsage:generateByWeek")
    @PostMapping("/generate-by-week")
    @ResponseBody
    @ApiOperation("按照周维度份生成周维度原材料用量记录")
    public AjaxResult generateByWeek(@RequestParam("factoryCode") String factoryCode,
                                     @RequestParam("year") Integer year,
                                     @RequestParam("month") Integer month,
                                     @RequestParam("week") Integer week) {
        return iRawWeekUsageService.generateByWeek(factoryCode, year, month, week);
    }

    @RequiresPermissions("maindata:rawWeekUsage:statistics")
    @GetMapping("/statistics")
    @ResponseBody
    @ApiOperation("获取周用量统计数据")
    public AjaxResult getStatistics(@RequestParam("factoryCode") String factoryCode,
                                    @RequestParam("year") Integer year,
                                    @RequestParam(value = "month", required = false) Integer month,
                                    @RequestParam(value = "week", required = false) Integer week) {
        Map<String, Object> statistics = iRawWeekUsageService
                .getStatistics(factoryCode, year, month, week);
        return AjaxResult.success(statistics);
    }
}