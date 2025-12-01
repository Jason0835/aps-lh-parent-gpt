package com.zlt.aps.factory.service;

import com.zlt.aps.factory.domain.dto.MachineCountDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 月份排产计算，需要获取数据的接口信息
 *
 * @author ZLT
 * @date 20250220
 */
public interface ProductionSchedulingDataService {
    /**
     * 获取分厂排程版本
     *
     * @param context 排产上下文
     * @return
     */
    FactoryProductionVersion getFactoryMonthPlanVersion(ProductionContext context);

    /**
     * 更新分厂排程版本
     * 补充 初始化版本及排产版本
     *
     * @param updateVersion
     * @return
     */
    int updateFactoryProductionVersion(FactoryProductionVersion updateVersion);

    /**
     * 根据排产版本号，更新排产月份模式及排产开始、结束日
     *
     * @param updateVersion
     * @return
     */
    int updateProductionVersionInfo(FactoryProductionVersion updateVersion);

    /**
     * 增加一条分厂排程版本记录
     *
     * @param updateVersion
     * @return
     */
    int addFactoryProductionVersion(FactoryProductionVersion updateVersion);

    /**
     * 根据分厂、年份、月份获取分厂对应的停车日历日期范围
     *
     * @param context
     * @return
     */
    List<ProductionCalendarVO> getProductCalendar(ProductionContext context);

    /**
     * 根据分厂、年份、月份获取对应的物料施工关系信息
     *
     * @param context
     * @return
     */
    List<MdmProductConstruction> getProductConstruction(ProductionContext context);

    /**
     * 获取投产施工基础信息
     *
     * @return
     */
    Map<String, BaseConstructionVersionInfoVo> getBaseConstructionInfo();

    /**
     * 根据分厂、年份、月份获取对应的物料基础信息
     * 包含 寸口、毛利率，硫化时间，模具大类
     *
     * @param context
     * @return
     */
    List<ProductBaseInfoVo> getProductBaseInfo(ProductionContext context);

    /**
     * 根据查询条件，获取分厂的排产制造需求计划数据
     *
     * @param productionContext
     * @return
     */
    List<SaleMonthPlanRequire> getFactoryMonthPlan(ProductionContext productionContext);

    /**
     * 根据查询条件，获取分厂的排程计划数据
     *
     * @param productionContext
     * @return
     */
    List<MonthPlanManufacturingRequirementVo> getFactoryMonthPlanManufacturing(ProductionContext productionContext);

    /**
     * 获取分厂的品名排产参数设置
     *
     * @param factoryCode
     * @param productTypeCode
     * @return
     */
    Map<String, FactoryParam> getFactoryParamConfiguration(String factoryCode, String productTypeCode);

    /**
     * 获取分厂品名物料的折损率配置
     *
     * @param factoryCode
     * @param productTypeCode
     * @return
     */
    Map<String, ProductALevelVo> getProductDamageConfiguration(String factoryCode, String productTypeCode);

    /**
     * 获取分厂的最小批量设置
     *
     * @param productionContext
     * @return
     */
    Map<String, Long> getMinimumLotSizeConfiguration(ProductionContext productionContext);

    /**
     * 根据分厂编号，获取分厂的排产分组信息
     *
     * @param factoryCode 分厂编码
     * @return
     */
    List<ProductionGroupVo> getFactoryProductionGroupConfiguration(String factoryCode);

    /**
     * 获取分厂、年份、月份的可用模具信息
     * 没有，则返回空集合
     *
     * @param context
     * @return
     */
    List<MouldInfoVO> getMonthEnableMouldConfiguration(ProductionContext context);

    /**
     * 获取分厂、年份、月份的物料与模具关系
     *
     * @param context
     * @return
     */
    List<ProductMouldConfigurationVo> getProductionMouldInfoConfiguration(ProductionContext context);

    /**
     * 获取分厂在指定年份、月份的不排产物料信息，并按物料分组
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    Map<String, FactoryNoProduction> getFactoryNoProductionConfiguration(String factoryCode, Integer year, Integer month);

    /**
     * 获取分厂在指定年份、月份的寸口产能分配配置
     *
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<SizeCapacityConfiguration> getSizeCapacityConfiguration(String factoryCode, Integer year, Integer month);

    /**
     * 获取分厂在指定年份、月份的轮胎类型产能分配配置
     *
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<TireCapacityConfiguration> getTireCapacityConfiguration(String factoryCode, Integer year, Integer month);

    /**
     * 获取物料可用模具配置信息
     *
     * @param context
     * @return
     */
    List<ProductMouldInfoVO> getEnableUseProductMouldConfiguration(ProductionContext context);

    /**
     * 获取模具维修返厂配置信息
     *
     * @param context
     * @return
     */
    List<MouldInfoVO> getMouldMaintenanceConfiguration(ProductionContext context);

    /**
     * 获取利率优先值配置
     *
     * @return
     */
    List<MdmInterestRate> getInterestRateConfiguration();

    /**
     * 根据上下文，删除某个版本的初始化数据
     *
     * @param context
     */
    void deletedInitData(ProductionContext context);

    /**
     * 根据上下文，删除某个版本的模具排产数据
     *
     * @param context
     */
    void deletedMouldProductionData(ProductionContext context);

    /**
     * 根据上下文，获取正在续作的规格和模具
     *
     * @param context 上下文配置
     * @return
     */
    List<MouldProductionProductVo> getContinueProductAndMould(ProductionContext context);

    /**
     * 保存不排产记录信息
     *
     * @param factoryNoProductionPlanList
     */
    void saveNoProductionPlanRecord(List<MonthPlanNoProductionRecord> factoryNoProductionPlanList);

    /**
     * 删除不排产记录
     *
     * @param context
     * @return
     */
    int deletedNoProductionRecord(ProductionContext context);

    /**
     * 保存排产版本的模具产能预占分配结果
     *
     * @param productionContext 排产上下文
     * @param preCapacityList   模具预分配结果列表
     */
    void saveMouldPreCapacity(ProductionContext productionContext, List<MonthPlanManufacturingRequirementVo> preCapacityList);

    /**
     * 保存排产初始化信息
     *
     * @param monthPlanInitList
     */
    void saveMonthPlanInit(List<MonthPlanManufacturingRequirementVo> monthPlanInitList);

    /**
     * 根据上下文，获取分厂排程排产顺序配置
     *
     * @param context
     * @return
     */
    List<PlanOrderSortConfiguration> getProductionConfiguration(ProductionContext context);

    /**
     * 更新排产计划初始化的排产顺序
     *
     * @param productionSequenceList
     */
    void updateProductionSequence(List<MonthPlanManufacturingRequirementVo> productionSequenceList);

    /**
     * 保存明细
     *
     * @param detailList
     */
    void saveMouldProductionDetail(List<MonthPlanProductionResultDetail> detailList);

    /**
     * 保存未排计划信息
     *
     * @param noProductionPlanList
     */
    void saveNoProductionPlan(List<MonthPlanNoProductionPlan> noProductionPlanList);

    /**
     * 保存排产汇总结果
     *
     * @param dayList 汇总结果列表
     */
    void saveMouldProductionSummary(List<MonthPlanMouldingDayResult> dayList);

    /**
     * 保存月计划版本排产结果--按SKU汇总
     *
     * @param dayProductionResultList SKU排产结果
     */
    void saveMonthPlanProductionResult(List<MonthPlanProductionDayResult> dayProductionResultList);

    /**
     * 保存模具排产结果辅助记录
     *
     * @param mouldingProductionResultList
     */
    void saveMouldingProductionResult(List<MouldingProductionResultHelper> mouldingProductionResultList);

    /**
     * 排产排产分组结果辅助记录
     *
     * @param productionGroupResultList
     */
    void saveProductionGroupResult(List<ProductionGroupResultHelper> productionGroupResultList);

    /**
     * 保存模具排程排产流程日志
     *
     * @param productionLog 日志信息
     */
    void saveMouldProductionLog(MouldProductionLog productionLog);

    /**
     * 获取分厂成型机、硫化机 机台数
     *
     * @param factoryCode
     * @return
     */
    MachineCountDto getMachineNumberInfo(String factoryCode);

    /**
     * 获取分厂外贸贴牌品牌耗损
     *
     * @param factoryCode 分厂
     * @return
     */
    Set<String> getExportOemBrand(String factoryCode);

    /**
     * 获取月平均销量大于averageValue值的物料集合
     *
     * @param factoryCode  分厂
     * @param year         年份
     * @param month        月份
     * @param averageValue 月均销量
     * @return
     */
    Set<String> getGreaterAverageValueProductInfo(String factoryCode, Integer year, Integer month, Integer averageValue);
}
