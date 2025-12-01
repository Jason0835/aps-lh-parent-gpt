package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.enums.BrandProSizeSummaryTypeEnum;
import com.zlt.aps.monthplan.api.domain.dto.*;
import com.zlt.aps.monthplan.api.domain.vo.MonthFinishRateVo;
import com.zlt.aps.monthplan.api.domain.vo.SkuMonthQtyVo;
import com.zlt.aps.monthplan.api.service.IMonthPlanReportRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Chen
 */
@Slf4j
@Controller
@RequestMapping("/monthPlan/report")
@Api(tags = "月计划报表管理")
public class MonthPlanReportUIController {

    @Autowired
    private IMonthPlanReportRemoteService monthPlanReportService;

    /**
     * 根据条件查询T月完成率数据
     */
    @RequiresPermissions("report:monthFinishRate:list")
    @ApiOperation("T月完成率-根据条件查询数据")
    @PostMapping("/listMonthFinishRate")
    @ResponseBody
    public MonthFinishRateVo listMonthFinishRate(MonthPlanReportDto queryDto) {
        return monthPlanReportService.listMonthFinishRate(queryDto);
    }

    @RequiresPermissions("report:monthFinishRate:export")
    @ApiOperation("T月完成率-数据导出")
    @GetMapping({"/exportMonthFinishRate"})
    @ResponseBody
    public void exportMonthFinishRate(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tMonthFinishRate.modelName");
        byte[] excelBytes = monthPlanReportService.exportMonthFinishRate(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 根据条件查询T月完成率-品牌数据
     */
    @RequiresPermissions("report:monthFinishRateBrand:list")
    @ApiOperation("T月完成率-品牌-根据条件查询数据")
    @PostMapping("/listMonthFinishRateBrand")
    @ResponseBody
    public TableDataInfo listMonthFinishRateBrand(MonthPlanReportDto queryDto) {
        return monthPlanReportService.listMonthFinishRateBrand(queryDto);
    }

    /**
     * 根据条件导出T月完成率-品牌数据
     */
    @RequiresPermissions("report:monthFinishRateBrand:export")
    @ApiOperation("T月完成率-品牌-数据导出")
    @GetMapping({"/exportMonthFinishRateBrand"})
    @ResponseBody
    public void exportMonthFinishRateBrand(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tMonthFinishRateBrand.modelName");
        byte[] excelBytes = monthPlanReportService.exportMonthFinishRateBrand(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 根据条件查询T月完成率-品牌寸别数据
     */
    @RequiresPermissions("report:monthFinishRateBrandProSize:list")
    @ApiOperation("T月完成率-品牌寸别-根据条件查询数据")
    @PostMapping("/listMonthFinishRateBrandProSize")
    @ResponseBody
    public TableDataInfo listMonthFinishRateBrandProSize(MonthPlanReportDto queryDto) {
        return monthPlanReportService.listMonthFinishRateBrandProSize(queryDto);
    }

    /**
     * 根据条件导出T月完成率-品牌寸别数据
     */
    @RequiresPermissions("report:monthFinishRateBrandProSize:export")
    @ApiOperation("T月完成率-品牌寸别-数据导出")
    @GetMapping({"/exportMonthFinishRateBrandProSize"})
    @ResponseBody
    public void exportMonthFinishRateBrandProSize(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tMonthFinishRateBrandProSize.modelName");
        byte[] excelBytes = monthPlanReportService.exportMonthFinishRateBrandProSize(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询SKU汇总分析
     */
    @RequiresPermissions("report:skuSummary:list")
    @ApiOperation("sku汇总分析")
    @PostMapping("/listSkuSummary")
    @ResponseBody
    public SkuMonthQtyVo listSkuSummary(MonthPlanReportDto queryDto) {
        return monthPlanReportService.listSkuSummary(queryDto);
    }

    /**
     * 导出sku汇总分析
     */
    @RequiresPermissions("report:skuSummary:export")
    @ApiOperation("导出sku汇总分析")
    @GetMapping({"/exportSkuSummary"})
    @ResponseBody
    public void exportSkuSummary(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.skuSummary.modelName");
        byte[] excelBytes = monthPlanReportService.exportSkuSummary(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @RequiresPermissions("report:skuSummaryProduce:list")
    @ApiOperation("投产sku汇总分析")
    @PostMapping("/listSkuSummaryProduce")
    @ResponseBody
    public TableDataInfo listSkuSummaryProduce(MonthPlanReportDto queryDto) {
        return monthPlanReportService.listSkuSummaryProduce(queryDto);
    }

    /**
     * 导出投产sku汇总分析
     *
     * @param entity 查询参数
     */
    @RequiresPermissions("report:skuSummaryProduce:export")
    @ApiOperation("导出投产sku汇总分析")
    @GetMapping({"/exportSkuSummaryProduce"})
    @ResponseBody
    public void exportSkuSummaryProduce(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.skuSummaryProduce.modelName");
        byte[] excelBytes = monthPlanReportService.exportSkuSummaryProduce(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @RequiresPermissions("report:skuSummaryTrial:list")
    @ApiOperation("试制sku汇总分析")
    @PostMapping("/listSkuSummaryTrial")
    @ResponseBody
    public TableDataInfo listSkuSummaryTrial(MonthPlanReportDto queryDto) {
        return monthPlanReportService.listSkuSummaryTrial(queryDto);
    }

    /**
     * 导出试制sku汇总分析
     *
     * @param entity 查询参数
     */
    @RequiresPermissions("report:skuSummaryTrial:export")
    @ApiOperation("导出试制sku汇总分析")
    @GetMapping({"/exportSkuSummaryTrial"})
    @ResponseBody
    public void exportSkuSummaryTrial(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.skuSummaryTrial.modelName");
        byte[] excelBytes = monthPlanReportService.exportSkuSummaryTrial(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询品牌-尺寸汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @RequiresPermissions("report:brandSummary:list")
    @ApiOperation("查询品牌汇总分析")
    @PostMapping("/listBrandSummary")
    @ResponseBody
    public TableDataInfo listBrandSummary(MonthPlanReportDto queryDto) {
        queryDto.setBrandProSizeSummaryType(BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode());
        return monthPlanReportService.listBrandProSizeSummary(queryDto);
    }

    /**
     * 导出品牌-尺寸汇总分析
     * @param entity 查询条件
     */
    @RequiresPermissions("report:brandSummary:export")
    @ApiOperation("导出品牌汇总分析")
    @GetMapping({"/exportBrandSummary"})
    @ResponseBody
    public void exportBrandSummary(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        entity.setBrandProSizeSummaryType(BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode());
        String fileName = I18nUtil.getMessage("ui.data.column.brandSummary.modelName");
        byte[] excelBytes = monthPlanReportService.exportBrandProSizeSummary(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询品牌-尺寸汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @RequiresPermissions("report:proSizeSummary:list")
    @ApiOperation("查询寸别汇总分析")
    @PostMapping("/listProSizeSummary")
    @ResponseBody
    public TableDataInfo listProSizeSummary(MonthPlanReportDto queryDto) {
        queryDto.setBrandProSizeSummaryType(BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode());
        return monthPlanReportService.listBrandProSizeSummary(queryDto);
    }

    /**
     * 导出品牌-尺寸汇总分析
     * @param entity 查询条件
     */
    @RequiresPermissions("report:proSizeSummary:export")
    @ApiOperation("导出寸别汇总分析")
    @GetMapping({"/exportProSizeSummary"})
    @ResponseBody
    public void exportProSizeSummary(HttpServletResponse response, MonthPlanReportDto entity) throws IOException {
        entity.setBrandProSizeSummaryType(BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode());
        String fileName = I18nUtil.getMessage("ui.data.column.proSizeSummary.modelName");
        byte[] excelBytes = monthPlanReportService.exportBrandProSizeSummary(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:channelClassification:list")
    @ApiOperation("查询渠道分类缺口差异")
    @PostMapping("/listChannelClassification")
    @ResponseBody
    public TableDataInfo listChannelClassification(ClassificationReportDto queryDto) {
        return monthPlanReportService.listChannelClassification(queryDto);
    }

    /**
     * 导出渠道分类缺口差异
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:channelClassification:export")
    @ApiOperation("导出渠道分类缺口差异")
    @GetMapping({"/exportChannelClassification"})
    @ResponseBody
    public void exportChannelClassification(HttpServletResponse response, ClassificationReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.channelClassification.modelName");
        byte[] excelBytes = monthPlanReportService.exportChannelClassification(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:brandClassification:list")
    @ApiOperation("查询品牌分类缺口差异")
    @PostMapping("/listBrandClassification")
    @ResponseBody
    public TableDataInfo listBrandClassification(ClassificationReportDto queryDto) {
        return monthPlanReportService.listBrandClassification(queryDto);
    }

    /**
     * 导出品牌分类缺口差异
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:brandClassification:export")
    @ApiOperation("导出品牌分类缺口差异")
    @GetMapping({"/exportBrandClassification"})
    @ResponseBody
    public void exportBrandClassification(HttpServletResponse response, ClassificationReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.brandClassification.modelName");
        byte[] excelBytes = monthPlanReportService.exportBrandClassification(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:proSizeClassification:list")
    @ApiOperation("查询寸别分类缺口差异")
    @PostMapping("/listProSizeClassification")
    @ResponseBody
    public TableDataInfo listProSizeClassification(ClassificationReportDto queryDto) {
        return monthPlanReportService.listProSizeClassification(queryDto);
    }

    /**
     * 导出寸别分类缺口差异
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:proSizeClassification:export")
    @ApiOperation("导出寸别分类缺口差异")
    @GetMapping({"/exportProSizeClassification"})
    @ResponseBody
    public void exportProSizeClassification(HttpServletResponse response, ClassificationReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.proSizeClassification.modelName");
        byte[] excelBytes = monthPlanReportService.exportProSizeClassification(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:brandLocationClassification:list")
    @ApiOperation("查询品牌库位分类缺口差异")
    @PostMapping("/listBrandLocationClassification")
    @ResponseBody
    public TableDataInfo listBrandLocationClassification(ClassificationReportDto queryDto) {
        return monthPlanReportService.listBrandLocationClassification(queryDto);
    }

    /**
     * 导出品牌库位分类缺口差异
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:brandLocationClassification:export")
    @ApiOperation("导出品牌库位分类缺口差异")
    @GetMapping({"/exportBrandLocationClassification"})
    @ResponseBody
    public void exportBrandLocationClassification(HttpServletResponse response, ClassificationReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.brandLocationClassification.modelName");
        byte[] excelBytes = monthPlanReportService.exportBrandLocationClassification(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:proSizeChannelClassification:list")
    @ApiOperation("查询寸别渠道分类缺口差异")
    @PostMapping("/listProSizeChannelClassification")
    @ResponseBody
    public TableDataInfo listProSizeChannelClassification(ClassificationReportDto queryDto) {
        return monthPlanReportService.listProSizeChannelClassification(queryDto);
    }

    /**
     * 导出寸别渠道分类缺口差异
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:proSizeChannelClassification:export")
    @ApiOperation("导出寸别渠道分类缺口差异")
    @GetMapping({"/exportProSizeChannelClassification"})
    @ResponseBody
    public void exportProSizeChannelClassification(HttpServletResponse response, ClassificationReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.proSizeChannelClassification.modelName");
        byte[] excelBytes = monthPlanReportService.exportProSizeChannelClassification(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询胎类区分及缺口汇总
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:tireTypeSatisfyRate:list")
    @ApiOperation("查询胎类区分及缺口汇总")
    @PostMapping("/listTireTypeSatisfyRateList")
    @ResponseBody
    public TableDataInfo listTireTypeSatisfyRateList(ClassificationReportDto queryDto) {
        return monthPlanReportService.listTireTypeSatisfyRateList(queryDto);
    }

    /**
     * 导出胎类区分及缺口汇总-排产受限影响满足率
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:tireTypeSatisfyRate:export")
    @ApiOperation("导出胎类区分及缺口汇总-排产受限影响满足率")
    @GetMapping({"/exportTireTypeSatisfyRate"})
    @ResponseBody
    public void exportTireTypeSatisfyRate(HttpServletResponse response, ClassificationReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.tireTypeSatisfyRate");
        byte[] excelBytes = monthPlanReportService.exportTireTypeSatisfyRate(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询胎类区分及缺口-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:monthTireType:list")
    @ApiOperation("查询胎类区分及缺口-月份综合")
    @PostMapping("/listMonthTireTypeList")
    @ResponseBody
    public TableDataInfo listMonthTireTypeList(TireTypeReportDto queryDto) {
        return monthPlanReportService.listMonthTireTypeList(queryDto);
    }

    /**
     * 导出胎类区分及缺口汇总-月份综合
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:monthTireType:export")
    @ApiOperation("导出胎类区分及缺口汇总-月份综合")
    @GetMapping({"/exportMonthTireType"})
    @ResponseBody
    public void exportMonthTireType(HttpServletResponse response, TireTypeReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.monthTireType");
        byte[] excelBytes = monthPlanReportService.exportMonthTireType(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询生产销售计划数据
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:produceSalePlan:list")
    @ApiOperation("查询生产销售计划数据")
    @PostMapping("/listProduceSalePlanList")
    @ResponseBody
    public TableDataInfo listProduceSalePlanList(BaseReportDto queryDto) {
        return monthPlanReportService.listProduceSalePlanList(queryDto);
    }

    /**
     * 导出生产销售计划数据
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:produceSalePlan:export")
    @ApiOperation("导出生产销售计划数据")
    @GetMapping({"/exportProduceSalePlan"})
    @ResponseBody
    public void exportProduceSalePlan(HttpServletResponse response, BaseReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.produceSalePlan");
        byte[] excelBytes = monthPlanReportService.exportProduceSalePlan(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 查询排产版本列表数据
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @RequiresPermissions("report:produceVersion:list")
    @ApiOperation("查询排产版本列表数据")
    @PostMapping("/listProduceVersionList")
    @ResponseBody
    public TableDataInfo listProduceVersionList(ProduceVersionDto queryDto) {
        return monthPlanReportService.listProduceVersionList(queryDto);
    }

    /**
     * 导出排产版本列表数据
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:produceVersion:export")
    @ApiOperation("导出排产版本列表数据")
    @GetMapping({"/exportProduceVersion"})
    @ResponseBody
    public void exportProduceVersion(HttpServletResponse response, ProduceVersionDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.produceVersion");
        byte[] excelBytes = monthPlanReportService.exportProduceVersion(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping("/homePage4Plan")
    @ApiOperation("首页-计划")
    @ResponseBody
    public AjaxResult homePage4Plan() {
        return monthPlanReportService.homePage4Plan();
    }

    @PostMapping("/homePage4Order")
    @ApiOperation("首页-订单接单情况")
    @ResponseBody
    public AjaxResult homePage4Order() {
        return monthPlanReportService.homePage4Order();
    }

    @PostMapping("/homePage4ProductionProcesses")
    @ApiOperation("首页-工序完成情况")
    @ResponseBody
    public AjaxResult homePage4ProductionProcesses() {
        return monthPlanReportService.homePage4ProductionProcesses();
    }

    @PostMapping("/homePage4Machine")
    @ApiOperation("首页-工厂设备情况")
    @ResponseBody
    public AjaxResult homePage4Machine() {
        return monthPlanReportService.homePage4Machine();
    }

    @PostMapping("/homePage4YearProduct")
    @ApiOperation("首页-今年生产情况")
    @ResponseBody
    public AjaxResult homePage4YearProduct() {
        return monthPlanReportService.homePage4YearProduct();
    }

    @PostMapping("/selectProductionProcessesByDate7")
    @ApiOperation("工序完成情况-7天")
    @ResponseBody
    public AjaxResult selectProductionProcessesByDate7() {
        return monthPlanReportService.selectProductionProcessesByDate7();
    }

    @PostMapping("/selectCxLhMachineRepair")
    @ApiOperation("成型硫化机台维修情况")
    @ResponseBody
    public AjaxResult selectCxLhMachineRepair() {
        return monthPlanReportService.selectCxLhMachineRepair();
    }

    /**
     * 查询sku汇总分析-大屏
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @PostMapping("/selectSkuSummary4BigScreen")
    @ApiOperation("查询sku汇总分析-大屏")
    @ResponseBody
    public AjaxResult selectSkuSummary4BigScreen(MonthPlanReportDto queryDto) {
        return monthPlanReportService.selectSkuSummary4BigScreen(queryDto);
    }

    /**
     * 查询系统运行报表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @RequiresPermissions("report:systemRun:list")
    @PostMapping("/selectSystemRunReport")
    @ApiOperation("查询系统运行报表")
    @ResponseBody
    public AjaxResult selectSystemRunReport(SystemRunReportDto queryDto) {
        return monthPlanReportService.selectSystemRunReport(queryDto);
    }

    /**
     * 导出系统运行报表
     *
     * @param entity 查询条件
     */
    @RequiresPermissions("report:systemRun:export")
    @ApiOperation("导出系统运行报表")
    @GetMapping({"/exportSystemRunReport"})
    @ResponseBody
    public void exportSystemRunReport(HttpServletResponse response, SystemRunReportDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.systemRunReport");
        byte[] excelBytes = monthPlanReportService.exportSystemRunReport(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}
