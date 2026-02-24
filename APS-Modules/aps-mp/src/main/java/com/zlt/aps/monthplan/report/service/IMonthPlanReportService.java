package com.zlt.aps.monthplan.report.service;

import com.zlt.aps.monthplan.api.domain.dto.*;
import com.zlt.aps.monthplan.api.domain.vo.*;

import java.util.Date;
import java.util.List;

/**
 * 报表服务
 * @author Chen
 */
public interface IMonthPlanReportService {

    /**
     * 查询T月完成率列表数据
     * @param queryDto 查询参数
     * @return 结果
     */
    MonthFinishRateVo listMonthFinishRate(MonthPlanReportDto queryDto);

    /**
     * 导出T月完成率数据
     * @param queryDto 查询参数
     * @return 文件数组
     */
    byte[] exportMonthFinishRate(MonthPlanReportDto queryDto);

    /**
     * 查询T月完成率-品牌列表数据
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateBrandVo> listMonthFinishRateBrand(MonthPlanReportDto queryDto);

    /**
     * 导出T月完成率-品牌数据
     * @param queryDto 查询参数
     * @return 文件数组
     */
    byte[] exportMonthFinishRateBrand(MonthPlanReportDto queryDto);

    /**
     * 查询T月完成率-品牌寸别列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    public List<MonthFinishRateProSizeVo> listMonthFinishRateBrandProSize(MonthPlanReportDto queryDto);

    /**
     * 导出T月完成率-品牌寸别数据
     *
     * @param queryDto 查询参数
     * @return 文件数组
     */
    public byte[] exportMonthFinishRateBrandProSize(MonthPlanReportDto queryDto);

    /**
     * 查询sku汇总分析
     * @param queryDto 查询参数
     * @return 结果
     */
    SkuMonthQtyVo listSkuSummary(MonthPlanReportDto queryDto);

    /**
     * 导出sku汇总分析
     * @param queryDto 查询参数
     * @return 结果
     */
    byte[] exportSkuSummary(MonthPlanReportDto queryDto);

    /**
     * 查询投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    public List<SkuSummaryProduceVo> listSkuSummaryProduce(MonthPlanReportDto queryDto);

    /**
     * 导出投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    public byte[] exportSkuSummaryProduce(MonthPlanReportDto queryDto);

    /**
     * 查询试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    public List<SkuSummaryTrialVo> listSkuSummaryTrial(MonthPlanReportDto queryDto);

    /**
     * 导出试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    public byte[] exportSkuSummaryTrial(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> listBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 导出品牌-尺寸汇总分析
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ReportClassificationVo> listChannelClassification(ClassificationReportDto queryDto);

    /**
     * 导出渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportChannelClassification(ClassificationReportDto queryDto);

    /**
     * 查询品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ReportClassificationVo> listBrandClassification(ClassificationReportDto queryDto);

    /**
     * 导出品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportBrandClassification(ClassificationReportDto queryDto);

    /**
     * 查询寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ReportClassificationVo> listProSizeClassification(ClassificationReportDto queryDto);

    /**
     * 导出寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportProSizeClassification(ClassificationReportDto queryDto);

    /**
     * 查询品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ReportClassificationVo> listBrandLocationClassification(ClassificationReportDto queryDto);

    /**
     * 导出品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportBrandLocationClassification(ClassificationReportDto queryDto);

    /**
     * 查询寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ReportClassificationVo> listProSizeChannelClassification(ClassificationReportDto queryDto);

    /**
     * 导出寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportProSizeChannelClassification(ClassificationReportDto queryDto);

    /**
     * 获取胎类区分-排产受限满足率列表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<TireTypeReportSatisfyRateVo> getReportTireTypeSatisfyRateList(BaseReportDto queryDto);

    /**
     * 导出胎类区分-排产受限满足率列表
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportTireTypeSatisfyRate(BaseReportDto queryDto);

    /**
     * 查询胎类区分-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeClassificationVo> selectMonthTireTypeList(TireTypeReportDto queryDto);

    /**
     * 导出胎类区分-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    byte[] exportMonthTireType(TireTypeReportDto queryDto);

    /**
     * 查询生产销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProduceSalePlanResultVo> selectProduceSalePlanList(BaseReportDto queryDto);

    /**
     * 导出生产销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    byte[] exportProduceSalePlan(BaseReportDto queryDto);

    /**
     * 查询排产版本列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> listProduceVersionList(ProduceVersionDto queryDto);

    /**
     * 查询首页计划数据
     *
     * @param date 日期
     * @return 结果
     */
    HomePage4PlanVo homePage4Plan(Date date);

    /**
     * 查询首页订单接单情况
     *
     * @param date 日期
     * @return 结果
     */
    List<HomePage4OrderVo> homePage4Order(Date date);

    /**
     * 查询首页工序完成情况
     *
     * @param date 日期
     * @return 结果
     */
    List<HomePage4ProductProcessesVo> homePage4ProductionProcesses(Date date);

    /**
     * 查询首页工厂设备情况
     *
     * @param date 日期
     * @return 结果
     */
    List<HomePage4MachineVo> homePage4Machine(Date date);

    /**
     * 查询今年生产情况
     *
     * @param date 日期
     * @return 结果
     */
    List<HomePage4PlanVo> homePage4YearProduct(Date date);

    /**
     * 查询首页工序完成情况-7天
     *
     * @param date 日期
     * @return 结果
     */
    List<HomePage4ProductProcessesVo> selectProductionProcessesByDate7(Date date);

    /**
     * 查询成型硫化机台，维修情况
     *
     * @param date 日期
     * @return 结果
     */
    List<CxLhMachineVo> selectCxLhMachine(Date date);

    /**
     * 查询sku汇总分析-大屏
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    SkuMonthQtyVo selectSkuSummary4BigScreen(MonthPlanReportDto queryDto);

    /**
     * 查询系统运行报表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<SystemRunReportVo> selectSystemRunReport(SystemRunReportDto queryDto);
}
