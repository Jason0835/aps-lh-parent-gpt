package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.utils.CustomerExcelUtils;
import com.zlt.aps.common.utils.ProductionPlanTemplateExcelUtils;
import com.zlt.aps.monthplan.api.domain.dto.ChangeSpecCodeMouldingDayResultParam;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.service.IFactoryConsoleRemoteService;
import com.zlt.aps.monthplan.api.service.IMonthPlanMouldingDayResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanMouldingDayResultUIController.java
 * 描    述：分厂月生产计划排产过程-模具排产结果汇总 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/monthplan/mouldingDayResult")
@Api(tags = "分厂月生产计划排产-需求计划排产版本排产计划结果-->ZLT")
public class MonthPlanMouldingDayResultUIController extends BaseUIController<MonthPlanMouldingDayResult> {

    private final IMonthPlanMouldingDayResultRemoteService iMonthPlanMouldingDayResultService;

    private final IFactoryConsoleRemoteService factoryConsoleRemoteService;
    /**
     * 是否是自然月
     */
    private static final Integer IS_NATURAL_MONTH = 1;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mouldingDayResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MonthPlanMouldingDayResult monthPlanMouldingDayResult) {
        return iMonthPlanMouldingDayResultService.list(monthPlanMouldingDayResult);
    }

    @ResponseBody
    @PostMapping("/statistics")
    @ApiOperation("统计分厂月生产计划排产")
    public AjaxResult statistics(MonthPlanMouldingDayResult monthPlanMouldingDayResult) {
        return iMonthPlanMouldingDayResultService.statistics(monthPlanMouldingDayResult);
    }

    @ResponseBody
    @PostMapping("/statisticsDay")
    @ApiOperation("统计分厂月生产计划日排产规格数及日排产总量")
    public AjaxResult statisticsByDay(@RequestBody MonthPlanMouldingDayResult monthPlanMouldingDayResult) {
        return iMonthPlanMouldingDayResultService.getStatisticsDay(monthPlanMouldingDayResult);
    }

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     */
    @ResponseBody
    @PostMapping("/productionVersionList")
    @ApiOperation("查询对应年月+分厂+需求计划版本的分厂月计划版本")
    public AjaxResult productionVersionList(MonthPlanMouldingDayResult query) {
        return iMonthPlanMouldingDayResultService.productionVersionList(query);
    }

    /**
     * 切换硫化规格代号
     */
    @ResponseBody
    @ApiOperation("切换硫化规格代号")
    @PostMapping("/changeSpecCode")
    @RequiresPermissions("monthPlan:mouldingDayResult:changeSpecCode")
    public AjaxResult changeSpecCode(@RequestBody ChangeSpecCodeMouldingDayResultParam changeParam) {
        if (null == changeParam || null == changeParam.getProductionId() || StringUtils.isBlank(changeParam.getSpecCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.checkChangeSpecCode"));
        }
        return iMonthPlanMouldingDayResultService.changeSpecCode(changeParam);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MonthPlanMouldingDayResult> util = new ExcelUtil<>(MonthPlanMouldingDayResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ResponseBody
    @ApiOperation("下载导入模板")
    @GetMapping({"/importTemplateByMonth"})
    public AjaxResult importTemplateByMonth(HttpServletResponse response, String factoryCode, Integer year, Integer month) throws IOException {
        FactoryProductionParamVo param = new FactoryProductionParamVo();
        param.setFactoryCode(factoryCode);
        param.setYear(year);
        param.setMonth(month);
        FactoryProductionVersion version = factoryConsoleRemoteService.createImportVersion(param);
        if (null == version || IS_NATURAL_MONTH.equals(version.getIsNaturalMonth())) {
            return importTemplate(response);
        }
        String fileName = this.getExportTemplateFileName();
        List<Integer> dayList = ProductionPlanTemplateExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.monthPlanMouldingDayResult.day";
        CustomerExcelUtils<MonthPlanMouldingDayResult> util = new CustomerExcelUtils<>(MonthPlanMouldingDayResult.class, dayList, startWithName, MonthPlanMouldingDayResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MonthPlanMouldingDayResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMonthPlanMouldingDayResultService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMonthPlanMouldingDayResultService.importData(context, false);
        return ajaxResult;
    }

    /**
     * 根据条件查询合并SKU数据
     */
    @ApiOperation("根据条件查询合并SKU数据")
    @RequiresPermissions("monthplan:mouldingDayResult:listFacProduct")
    @PostMapping("/listFacProduct")
    @ResponseBody
    public TableDataInfo listFacProduct(MonthPlanMouldingDayResult monthPlanMouldingDayResult) {
        return iMonthPlanMouldingDayResultService.listFacProduct(monthPlanMouldingDayResult);
    }

    /**
     * 合并SKU数据导出
     *
     * @param response 响应结果
     * @param entity   查询条件
     * @throws IOException 异常
     */
    @RequiresPermissions("monthplan:mouldingDayResult:exportFacProduct")
    @ApiOperation("合并SKU数据导出")
    @GetMapping({"/exportFacProduct"})
    @ResponseBody
    public void exportFacProduct(HttpServletResponse response, MonthPlanMouldingDayResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.summarySku.modelName");
        byte[] excelBytes = iMonthPlanMouldingDayResultService.exportFacProductData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询月计划排产统计
     */
    @ApiOperation("查询月计划排产统计")
    @RequiresPermissions("monthplan:mouldingDayResult:listFacProductStatistics")
    @PostMapping("/listFacProductStatistics")
    @ResponseBody
    public TableDataInfo listFacProductStatistics(MonthPlanMouldingDayResult monthPlanMouldingDayResult) {
        return iMonthPlanMouldingDayResultService.listFacProductStatistics(monthPlanMouldingDayResult);
    }

    /**
     * 导出月计划排产统计
     *
     * @param response 响应结果
     * @param entity   查询条件
     * @throws IOException 异常
     */
    @RequiresPermissions("monthplan:mouldingDayResult:exportFacProductStatisticsData")
    @ApiOperation("导出月计划排产统计")
    @GetMapping({"/exportFacProductStatisticsData"})
    @ResponseBody
    public void exportFacProductStatisticsData(HttpServletResponse response, MonthPlanMouldingDayResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.monthPlanMouldingDayResult.statistics.modelName");
        byte[] excelBytes = iMonthPlanMouldingDayResultService.exportFacProductStatisticsData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

}
