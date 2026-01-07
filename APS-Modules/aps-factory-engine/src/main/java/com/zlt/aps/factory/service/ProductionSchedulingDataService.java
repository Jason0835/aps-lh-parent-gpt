package com.zlt.aps.factory.service;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ContinueGroupInfo;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
import com.zlt.aps.factory.domain.dto.MachineCountDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;

import java.util.List;
import java.util.Map;

/**
 * 月份排产计算，需要获取数据的接口信息
 *
 * @author ZLT
 * @date 20251208
 */
public interface ProductionSchedulingDataService {
    /**
     * 获取排产周期配置信息
     * 自然月与非自然月周期
     *
     * @param context 排产上下文
     * @return
     */
    Integer getProductionCycleConfiguration(Context context);

    /**
     * 批量获取业务参数设定
     *
     * @param context       排产上下文
     * @param paramCodeList 参数编码集合
     * @return
     */
    Map<String, Object> getFactoryParamByCondition(Context context, List<String> paramCodeList);

    /**
     * 获取工厂排程版本
     *
     * @param context 排产上下文
     * @return
     */
    MpFactoryProductionVersion getFactoryMonthPlanVersion(Context context);

    /**
     * 获取工厂需求计划版本第一个版本信息
     * (可能没有排，也可能有排)
     *
     * @param context
     * @return
     */
    MpFactoryProductionVersion getFirstFactoryMonthPlanVersion(Context context);
    /**
     * 根据工厂编码、年份、月份获取对应的定稿版本信息
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    MpFactoryProductionVersion getFinalVersion(String factoryCode, Integer year, Integer month);

    /**
     * 更新分厂排程版本
     * 补充 初始化版本及排产版本
     *
     * @param updateVersion
     * @return
     */
    int updateFactoryProductionVersion(MpFactoryProductionVersion updateVersion);

    /**
     * 根据排产版本号，更新排产月份模式及排产开始、结束日
     *
     * @param updateVersion
     * @return
     */
    int updateProductionVersionInfo(MpFactoryProductionVersion updateVersion);

    /**
     * 增加一条分厂排程版本记录
     *
     * @param updateVersion
     * @return
     */
    int addFactoryProductionVersion(MpFactoryProductionVersion updateVersion);

    /**
     * 根据工厂、排产信息获取工厂对应的月计划开停产工作日历
     *
     * @param context
     * @return
     */
    List<ProductionDayInfoVo> getProductCalendar(Context context);

    /**
     * 获取结构最小硫化机台配比信息
     *
     * @param context           排产上下文
     * @param structureNameList 结构集合
     * @return
     */
    List<MonthPlanStructureLhRatioVo> getLhRatioInfo(Context context, List<String> structureNameList);

    /**
     * 获取周期结构的最低硫化配比信息
     *
     * @param context 排产上下文
     * @return
     */
    List<CycleStructureMinLhMachineQtyVo> getCycleLhRatioInfo(Context context);

    /**
     * 获取续作SKU信息，包含续作机台及使用的模具数
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    List<ContinueProductInfo> getContinueProductionInfo(String factoryCode, Integer year, Integer month, Integer lastDay);

    /**
     * 获取在机结构信息，从结构排产表中获取
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    List<ContinueGroupInfo> getContinueGroupInfo(String factoryCode, Integer year, Integer month, Integer lastDay);

    /**
     * 获取工厂的成型基础配置信息
     * 包含成型维修停机信息(合并全局停产日)
     * 固定机构先后顺序，固定SKU
     * 不可作业结构，不可作业SKU
     * 最大排产天数及剩余可排产天数
     *
     * @param context 排产上下文
     * @return
     */
    Map<String, CxMachineBaseInfoVo> getCxMachineBaseInfo(Context context);

    /**
     * 获取投产施工基础信息
     *
     * @return
     */
    @Deprecated
    Map<String, BaseConstructionVersionInfoVo> getBaseConstructionInfo();

    /**
     * 根据查询条件，获取分厂的排产制造需求计划数据
     *
     * @param context 排产上下文
     * @return
     */
    List<DpDemandPlan> getFactoryMonthPlan(Context context);

    /**
     * 获取需求计划对应的物料基础信息
     *
     * @param context 排产上下文
     * @return
     */
    List<ProductBaseInfoVo> getProductionMaterialInfo(Context context);

    /**
     * 获取需求计划对应的施工配置关系信息
     *
     * @param context 排产上下文
     * @return
     */
    List<MonthPlanProductConstructionInfoVo> getProductionConstructionInfo(Context context);

    /**
     * 获取含有特殊材料的生胎配置信息
     *
     * @param context 排产上下文
     * @return
     */
    List<EmbryoSpecialMaterialInfoVo> getEmbryoSpecialMaterialInfo(Context context);

    /**
     * 获取特殊材料库存
     *
     * @param context 排产上下文
     * @return
     */
    List<SpecialMaterialStockVo> getSpecialMaterialStockInfo(Context context);

    /**
     * 获取成品库存
     *
     * @param context 排产上下文
     * @return
     */
    List<MdmProductStock> getMdmProductStock(Context context);

    /**
     * 根据查询条件，获取工厂的排产计划信息
     * 从初始化中获取
     *
     * @param context
     * @return
     */
    List<MonthPlanProductionRequirePlanVo> getFactoryMonthPlanManufacturing(Context context);

    /**
     * 获取分厂品名物料的折损率配置
     *
     * @param factoryCode
     * @param productTypeCode
     * @return
     */
    @Deprecated
    Map<String, ProductALevelVo> getProductDamageConfiguration(String factoryCode, String productTypeCode);

    /**
     * 获取分厂的最小批量设置
     *
     * @param productionContext
     * @return
     */
    @Deprecated
    Map<String, Long> getMinimumLotSizeConfiguration(ProductionContext productionContext);

    /**
     * 根据分厂编号，获取分厂的排产分组信息
     *
     * @param factoryCode 分厂编码
     * @return
     */
    @Deprecated
    List<ProductionGroupVo> getFactoryProductionGroupConfiguration(String factoryCode);

    /**
     * 根据需求计划，获取对应的需求模具配置信息
     * 其包含的信息为物料配置的模具及对应模具的基础信息(状态、模壳标准、主花纹)
     *
     * @param context
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getProductionMouldInfo(Context context);

    /**
     * 获取在排产周期范围内可到货的新物料模具关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次需求范围内
     *
     * @param context 排产上下文
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getProductionMouldDeliveryInfo(Context context);

    /**
     * 根据排产初始化，获取可排产SKU的模具关系信息
     * 其包含的信息为物料配置的模具(模具编号、模壳标准、主花纹)
     *
     * @param context
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionMouldInfo(Context context);

    /**
     * 根据排产初始化，获取在排产周期范围内可到货的新模具-物料关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次可排产范围内
     *
     * @param context 排产上下文
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionMouldDeliveryInfo(Context context);

    /**
     * 根据工厂，获取工厂的模壳台账信息
     *
     * @param context 排产上下文
     * @return
     */
    List<MouldShellBaseInfoVo> getMouldShellInfo(Context context);

    /**
     * 根据工厂，获取工厂的模具分配比例配置
     *
     * @param context
     * @return
     */
    List<MouldAllocationInfoVo> getMouldAllocationInfo(Context context);

    /**
     * 获取对应SKU的日硫化量信息
     *
     * @param context 排产上下文
     * @return
     */
    List<MonthPlanProductLhCapacityVo> getProductLhCapacityInfo(Context context);

    /**
     * 获取分厂在指定年份、月份的不排产物料信息，并按物料分组
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    @Deprecated
    Map<String, FactoryNoProduction> getFactoryNoProductionConfiguration(String factoryCode, Integer year, Integer month);

    /**
     * 获取利率优先值配置
     *
     * @return
     */
    @Deprecated
    List<MdmInterestRate> getInterestRateConfiguration();

    /**
     * 根据上下文，删除某个版本的初始化数据
     *
     * @param context
     */
    void deletedInitData(Context context);

    /**
     * 根据上下文，删除某个版本的模具排产数据
     *
     * @param context
     */
    void deletedMouldProductionData(Context context);

    /**
     * 保存排产初始化信息
     *
     * @param monthPlanInitList
     */
    void saveMonthPlanInit(List<MonthPlanProductionRequirePlanVo> monthPlanInitList);

    /**
     * 根据上下文，获取分厂排程排产顺序配置
     *
     * @param context
     * @return
     */
    @Deprecated
    List<PlanOrderSortConfiguration> getProductionConfiguration(ProductionContext context);

    /**
     * 保存未排计划信息
     *
     * @param noProductionPlanList
     */
    void saveNoProductionPlan(List<MonthPlanNoProductionPlan> noProductionPlanList);

    /**
     * 保存模具排产明细日志
     *
     * @param detailLogList
     */
    void saveMouldProductionDetailLog(List<FactoryMonthPlanMouldDayDetail> detailLogList);

    /**
     * 保存模具排产结果信息
     *
     * @param dayResultList
     */
    void saveMouldProductionResult(List<FactoryMonthPlanMouldDayResult> dayResultList);

    /**
     * 保存模具排程排产流程日志
     *
     * @param productionLog 日志信息
     */
    void saveMouldProductionLog(MouldProductionLog productionLog);

    /**
     * 保存分组计划的成型转产结果
     * TBR-为结构
     * PCR-英寸
     *
     * @param allocationResult
     */
    void saveGroupConversionResult(List<MpStructureAllocation> allocationResult);

    /**
     * 获取分厂成型机、硫化机 机台数
     *
     * @param factoryCode
     * @return
     */
    MachineCountDto getMachineNumberInfo(String factoryCode);

}
