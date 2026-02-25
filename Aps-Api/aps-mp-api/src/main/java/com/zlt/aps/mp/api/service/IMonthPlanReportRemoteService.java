package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.dto.*;
import com.zlt.aps.mp.api.domain.vo.MonthFinishRateVo;
import com.zlt.aps.mp.api.domain.vo.SkuMonthQtyVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "IMonthPlanReportService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMonthPlanReportRemoteService {

    /**
     * 查询列表
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询列表")
    @PostMapping("/monthPlan/report/listMonthFinishRate")
    MonthFinishRateVo listMonthFinishRate(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出T月完成率数据
     * @param entity 查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("T月完成率数据导出")
    @PostMapping({"/monthPlan/report/exportMonthFinishRate"})
    byte[] exportMonthFinishRate(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询T月完成率-品牌列表
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询T月完成率-品牌列表")
    @PostMapping("/monthPlan/report/listMonthFinishRateBrand")
    TableDataInfo listMonthFinishRateBrand(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出T月完成率-品牌数据
     * @param entity 查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("T月完成率-品牌数据导出")
    @PostMapping({"/monthPlan/report/exportMonthFinishRateBrand"})
    byte[] exportMonthFinishRateBrand(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询T月完成率-品牌列表
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @ApiOperation("查询T月完成率-品牌寸别列表")
    @PostMapping("/monthPlan/report/listMonthFinishRateBrandProSize")
    public TableDataInfo listMonthFinishRateBrandProSize(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出T月完成率数据为Excel文件
     *
     * @param entity 包含月份计划报告信息的DTO，用于查询完成率数据
     * @param fileName 要导出的Excel文件名称
     * @return 导出的Excel文件的字节数组
     */
    @ApiOperation("T月完成率-品牌寸别数据导出")
    @PostMapping({"/monthPlan/report/exportMonthFinishRateBrandProSize"})
    public byte[] exportMonthFinishRateBrandProSize(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询sku汇总分析
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询sku汇总分析")
    @PostMapping("/monthPlan/report/listSkuSummary")
    public SkuMonthQtyVo listSkuSummary(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出sku汇总分析
     * @param entity 查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出sku汇总分析")
    @PostMapping({"/monthPlan/report/exportSkuSummary"})
    public byte[] exportSkuSummary(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询投产sku汇总分析")
    @PostMapping("/monthPlan/report/listSkuSummaryProduce")
    public TableDataInfo listSkuSummaryProduce(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出投产sku汇总分析
     *
     * @param entity 查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出投产sku汇总分析")
    @PostMapping({"/monthPlan/report/exportSkuSummaryProduce"})
    public byte[] exportSkuSummaryProduce(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询试制sku汇总分析")
    @PostMapping("/monthPlan/report/listSkuSummaryTrial")
    public TableDataInfo listSkuSummaryTrial(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出试制sku汇总分析
     *
     * @param entity 查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出试制sku汇总分析")
    @PostMapping({"/monthPlan/report/exportSkuSummaryTrial"})
    public byte[] exportSkuSummaryTrial(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询品牌-尺寸汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询品牌-尺寸汇总分析")
    @PostMapping("/monthPlan/report/listBrandProSizeSummary")
    public TableDataInfo listBrandProSizeSummary(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 导出品牌-尺寸汇总分析
     * @param entity 查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出品牌-尺寸汇总分析")
    @PostMapping({"/monthPlan/report/exportBrandProSizeSummary"})
    public byte[] exportBrandProSizeSummary(@RequestBody MonthPlanReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询渠道分类缺口差异")
    @PostMapping("/monthPlan/report/listChannelClassification")
    public TableDataInfo listChannelClassification(@RequestBody ClassificationReportDto queryDto);

    /**
     * 导出渠道分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出渠道分类缺口差异")
    @PostMapping({"/monthPlan/report/exportChannelClassification"})
    public byte[] exportChannelClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询品牌分类缺口差异")
    @PostMapping("/monthPlan/report/listBrandClassification")
    public TableDataInfo listBrandClassification(@RequestBody ClassificationReportDto queryDto);

    /**
     * 导出品牌分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出品牌分类缺口差异")
    @PostMapping({"/monthPlan/report/exportBrandClassification"})
    public byte[] exportBrandClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询寸别分类缺口差异")
    @PostMapping("/monthPlan/report/listProSizeClassification")
    public TableDataInfo listProSizeClassification(@RequestBody ClassificationReportDto queryDto);

    /**
     * 导出寸别分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出寸别分类缺口差异")
    @PostMapping({"/monthPlan/report/exportProSizeClassification"})
    public byte[] exportProSizeClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询品牌库位分类缺口差异")
    @PostMapping("/monthPlan/report/listBrandLocationClassification")
    public TableDataInfo listBrandLocationClassification(@RequestBody ClassificationReportDto queryDto);

    /**
     * 导出品牌库位分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出品牌库位分类缺口差异")
    @PostMapping({"/monthPlan/report/exportBrandLocationClassification"})
    public byte[] exportBrandLocationClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询寸别渠道分类缺口差异")
    @PostMapping("/monthPlan/report/listProSizeChannelClassification")
    public TableDataInfo listProSizeChannelClassification(@RequestBody ClassificationReportDto queryDto);

    /**
     * 导出寸别渠道分类缺口差异
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出寸别渠道分类缺口差异")
    @PostMapping({"/monthPlan/report/exportProSizeChannelClassification"})
    public byte[] exportProSizeChannelClassification(@RequestBody ClassificationReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询胎类区分及缺口汇总
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询胎类区分及缺口汇总")
    @PostMapping("/monthPlan/report/listTireTypeSatisfyRateList")
    public TableDataInfo listTireTypeSatisfyRateList(@RequestBody BaseReportDto queryDto);

    /**
     * 导出胎类区分及缺口汇总-排产受限影响满足率
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出胎类区分及缺口汇总-排产受限影响满足率")
    @PostMapping({"/monthPlan/report/exportTireTypeSatisfyRate"})
    public byte[] exportTireTypeSatisfyRate(@RequestBody BaseReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询胎类区分及缺口-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @ApiOperation("查询胎类区分及缺口-月份综合")
    @PostMapping("/monthPlan/report/listMonthTireTypeList")
    public TableDataInfo listMonthTireTypeList(@RequestBody TireTypeReportDto queryDto);

    /**
     * 导出胎类区分及缺口汇总-月份综合
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出胎类区分及缺口汇总-月份综合")
    @PostMapping({"/monthPlan/report/exportMonthTireType"})
    public byte[] exportMonthTireType(@RequestBody TireTypeReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询生产销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询生产销售计划数据")
    @PostMapping({"/monthPlan/report/listProduceSalePlanList"})
    public TableDataInfo listProduceSalePlanList(@RequestBody BaseReportDto queryDto);

    /**
     * 导出生产销售计划数据
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出生产销售计划数据")
    @PostMapping({"/monthPlan/report/exportProduceSalePlan"})
    public byte[] exportProduceSalePlan(@RequestBody BaseReportDto entity, @RequestParam("fileName") String fileName);

    /**
     * 查询排产版本列表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询排产版本列表数据")
    @PostMapping({"/monthPlan/report/listProduceVersionList"})
    public TableDataInfo listProduceVersionList(@RequestBody ProduceVersionDto queryDto);

    /**
     * 导出排产版本列表
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出排产版本列表数据")
    @PostMapping({"/monthPlan/report/exportProduceVersion"})
    public byte[] exportProduceVersion(@RequestBody ProduceVersionDto entity, @RequestParam("fileName") String fileName);

    /**
     * 首页-计划
     *
     * @return 结果
     */
    @ApiOperation("首页-计划")
    @PostMapping("/monthPlan/report/homePage4Plan")
    public AjaxResult homePage4Plan();

    /**
     * 首页-订单接单情况
     *
     * @return 结果
     */
    @ApiOperation("首页-订单接单情况")
    @PostMapping("/monthPlan/report/homePage4Order")
    public AjaxResult homePage4Order();

    /**
     * 首页-工序完成情况
     *
     * @return 结果
     */
    @ApiOperation("首页-工序完成情况")
    @PostMapping("/monthPlan/report/homePage4ProductionProcesses")
    public AjaxResult homePage4ProductionProcesses();

    /**
     * 首页-工厂设备情况
     *
     * @return 结果
     */
    @ApiOperation("首页-工厂设备情况")
    @PostMapping("/monthPlan/report/homePage4Machine")
    public AjaxResult homePage4Machine();

    /**
     * 首页-今年生产情况
     *
     * @return 结果
     */
    @ApiOperation("首页-今年生产情况")
    @PostMapping("/monthPlan/report/homePage4YearProduct")
    public AjaxResult homePage4YearProduct();

    /**
     * 首页-工序完成情况-7天
     *
     * @return 结果
     */
    @ApiOperation("首页-工序完成情况-7天")
    @PostMapping("/monthPlan/report/selectProductionProcessesByDate7")
    public AjaxResult selectProductionProcessesByDate7();

    /**
     * 成型硫化机台维修情况
     *
     * @return 结果
     */
    @ApiOperation("成型硫化机台维修情况")
    @PostMapping("/monthPlan/report/selectCxLhMachineRepair")
    public AjaxResult selectCxLhMachineRepair();

    /**
     * 查询sku汇总分析-大屏
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询sku汇总分析-大屏")
    @PostMapping("/monthPlan/report/selectSkuSummary4BigScreen")
    public AjaxResult selectSkuSummary4BigScreen(@RequestBody MonthPlanReportDto queryDto);

    /**
     * 查询系统运行报表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @ApiOperation("查询系统运行报表")
    @PostMapping("/monthPlan/report/selectSystemRunReport")
    AjaxResult selectSystemRunReport(@RequestBody SystemRunReportDto queryDto);

    /**
     * 导出系统运行报表
     *
     * @param queryDto 查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出系统运行报表")
    @PostMapping({"/monthPlan/report/exportSystemRunReport"})
    public byte[] exportSystemRunReport(@RequestBody SystemRunReportDto queryDto, @RequestParam("fileName") String fileName);
}
