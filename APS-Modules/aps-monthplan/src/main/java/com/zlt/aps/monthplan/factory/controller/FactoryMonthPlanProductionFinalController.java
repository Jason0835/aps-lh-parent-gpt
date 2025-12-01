package com.zlt.aps.monthplan.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.dto.TrialProductionPlanDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanDayProductionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanProductionFinalResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.common.utils.CustomerExcelUtils;
import com.zlt.aps.monthplan.common.utils.ExcelExportUtils;
import com.zlt.aps.monthplan.factory.helper.MonthPlanProductionFinalUtils;
import com.zlt.aps.monthplan.factory.helper.ProductionPlanExcelUtils;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalService;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProdFinalController.java
 * 描    述：分厂月生产计划排产结果-生产计划排产结果 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "分厂月生产计划排产结果-按SKU")
@RequestMapping("/factoryMonthPlanFinal")
public class FactoryMonthPlanProductionFinalController extends AbstractDocBizController<MonthPlanProductionFinalResult> {

    private final IExportLogService iExportLogService;

    private final IImportLogService iImportLogService;

    private final IImportErrorLogService iImportErrorLogService;

    private final IFactoryMonthPlanProductionFinalService monthPlanProductionFinalService;

    private final IFactoryProductionVersionService factoryProductionVersionService;
    /**
     * 查询分厂月生产计划排产结果-生产计划排产结果列表
     * 带有分页信息
     */
    @PostMapping("/list")
    @ApiOperation("根据查询条件分页查询列表")
    public TableDataInfo list(@RequestBody MonthPlanProductionFinalResult queryVO) {
        startPage("create_time desc");
        QueryWrapper<MonthPlanProductionFinalResult> queryWrapper = new QueryWrapper<>();
        //构建查询条件
        MonthPlanProductionFinalUtils.builderCondition(queryWrapper, queryVO);
        List<MonthPlanProductionFinalResult> dataList = monthPlanProductionFinalService.getList(queryWrapper, true);
        TableDataInfo tableInfo =  getDataTable(dataList);
        List<MonthPlanProductionFinalResult>  records =   (List<MonthPlanProductionFinalResult>)tableInfo.getRows();
        if(CollectionUtils.isEmpty(records)) {
            return tableInfo;
        }
        List<MonthPlanProductionFinalResultVo> resultDataList = MonthPlanProductionFinalUtils.buildData(records);
        tableInfo.setRows(resultDataList);
        return tableInfo;
    }

    @ApiOperation("统计分厂月生产计划排产结果-排产结果列表")
    @PostMapping("/statistics")
    public AjaxResult statistics(@RequestBody MonthPlanProductionFinalResult prodFinal) {
        MonthPlanStatisticsVo result = monthPlanProductionFinalService.statistics(prodFinal);
        return AjaxResult.success(result);
    }

    /**
     * 统计分厂月生产计划日排产规格数及日排产总量
     */
    @ApiOperation("统计分厂月生产计划日排产规格数及日排产总量")
    @PostMapping("/statisticsDay")
    public AjaxResult statisticsByDay(@RequestBody MonthPlanProductionFinalResult query) {
        if (null == query) {
            return AjaxResult.success(Collections.emptyList());
        }
        return AjaxResult.success(monthPlanProductionFinalService.statisticsDay(query));
    }

    /**
     * 根据查询条件，获取对应的月计划定稿数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @PostMapping("/getProdResult")
    @ApiOperation("根据查询条件，获取对应的月计划定稿数据")
    public List<FactoryMonthPlanProdFinalVo> getProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        return monthPlanProductionFinalService.getProdResult(queryCondition);
    }

    /**
     * 根据查询条件，获取日对应的月计划定稿数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @PostMapping("/getMonthPlanProdResult")
    @ApiOperation("根据查询条件，获取日对应的月计划定稿数据")
    public List<FactoryMonthPlanProdFinalVo> getMonthPlanProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        return monthPlanProductionFinalService.getMonthPlanProdResult(queryCondition);
    }

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @ApiOperation("根据查询条件，获取某日的月计划排产数据")
    @PostMapping("/getDayProductionInfo")
    public List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanProductionInfo(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        if (null == queryCondition || StringUtils.isBlank(queryCondition.getFactoryCode()) || null == queryCondition.getProductionDate()) {
            return Collections.emptyList();
        }
        return monthPlanProductionFinalService.getMonthPlanDayProductionInfo(queryCondition);
    }

    /**
     * 导入调整计划
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @PostMapping("/importData")
    @ApiOperation("导入分厂月生产计划排产最终结果数据-即定稿后的数据，包含调整")
    @Log(title = "ui.data.adjust.monthPlan.modelName", businessType = BusinessType.IMPORT)
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MonthPlanProductionFinalResultVo> util = new ExcelUtil<>(MonthPlanProductionFinalResultVo.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<MonthPlanProductionFinalResultVo> list = util.importExcel(is);
        AjaxResult ajaxResult = monthPlanProductionFinalService.importAdjustPlan(list, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        if (null == ajaxResult) {
            throw new CustomException("接口数据返回空，后台服务没有开启");
        } else {
            Object msg = ajaxResult.get("msg");
            if (null == msg) {
                throw new CustomException("接口数据返回空，后台服务没有开启");
            } else {
                String[] message = ajaxResult.get("msg").toString().split(",");
                switch (message.length) {
                    case 2:
                        importLog.setSuccessNum(Long.valueOf(message[1]));
                        importLog.setFailNum(0L);
                        ajaxResult.put("msg", com.ruoyi.common.utils.StringUtils.format(message[0], message[1]));
                        break;
                    case 4:
                        importLog.setSuccessNum(Long.valueOf(message[1]));
                        importLog.setFailNum(Long.valueOf(message[2]));
                        ajaxResult.put("msg", com.ruoyi.common.utils.StringUtils.format(message[0], message[1], message[2], message[3]));
                    default:
                }
                iImportLogService.edit(importLog);
            }
        }
//        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        ajaxResult.put(AjaxResult.DATA_TAG, importLog.getId());
        return ajaxResult;
    }

    /**
     * 导出导入错误日志列表
     */
    @Log(title = "导入错误日志", businessType = BusinessType.EXPORT)
    @ApiOperation("导出导入错误日志")
    @PostMapping("/exportImportErrorLog/{fileName}")
    public byte[] exportImportErrorLog(@RequestBody ImportErrorLog queryVO, @PathVariable("fileName") String fileName,
                                       HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<ImportErrorLog> list = iImportErrorLogService.getList(queryVO);
        ExcelUtil<ImportErrorLog> util = new ExcelUtil<>(ImportErrorLog.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 导入试制量试计划
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @PostMapping("/importTrialProductionPlan")
    @ApiOperation("导入试制量试计划")
    @Log(title = "ui.data.column.monthPlanProductionFinalResult.trialProductionPlan", businessType = BusinessType.IMPORT)
    public AjaxResult importTrialProductionPlan(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<TrialProductionPlanDto> util = new ExcelUtil(TrialProductionPlanDto.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<TrialProductionPlanDto> list = util.importExcel(is);
        AjaxResult ajaxResult = monthPlanProductionFinalService.importTrialProductionPlan(list, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 导出列表
     */
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导出分厂月生产计划排产最终结果数据-即定稿后的数据，包含调整-SKU")
    @Log(title = "分厂月生产计划排产结果-生产计划排产结果-SKU", businessType = BusinessType.EXPORT)
    public byte[] exportData(@RequestBody MonthPlanProductionFinalResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        FactoryProductionVersion version = factoryProductionVersionService.getFinalVersionByYearMonth(queryVO.getFactoryCode(), queryVO.getYear(), queryVO.getMonth());
        ExportLog exportLog = new ExportLog();
        List<MonthPlanProductionFinalResult> dataList = getData(queryVO, false);
        if(CollectionUtils.isEmpty(dataList)){
            ExcelUtil<MonthPlanProductionFinalResultVo> util = new ExcelUtil(MonthPlanProductionFinalResultVo.class);
            byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, dataList, fileName, queryVO, exportLog, "0");
            this.iExportLogService.add(exportLog);
            return resultBytes;
        }
        List<MonthPlanProductionFinalResultVo> list = MonthPlanProductionFinalUtils.buildData(dataList);
        if (null == version || YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            ExcelUtil<MonthPlanProductionFinalResultVo> util = new ExcelUtil(MonthPlanProductionFinalResultVo.class);
            byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
            this.iExportLogService.add(exportLog);
            return resultBytes;
        }
        List<Integer> dayList = ProductionPlanExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.monthPlanProductionFinalResult.day";
        CustomerExcelUtils<MonthPlanProductionFinalResultVo> util = new CustomerExcelUtils<>(MonthPlanProductionFinalResultVo.class, dayList, startWithName, MonthPlanProductionFinalResultVo.class);
        byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryVO, exportLog, "0");
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 下载试制量试导入模板
     */
    @PostMapping("/importTemplate/{fileName}")
    @ApiOperation("下载分厂月生产计分试制量试导入模板")
    @Log(title = "下载分厂月生产计分试制量试导入模板", businessType = BusinessType.EXPORT)
    public byte[] importTemplate(@RequestBody MonthPlanProductionFinalResult queryCondition, @PathVariable("fileName") String fileName,
                                 HttpServletResponse response) throws IOException {
        FactoryProductionVersion version = factoryProductionVersionService.getFinalVersionByYearMonth(queryCondition.getFactoryCode(), queryCondition.getYear(), queryCondition.getMonth());
        ExportLog exportLog = new ExportLog();
        //空数据
        List<TrialProductionPlanDto> list = new ArrayList<>();
        if (null == version || YesOrNoEnum.YES.getValue().equals(version.getIsNaturalMonth())) {
            ExcelUtil<TrialProductionPlanDto> util = new ExcelUtil(TrialProductionPlanDto.class);
            byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryCondition, exportLog, "0");
            this.iExportLogService.add(exportLog);
            return resultBytes;
        }
        List<Integer> dayList = ProductionPlanExcelUtils.getCycleDayList(version);
        String startWithName = "ui.data.column.monthPlanProductionFinalResult.day";
        CustomerExcelUtils<TrialProductionPlanDto> util = new CustomerExcelUtils<>(TrialProductionPlanDto.class, dayList, startWithName, TrialProductionPlanDto.class);
        byte[] resultBytes = ExcelExportUtils.fillExcelAndLog(response, util, list, fileName, queryCondition, exportLog, "0");
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    @ApiOperation("输入SAP代码后自动关联出字段")
    @PostMapping("/linkProductInfoByProductCode")
    public AjaxResult linkProductInfoByProductCode(@RequestBody MonthPlanProductionFinalResult param) {
        return AjaxResult.success(monthPlanProductionFinalService.linkProductInfoByProductCode(param));
    }

    @ApiOperation("输入订单数量后系统自动计算")
    @PostMapping("/calculateByOrderQty")
    public AjaxResult calculateByOrderQty(@RequestBody MonthPlanProductionFinalResult param) {
        return AjaxResult.success(monthPlanProductionFinalService.calculateByOrderQty(param));
    }

    @ApiOperation("新增规格")
    @PostMapping("/addSpecifications")
    public AjaxResult addSpecifications(@RequestBody MonthPlanProductionFinalResult param) {
        monthPlanProductionFinalService.addSpecifications(param);
        return AjaxResult.success();
    }

    @ApiOperation("编辑计划")
    @PostMapping("/editPlan")
    public AjaxResult editPlan(@RequestBody MonthPlanProductionFinalResult param) {
        monthPlanProductionFinalService.editPlan(param);
        return AjaxResult.success();
    }

    @ApiOperation("规格减量")
    @PostMapping("/subtractSpecification")
    public AjaxResult subtractSpecification(@RequestBody MonthPlanProductionFinalResult param) {
        monthPlanProductionFinalService.subtractSpecification(param);
        return AjaxResult.success();
    }



    /**
     * 根据查询条件，获取查询数据-带有分页处理
     *
     * @param queryVO 查询条件
     * @return
     */
    private List<MonthPlanProductionFinalResult> getData(MonthPlanProductionFinalResult queryVO, boolean isPage) {
        QueryWrapper<MonthPlanProductionFinalResult> queryWrapper = new QueryWrapper<>();
        //构建查询条件
        MonthPlanProductionFinalUtils.builderCondition(queryWrapper, queryVO);
        //分页
        PageUtils.startPage(isPage, MonthPlanProductionFinalUtils.getOrderBy(queryVO));
        List<MonthPlanProductionFinalResult> dataList = monthPlanProductionFinalService.getList(queryWrapper, true);
        if (isPage) {
            clearPage();
        }
        //数据字典转换或是基础数据转换
        try {
            QueryFormulaUtil.execFormula(dataList, getQueryFormulas());
        } catch (QueryExprException var6) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return dataList;
    }

    @Override
    protected IDocService getDocService() {
        return null;
    }

    @Override
    protected String getTypeCode() {
        return "";
    }
}
