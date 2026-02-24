package com.zlt.aps.mp.report.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.monthplan.api.domain.dto.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.mp.report.service.IMonthPlanReportService;
import com.zlt.common.utils.ExcelReadUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author Chen
 */
@Slf4j
@Api(tags = "月计划报表")
@RestController
@RequestMapping("/monthPlan/report")
public class MonthPlanReportController extends BaseController {

    @Autowired
    private IMonthPlanReportService monthPlanReportService;

    /**
     * 查询T月完成率列表
     *
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询T月完成率列表")
    @PostMapping("/listMonthFinishRate")
    public MonthFinishRateVo listMonthFinishRate(@RequestBody MonthPlanReportDto queryDto) {
        return monthPlanReportService.listMonthFinishRate(queryDto);
    }

    /**
     * 导出T月完成率数据为Excel文件
     *
     * @param entity   包含月份计划报告信息的DTO，用于查询完成率数据
     * @param fileName 要导出的Excel文件名称
     * @return 导出的Excel文件的字节数组
     * @throws IOException 如果文件导出过程中发生I/O错误
     */
    @ApiOperation("T月完成率数据导出")
    @PostMapping({"/exportMonthFinishRate"})
    public byte[] exportMonthFinishRate(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportMonthFinishRate(entity);
    }

    /**
     * 查询T月完成率-品牌列表
     *
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询T月完成率-品牌列表")
    @PostMapping("/listMonthFinishRateBrand")
    public TableDataInfo listMonthFinishRateBrand(@RequestBody MonthPlanReportDto queryDto) {
        this.startPage();
        List<MonthFinishRateBrandVo> brandVoList = monthPlanReportService.listMonthFinishRateBrand(queryDto);
        return getDataTable(brandVoList);
    }

    /**
     * 导出T月完成率数据为Excel文件
     *
     * @param entity   包含月份计划报告信息的DTO，用于查询完成率数据
     * @param fileName 要导出的Excel文件名称
     * @return 导出的Excel文件的字节数组
     * @throws IOException 如果文件导出过程中发生I/O错误
     */
    @ApiOperation("T月完成率-品牌数据导出")
    @PostMapping({"/exportMonthFinishRateBrand"})
    public byte[] exportMonthFinishRateBrand(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportMonthFinishRateBrand(entity);
    }

    /**
     * 查询T月完成率-品牌列表
     *
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询T月完成率-品牌寸别列表")
    @PostMapping("/listMonthFinishRateBrandProSize")
    public TableDataInfo listMonthFinishRateBrandProSize(@RequestBody MonthPlanReportDto queryDto) {
        this.startPage();
        List<MonthFinishRateProSizeVo> brandVoList = monthPlanReportService.listMonthFinishRateBrandProSize(queryDto);
        return getDataTable(brandVoList);
    }

    /**
     * 导出T月完成率数据为Excel文件
     *
     * @param entity   包含月份计划报告信息的DTO，用于查询完成率数据
     * @param fileName 要导出的Excel文件名称
     * @return 导出的Excel文件的字节数组
     * @throws IOException 如果文件导出过程中发生I/O错误
     */
    @ApiOperation("T月完成率-品牌寸别数据导出")
    @PostMapping({"/exportMonthFinishRateBrandProSize"})
    public byte[] exportMonthFinishRateBrandProSize(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportMonthFinishRateBrandProSize(entity);
    }

    /**
     * 查询sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询sku汇总分析")
    @PostMapping("/listSkuSummary")
    public SkuMonthQtyVo listSkuSummary(@RequestBody MonthPlanReportDto queryDto) {
        return monthPlanReportService.listSkuSummary(queryDto);
    }

    /**
     * 导出sku汇总分析
     *
     * @param entity   查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出sku汇总分析")
    @PostMapping({"/exportSkuSummary"})
    public byte[] exportSkuSummary(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportSkuSummary(entity);
    }

    /**
     * 查询投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询投产sku汇总分析")
    @PostMapping("/listSkuSummaryProduce")
    public TableDataInfo listSkuSummaryProduce(@RequestBody MonthPlanReportDto queryDto) {
        List<SkuSummaryProduceVo> voList = monthPlanReportService.listSkuSummaryProduce(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出投产sku汇总分析
     *
     * @param entity   查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出投产sku汇总分析")
    @PostMapping({"/exportSkuSummaryProduce"})
    public byte[] exportSkuSummaryProduce(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportSkuSummaryProduce(entity);
    }

    /**
     * 查询试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询试制sku汇总分析")
    @PostMapping("/listSkuSummaryTrial")
    public TableDataInfo listSkuSummaryTrial(@RequestBody MonthPlanReportDto queryDto) {
        List<SkuSummaryTrialVo> voList = monthPlanReportService.listSkuSummaryTrial(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出试制sku汇总分析
     *
     * @param entity   查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出试制sku汇总分析")
    @PostMapping({"/exportSkuSummaryTrial"})
    public byte[] exportSkuSummaryTrial(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportSkuSummaryTrial(entity);
    }

    /**
     * 查询品牌-尺寸汇总分析
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询品牌-尺寸汇总分析")
    @PostMapping("/listBrandProSizeSummary")
    public TableDataInfo listBrandProSizeSummary(@RequestBody MonthPlanReportDto queryDto) {
        List<BrandProSizeSummaryVo> voList = monthPlanReportService.listBrandProSizeSummary(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出品牌-尺寸汇总分析
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出试制sku汇总分析")
    @PostMapping({"/exportBrandProSizeSummary"})
    public byte[] exportBrandProSizeSummary(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportBrandProSizeSummary(entity);
    }

    /**
     * 查询渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询渠道分类缺口差异")
    @PostMapping("/listChannelClassification")
    public TableDataInfo listChannelClassification(@RequestBody ClassificationReportDto queryDto) {
        List<ReportClassificationVo> voList = monthPlanReportService.listChannelClassification(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出渠道分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出渠道分类缺口差异")
    @PostMapping({"/exportChannelClassification"})
    public byte[] exportChannelClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportChannelClassification(entity);
    }

    /**
     * 查询品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询品牌分类缺口差异")
    @PostMapping("/listBrandClassification")
    public TableDataInfo listBrandClassification(@RequestBody ClassificationReportDto queryDto) {
        List<ReportClassificationVo> voList = monthPlanReportService.listBrandClassification(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出品牌分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出品牌分类缺口差异")
    @PostMapping({"/exportBrandClassification"})
    public byte[] exportBrandClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportBrandClassification(entity);
    }

    /**
     * 查询寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询寸别分类缺口差异")
    @PostMapping("/listProSizeClassification")
    public TableDataInfo listProSizeClassification(@RequestBody ClassificationReportDto queryDto) {
        List<ReportClassificationVo> voList = monthPlanReportService.listProSizeClassification(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出寸别分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出寸别分类缺口差异")
    @PostMapping({"/exportProSizeClassification"})
    public byte[] exportProSizeClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportProSizeClassification(entity);
    }

    /**
     * 查询品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询品牌库位分类缺口差异")
    @PostMapping("/listBrandLocationClassification")
    public TableDataInfo listBrandLocationClassification(@RequestBody ClassificationReportDto queryDto) {
        List<ReportClassificationVo> voList = monthPlanReportService.listBrandLocationClassification(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出品牌库位分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出品牌库位分类缺口差异")
    @PostMapping({"/exportBrandLocationClassification"})
    public byte[] exportBrandLocationClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportBrandLocationClassification(entity);
    }

    /**
     * 查询寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询寸别渠道分类缺口差异")
    @PostMapping("/listProSizeChannelClassification")
    public TableDataInfo listProSizeChannelClassification(@RequestBody ClassificationReportDto queryDto) {
        List<ReportClassificationVo> voList = monthPlanReportService.listProSizeChannelClassification(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出寸别渠道分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出寸别渠道分类缺口差异")
    @PostMapping({"/exportProSizeChannelClassification"})
    public byte[] exportProSizeChannelClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportProSizeChannelClassification(entity);
    }

    /**
     * 查询胎类区分及缺口汇总
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询胎类区分及缺口汇总")
    @PostMapping("/listTireTypeSatisfyRateList")
    public TableDataInfo listTireTypeSatisfyRateList(@RequestBody BaseReportDto queryDto) {
        List<TireTypeReportSatisfyRateVo> voList = monthPlanReportService.getReportTireTypeSatisfyRateList(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出胎类区分及缺口汇总-排产受限影响满足率
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出胎类区分及缺口汇总-排产受限影响满足率")
    @PostMapping({"/exportTireTypeSatisfyRate"})
    public byte[] exportTireTypeSatisfyRate(@RequestBody BaseReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportTireTypeSatisfyRate(entity);
    }

    /**
     * 查询胎类区分及缺口-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询胎类区分及缺口-月份综合")
    @PostMapping("/listMonthTireTypeList")
    public TableDataInfo listMonthTireTypeList(@RequestBody TireTypeReportDto queryDto) {
        List<TireTypeClassificationVo> voList = monthPlanReportService.selectMonthTireTypeList(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出胎类区分及缺口汇总-月份综合
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出胎类区分及缺口汇总-月份综合")
    @PostMapping({"/exportMonthTireType"})
    public byte[] exportMonthTireType(@RequestBody TireTypeReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportMonthTireType(entity);
    }


    /**
     * 查询生产销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询生产销售计划数据")
    @PostMapping({"/listProduceSalePlanList"})
    public TableDataInfo listProduceSalePlanList(@RequestBody BaseReportDto queryDto) {
        List<ProduceSalePlanResultVo> voList = monthPlanReportService.selectProduceSalePlanList(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出生产销售计划数据
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出生产销售计划数据")
    @PostMapping({"/exportProduceSalePlan"})
    public byte[] exportProduceSalePlan(@RequestBody BaseReportDto entity, @RequestParam("fileName") String fileName) throws IOException {
        return monthPlanReportService.exportProduceSalePlan(entity);
    }


    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询排产版本列表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询排产版本列表数据")
    @PostMapping({"/listProduceVersionList"})
    public TableDataInfo listProduceVersionList(@RequestBody ProduceVersionDto queryDto) {
        List<ProductVersionReportVo> voList = monthPlanReportService.listProduceVersionList(queryDto);
        return getDataTable(voList);
    }

    /**
     * 导出排产版本列表
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出排产版本列表数据")
    @PostMapping({"/exportProduceVersion"})
    public byte[] exportProduceVersion(@RequestBody ProduceVersionDto entity, @RequestParam("fileName") String fileName,
                                       HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<ProductVersionReportVo> voList = monthPlanReportService.listProduceVersionList(entity);
        ExcelUtil<ProductVersionReportVo> util = new ExcelUtil<>(ProductVersionReportVo.class);
        Workbook workbook = util.exportExcel2(response, voList, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(entity.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(voList.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    @PostMapping("/homePage4Plan")
    @ApiOperation("首页-计划")
    public AjaxResult homePage4Plan() {
        Date date = new Date();
        HomePage4PlanVo vo = monthPlanReportService.homePage4Plan(date);
        return AjaxResult.success(vo);
    }

    @PostMapping("/homePage4Order")
    @ApiOperation("首页-订单接单情况")
    public AjaxResult homePage4Order() {
        Date date = new Date();
        List<HomePage4OrderVo> voList = monthPlanReportService.homePage4Order(date);
        return AjaxResult.success(voList);
    }

    @PostMapping("/homePage4ProductionProcesses")
    @ApiOperation("首页-工序完成情况")
    public AjaxResult homePage4ProductionProcesses() {
        Date date = new Date();
        List<HomePage4ProductProcessesVo> voList = monthPlanReportService.homePage4ProductionProcesses(date);
        return AjaxResult.success(voList);
    }

    @PostMapping("/homePage4Machine")
    @ApiOperation("首页-工厂设备情况")
    public AjaxResult homePage4Machine() {
        Date date = new Date();
        List<HomePage4MachineVo> voList = monthPlanReportService.homePage4Machine(date);
        return AjaxResult.success(voList);
    }

    @PostMapping("/homePage4YearProduct")
    @ApiOperation("首页-今年生产情况")
    public AjaxResult homePage4YearProduct() {
        Date date = new Date();
        List<HomePage4PlanVo> voList = monthPlanReportService.homePage4YearProduct(date);
        return AjaxResult.success(voList);
    }

    @PostMapping("/selectProductionProcessesByDate7")
    @ApiOperation("工序完成情况-7天")
    public AjaxResult selectProductionProcessesByDate7() {
        Date date = new Date();
        List<HomePage4ProductProcessesVo> voList = monthPlanReportService.selectProductionProcessesByDate7(date);
        return AjaxResult.success(voList);
    }

    @PostMapping("/selectCxLhMachineRepair")
    @ApiOperation("成型硫化机台维修情况")
    public AjaxResult selectCxLhMachineRepair() {
        Date date = new Date();
        List<CxLhMachineVo> cxLhMachineVoList = monthPlanReportService.selectCxLhMachine(date);
        return AjaxResult.success(cxLhMachineVoList);
    }

    /**
     * 查询sku汇总分析-大屏
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @PostMapping("/selectSkuSummary4BigScreen")
    @ApiOperation("查询sku汇总分析-大屏")
    public AjaxResult selectSkuSummary4BigScreen(@RequestBody MonthPlanReportDto queryDto) {
        Date date = new Date();
        queryDto.setYear(DateUtils.getYear(date));
        queryDto.setMonth(DateUtils.getMonth(date));
        queryDto.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        return AjaxResult.success(monthPlanReportService.selectSkuSummary4BigScreen(queryDto));
    }

    /**
     * 查询系统运行报表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @PostMapping("/selectSystemRunReport")
    @ApiOperation("查询系统运行报表")
    public AjaxResult selectSystemRunReport(@RequestBody SystemRunReportDto queryDto) {
        return AjaxResult.success(monthPlanReportService.selectSystemRunReport(queryDto));
    }

    /**
     * 导出系统运行报表
     *
     * @param queryDto 查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出系统运行报表")
    @PostMapping({"/exportSystemRunReport"})
    public byte[] exportSystemRunReport(@RequestBody SystemRunReportDto queryDto, @RequestParam("fileName") String fileName,
                                        HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<SystemRunReportVo> voList = monthPlanReportService.selectSystemRunReport(queryDto);
        for (SystemRunReportVo systemRunReportVo : voList) {
            Double finishRate = systemRunReportVo.getFinishRate();
            if (finishRate != null && finishRate != 0) {
                systemRunReportVo.setFinishRate(finishRate * 100);
            } else {
                systemRunReportVo.setFinishRate(0D);
            }
        }
        ExcelUtil<SystemRunReportVo> util = new ExcelUtil<>(SystemRunReportVo.class);
        Workbook workbook = util.exportExcel2(response, voList, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryDto.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(voList.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }
}
