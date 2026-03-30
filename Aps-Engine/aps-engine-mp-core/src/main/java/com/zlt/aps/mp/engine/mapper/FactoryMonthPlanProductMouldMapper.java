package com.zlt.aps.mp.engine.mapper;

import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.mp.api.domain.vo.MoldCavityInsertMaxValueCalculatorVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 月度计划-模具相关业务SQL接口定义
 *
 * @author ZLT
 * @date 20251210
 */
@Mapper
public interface FactoryMonthPlanProductMouldMapper {
    /**
     * 根据需求计划，获取对应的需求模具配置信息
     * 其包含的信息为物料配置的模具及对应模具的基础信息(状态、模壳标准、主花纹)
     *
     * @param factoryCode      工厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 需求计划版本
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getProductionMouldInfo(@Param("factoryCode") String factoryCode,
                                                             @Param("year") Integer year,
                                                             @Param("month") Integer month,
                                                             @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 获取在排产周期范围内可到货的新物料模具关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次需求范围内
     *
     * @param factoryCode         工厂编码
     * @param year                年份
     * @param month               月份
     * @param monthPlanVersion    需求计划版本
     * @param productionStartDate 排产周期开始日
     * @param productionEndDate   排产周期结束日
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getMouldDeliveryInfo(@Param("factoryCode") String factoryCode,
                                                           @Param("year") Integer year,
                                                           @Param("month") Integer month,
                                                           @Param("monthPlanVersion") String monthPlanVersion,
                                                           @Param("productionStartDate") Date productionStartDate,
                                                           @Param("productionEndDate") Date productionEndDate);

    /**
     * 根据初始化信息结果，获取对应的需求模具配置信息
     * 其包含的信息为物料配置的模具及对应模具的基础信息(状态、模壳标准、主花纹)
     * 有效的模具信息(排除不排的计划)
     *
     * @param factoryCode       工厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param productionVersion 排产版本
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionMouldInfo(@Param("factoryCode") String factoryCode,
                                                                   @Param("year") Integer year,
                                                                   @Param("month") Integer month,
                                                                   @Param("monthPlanVersion") String monthPlanVersion,
                                                                   @Param("productionVersion") String productionVersion);

    /**
     * 根据定稿结果，获取对应的需求模具配置信息
     * 其包含的信息为物料配置的模具及对应模具的基础信息(状态、模壳标准、主花纹)
     * 有效的模具信息(排除不排的计划)
     *
     * @param factoryCode       工厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param productionVersion 排产版本
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableProductionFinalMouldInfo(@Param("factoryCode") String factoryCode,
                                                                   @Param("year") Integer year,
                                                                   @Param("month") Integer month,
                                                                   @Param("monthPlanVersion") String monthPlanVersion,
                                                                   @Param("productionVersion") String productionVersion);
    
    /**
     * 根据净需求信息结果，获取对应的需求模具配置信息
     * 其包含的信息为物料配置的模具及对应模具的基础信息(状态、模壳标准、主花纹)
     * 有效的模具信息(排除不排的计划)
     *
     * @param factoryCode       工厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param allMaterial       取全物料，如果是true则不会关联需求计划
     * @return list
     */
    List<MoldCavityInsertMaxValueCalculatorVo> getEnableProductionMouldInfoByNetDemand(@Param("factoryCode") String factoryCode,
                                                                                       @Param("year") Integer year,
                                                                                       @Param("month") Integer month,
                                                                                       @Param("monthPlanVersion") String monthPlanVersion,
                                                                                       @Param("allMaterial") Boolean allMaterial);

    /**
     * 根据净需求信息获取在排产周期范围内可到货的新物料模具关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次需求范围内
     * 有效的模具信息(排除不排的计划)
     *
     * @param factoryCode         工厂编码
     * @param year                年份
     * @param month               月份
     * @param monthPlanVersion    需求计划版本
     * @param productionStartDate 排产周期开始日
     * @param productionEndDate   排产周期结束日
     * @return list
     */
    List<MoldCavityInsertMaxValueCalculatorVo> getEnableMouldDeliveryInfoByNetDemand(@Param("factoryCode") String factoryCode,
                                                                 @Param("year") Integer year,
                                                                 @Param("month") Integer month,
                                                                 @Param("monthPlanVersion") String monthPlanVersion,
                                                                 @Param("productionStartDate") Date productionStartDate,
                                                                 @Param("productionEndDate") Date productionEndDate);

    /**
     * 根据排产初始化获取在排产周期范围内可到货的新物料模具关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次需求范围内
     * 有效的模具信息(排除不排的计划)
     *
     * @param factoryCode         工厂编码
     * @param year                年份
     * @param month               月份
     * @param monthPlanVersion    需求计划版本
     * @param productionVersion   排产版本
     * @param productionStartDate 排产周期开始日
     * @param productionEndDate   排产周期结束日
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableMouldDeliveryInfo(@Param("factoryCode") String factoryCode,
                                                                 @Param("year") Integer year,
                                                                 @Param("month") Integer month,
                                                                 @Param("monthPlanVersion") String monthPlanVersion,
                                                                 @Param("productionVersion") String productionVersion,
                                                                 @Param("productionStartDate") Date productionStartDate,
                                                                 @Param("productionEndDate") Date productionEndDate);

    /**
     * 根据定稿获取在排产周期范围内可到货的新物料模具关系信息
     * 1、上机日期在排产周期范围 [productionStartDate,productionEndDate]
     * 2、新模具到货中的物料在本次需求范围内
     * 有效的模具信息(排除不排的计划)
     *
     * @param factoryCode         工厂编码
     * @param year                年份
     * @param month               月份
     * @param monthPlanVersion    需求计划版本
     * @param productionVersion   排产版本
     * @param productionStartDate 排产周期开始日
     * @param productionEndDate   排产周期结束日
     * @return
     */
    List<MonthPlanProductMouldInfoVo> getEnableFinalMouldDeliveryInfo(@Param("factoryCode") String factoryCode,
                                                                 @Param("year") Integer year,
                                                                 @Param("month") Integer month,
                                                                 @Param("monthPlanVersion") String monthPlanVersion,
                                                                 @Param("productionVersion") String productionVersion,
                                                                 @Param("productionStartDate") Date productionStartDate,
                                                                 @Param("productionEndDate") Date productionEndDate);

    
    /**
     * 获取工厂的模壳台账信息
     *
     * @param factoryCode 工厂编码
     * @return
     */
    List<MouldShellBaseInfoVo> getMouldShellInfo(@Param("factoryCode") String factoryCode);

    /**
     * 获取工厂某个年份、月份的模具分配比例配置
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<MouldAllocationInfoVo> getMouldAllocationInfo(@Param("factoryCode") String factoryCode,
                                                       @Param("year") Integer year,
                                                       @Param("month") Integer month);
}
