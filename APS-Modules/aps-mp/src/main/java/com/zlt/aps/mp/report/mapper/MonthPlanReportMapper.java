package com.zlt.aps.mp.report.mapper;

import com.zlt.aps.monthplan.api.domain.dto.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author Chen
 */
@Mapper
public interface MonthPlanReportMapper {

    /**
     * 查询月份对应的会计周期开始日期、结束日期
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<AccountingPeriodVo> selectAccountingPeriod(BaseReportDto queryDto);

    /**
     * 查询终稿的月计划版本，用于关联月计划版本、过滤数据
     * @param queryDto 查询参数
     * @return 终稿的版本
     */
    String selectFinalMonthPlanVersion(BaseReportDto queryDto);

    /**
     * 查询生产的T月完成率列表数据(会计周期)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateRangeVo> listProduceMonthFinishRate4AccountingPeriod(MonthPlanReportDto queryDto);

    /**
     * 查询生产的T月完成率列表数据(正常自然月)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateRangeVo> listProduceMonthFinishRate(MonthPlanReportDto queryDto);

    /**
     * 查询销售的T月完成率列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateRangeVo> listSaleMonthFinishRate(MonthPlanReportDto queryDto);

    /**
     * 查询试制或量试的T月完成率列表数据(会计周期)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    MonthFinishRateRangeVo selectTrialProduceMonthFinishRate4AccountingPeriod(MonthPlanReportDto queryDto);

    /**
     * 查询试制或量试的T月完成率列表数据(正常自然月)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    MonthFinishRateRangeVo selectTrialProduceMonthFinishRate(MonthPlanReportDto queryDto);

    /**
     * 查询生产及销售T月完成率数据(会计周期)
     * @param queryDto 查询参数
     * @return 结果
     */
    MonthFinishRateVo selectProduceAndSale4AccountingPeriod(MonthPlanReportDto queryDto);

    /**
     * 查询生产及销售T月完成率数据(正常自然月)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    MonthFinishRateVo selectProduceAndSale(MonthPlanReportDto queryDto);

    /**
     * 查询T月完成率-品牌数据(会计周期)
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateBrandVo> selectMonthPlanFinishBrand4AccountingPeriod(MonthPlanReportDto queryDto);

    /**
     * 查询T月完成率-品牌数据(正常自然月)
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateBrandVo> selectMonthPlanFinishBrand(MonthPlanReportDto queryDto);

    /**
     * 查询T月完成率-品牌寸别数据(会计周期)
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateProSizeVo> selectMonthPlanFinishBrandSize4AccountingPeriod(MonthPlanReportDto queryDto);

    /**
     * 查询T月完成率-品牌寸别数据(正常自然月)
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MonthFinishRateProSizeVo> selectMonthPlanFinishBrandSize(MonthPlanReportDto queryDto);

    /**
     * 查询会计周期月份对应的停车天数
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<AccountingPeriodVo> selectStopDay(BaseReportDto queryDto);

    /**
     * 查询会计周期月份对应的物料数、计划数(会计周期)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<AccountingPeriodVo> selectProduceQtyAndCount4AccountingPeriod(BaseReportDto queryDto);

    /**
     * 查询会计周期月份对应的物料数、计划数(正常自然月)
     * @param queryDto 查询条件
     * @return 结果
     */
    List<AccountingPeriodVo> selectProduceQtyAndCount(BaseReportDto queryDto);

    /**
     * 查询会计周期月份对应的物料数、完成数、完成天数
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<AccountingPeriodVo> selectFinishQtyAndCount(MonthPlanReportDto queryDto);

    /**
     * 根据条件查询投产sku分析
     * @param queryDto 查询参数
     * @return 结果
     */
    List<SkuSummaryProduceVo> selectSkuSummaryProduce(MonthPlanReportDto queryDto);

    /**
     * 根据条件查询试制sku分析
     * @param queryDto 查询参数
     * @param dayList 天数列表
     * @return 结果
     */
    List<SkuSummaryTrialVo> selectSkuSummaryTrial(@Param("queryDto") MonthPlanReportDto queryDto, @Param("dayList") List<Integer> dayList);


    /**
     * 根据条件查询试制sku分析-物料详细
     *
     * @param queryDto 查询参数
     * @param dayList  天数列表
     * @return 结果
     */
    List<SkuSummaryTrialProductVo> selectSkuSummaryTrial4Detail(@Param("queryDto") MonthPlanReportDto queryDto, @Param("dayList") List<Integer> dayList);

    /**
     * 查询品牌-尺寸汇总分析
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析-库存部分
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectStockBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析-销售计划部分
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectSalePlanBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析-销售完成部分
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectSaleFinishBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析-生产计划部分(正常自然月)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectProPlanBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析-生产计划部分(会计周期)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectProPlanBrandProSizeSummary4AccountingPeriod(MonthPlanReportDto queryDto);

    /**
     * 查询品牌-尺寸汇总分析-生产完成部分
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<BrandProSizeSummaryVo> selectProFinishBrandProSizeSummary(MonthPlanReportDto queryDto);

    /**
     * 查询分类缺口差异基础数据
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ClassificationGapVo> selectBaseClassificationList(ClassificationReportDto queryDto);

    /**
     * 查询分类缺口差异基础数据-销售计划
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ClassificationGapVo> selectSalePlanClassification(ClassificationReportDto queryDto);

    /**
     * 查询分类缺口差异基础数据-生产计划(正常自然月)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ClassificationGapVo> selectProPlanClassification(ClassificationReportDto queryDto);

    /**
     * 查询分类缺口差异基础数据-生产计划(会计周期)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ClassificationGapVo> selectProPlanClassification4AccountingPeriod(ClassificationReportDto queryDto);

    /**
     * 查询分类缺口差异基础数据-库存
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<ClassificationGapVo> selectStockClassification(ClassificationReportDto queryDto);

    /**
     * 查询胎类区分汇总-生产计划列表数据(正常自然月)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeReportSatisfyRateVo> selectBaseTireTypeSatisfyRateList(BaseReportDto queryDto);

    /**
     * 查询胎类区分汇总-生产计划列表数据(会计周期)
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeReportSatisfyRateVo> selectBaseTireTypeSatisfyRateList4AccountingPeriod(BaseReportDto queryDto);

    /**
     * 查询胎类区分汇总-生产计划
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeReportSatisfyRateVo> selectProPlan4TireTypeSatisfyRate(BaseReportDto queryDto);

    /**
     * 查询胎类区分汇总-销售计划
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeReportSatisfyRateVo> selectSalePlan4TireTypeSatisfyRate(BaseReportDto queryDto);

    /**
     * 查询胎类区分汇总-库存
     * 没有渠道
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeReportSatisfyRateVo> selectStock4TireTypeSatisfyRate(BaseReportDto queryDto);

    /**
     * 查询单模排产的物料号列表
     *
     * @return 结果
     */
    List<String> selectSingleMouldProductCodeList();

    /**
     * 查询单模排产数据
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<TireTypeSingleMouldVo> selectSingleMouldList(BaseReportDto queryDto);

    /**
     * 根据订单号查询分配列表
     *
     * @param queryDto    查询条件
     * @param orderNoList 订单号列表
     * @return 结果
     */
    List<TireTypeSingleMouldVo> selectOrderPlanAllocationByOrderNo(@Param("queryDto") BaseReportDto queryDto, @Param("orderNoList") Set<String> orderNoList);

    /**
     * 查询成型排程结果
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    List<CxScheduleResultReportVo> selectCxScheduleResultQty(BaseReportDto queryDto);

    /**
     * 查询物料号和施工关系，关联对应
     *
     * @return 结果
     */
    List<TireTypeConstructionRealVo> selectConstructionReal();

    /**
     * 查询胎类区分-有排产的列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<TireTypeClassificationAllVo> selectTireTypeProduceList(TireTypeReportDto queryDto);

    /**
     * 查询胎类区分-全部未排产的列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<TireTypeClassificationVo> selectTireTypeNoProduceList(TireTypeReportDto queryDto);

    /**
     * 查询胎类区分-库存满足的列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<TireTypeClassificationVo> selectTireTypeStockSatisfiedList(TireTypeReportDto queryDto);

    /**
     * 查询胎类区分-试制量试无工艺的列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<TireTypeClassificationVo> selectTireTypeTrialList(TireTypeReportDto queryDto);

    /**
     * 查询生产销售计划-计划部分数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProduceSalePlanVo> selectProPlanProSizeLocationTypeWeight(BaseReportDto queryDto);

    /**
     * 查询生产销售计划-销售部分数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProduceSalePlanResultVo> selectSalePlanProSizeLocationTypeWeight(BaseReportDto queryDto);

    /**
     * 查询库存数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductStockMonth> selectProductStockMonth(BaseReportDto queryDto);

    /**
     * 查询生产计划数据(正常自然月)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<OrderPlanAllocation> selectProPlanList(BaseReportDto queryDto);

    /**
     * 查询生产计划数据(会计周期)
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<OrderPlanAllocation> selectProPlanList4AccountingPeriod(BaseReportDto queryDto);

    /**
     * 查询生产完成数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<LhDayFinishQtyVo> selectProFinishList(BaseReportDto queryDto);

    /**
     * 查询销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<OrderPlanAllocation> selectSalePlanList(BaseReportDto queryDto);

    /**
     * 查询销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<SaleFinishVo> selectSaleFinishList(BaseReportDto queryDto);

    /**
     * 查询物料和模具关系，根据物料分组，取规格代码
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MdmSkuMouldRel> selectProductMoldeRealList(BaseReportDto queryDto);

    /**
     * 查询理论备货数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectStockUpPlanQtyList(ProduceVersionDto queryDto);

    /**
     * 查询需求量数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectDemandQtyList(BaseReportDto queryDto);

    /**
     * 查询排产量数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectProductQtyList(BaseReportDto queryDto);

    /**
     * 查询排产量数据-定稿明细
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectProductQtyList4FinalDetail(BaseReportDto queryDto);

    /**
     * 查询库存量数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectStockQtyList(BaseReportDto queryDto);

    /**
     * 查询实际备货量数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectStockUpActQtyList(BaseReportDto queryDto);

    /**
     * 查询未排量数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ProductVersionReportVo> selectUnProductQtyList(BaseReportDto queryDto);

    /**
     * 查询版本对应年月、分厂
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    ProduceVersionDto selectByMonthPlanVersion(BaseReportDto queryDto);

    /**
     * 查询月计划定稿年计划量
     *
     * @param year 日期
     * @return 结果
     */
    List<HomePage4PlanVo> selectYearMonthPlanQty(Integer year);

    /**
     * 查询月计划定稿年计划量
     *
     * @param year 日期
     * @return 结果
     */
    List<HomePage4PlanVo> selectYearMonthFinishQty(Integer year);

    /**
     * 查询计划订单量和备库量
     *
     * @param year 年份
     * @return 结果
     */
    List<HomePage4OrderVo> selectOrderAndStockUpQty(Integer year);

    /**
     * 查询各工序完成情况
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    List<HomePage4ProductProcessesVo> selectProductionProcesses(String scheduleDate);

    /**
     * 查询工厂设备情况
     *
     * @return 结果
     */
    List<HomePage4MachineVo> selectFactoryMachine();

    /**
     * 查询工序完成情况-7天
     *
     * @param scheduleDate 当前日期
     * @param scheduleDateBefore7 当前日期前7天
     * @return 结果
     */
    List<HomePage4ProductProcessesVo> selectProductionProcessesByDate7(@Param("scheduleDate") String scheduleDate,
                                                                       @Param("scheduleDateBefore7") String scheduleDateBefore7);

    /**
     * 查询成型、硫化机台可用、停机情况
     *
     * @param year  年
     * @param month 月
     * @return 结果
     */
    List<CxLhMachineVo> selectCxLhMachine(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * 查询理论备货量对应的施工信息
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<CxProductConstructionInfo> selectProductConstructionInfo4RequirePlan(BaseReportDto queryDto);

    /**
     * 查询理论备货量对应的施工信息
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<MdmProductConstruction> selectProductConstruction4RequirePlan(BaseReportDto queryDto);

    /**
     * 根据年月查询排产版本
     *
     * @param queryDto 参数
     * @return 版本号
     */
    String selectProductVersion(BaseReportDto queryDto);

    /**
     * 查询系统运行情况
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<SystemRunReportVo> selectSystemRunReport(SystemRunReportDto queryDto);

    /**
     * 查询成型消耗量
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<CxConsumeVo> selectCxConsume(SystemRunReportDto queryDto);

    /**
     * 查询钢丝圈消耗量
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    List<ScheduleSummaryVo> getGsqCxConsume(SystemRunReportDto queryDto);
}
