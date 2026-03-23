package com.zlt.aps.mp.engine.service;

import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.MachineCountDto;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.ProductALevelVo;
import com.zlt.aps.mp.engine.domain.vo.*;
import io.swagger.models.auth.In;

import java.util.List;
import java.util.Map;

/**
 * 月份排产计算，需要获取数据的接口信息
 *
 * @author ZLT
 * @date 20251208
 */
public interface ProductionMdmDataService {
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
     * 获取工厂的成型鼓台账信息
     *
     * @param context
     * @return
     */
    List<MdmWorkWearInfo> getWorkWearInfo(Context context);

    /**
     * 获取硫化机台数
     * @param context
     * @return
     */
    List<LhMachineInfo> listLhMachineInfo(Context context);

    /**
     * 获取工作日历
     * @param context
     * @return
     */
    Map<Integer, MdmWorkCalendar> getWorkCalendar(Context context);

    /**
     * 获取工厂的胶囊卡盘信息
     *
     * @param context
     * @return
     */
    List<MdmCapsuleChuck> getCapsuleChuck(Context context);

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
     * 获取含有特殊材料的生胎配置信息
     * 基于净需求计划
     *
     * @param context 排产上下文
     * @return
     */
    List<EmbryoSpecialMaterialInfoVo> getEmbryoSpecialMaterialInfoByRequire(Context context);
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
     * 获取分厂品名物料的折损率配置
     *
     * @param factoryCode
     * @param productTypeCode
     * @return
     */
    @Deprecated
    Map<String, ProductALevelVo> getProductDamageConfiguration(String factoryCode, String productTypeCode);

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
     * 根据定稿排产记录，获取可排产SKU的模具关系信息
     * 其包含的信息为物料配置的模具(模具编号、模壳标准、主花纹)
     *
     * @param context
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionFinalMouldInfo(Context context);

    /**
     * 根据定稿，获取在排产周期范围内可到货的新模具-物料关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次可排产范围内
     *
     * @param context 排产上下文
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionMouldDeliveryInfo(Context context);

    /**
     * 根据排产初始化，获取在排产周期范围内可到货的新模具-物料关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次可排产范围内
     *
     * @param context 排产上下文
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionFinalMouldDeliveryInfo(Context context);

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
     * 获取利率优先值配置
     *
     * @return
     */
    @Deprecated
    List<MdmInterestRate> getInterestRateConfiguration();

    /**
     * 获取分厂成型机、硫化机 机台数
     *
     * @param factoryCode
     * @return
     */
    MachineCountDto getMachineNumberInfo(String factoryCode);

}
