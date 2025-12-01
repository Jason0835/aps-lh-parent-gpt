package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 计划调整SQL接口定义类
 *
 * @author ZLT
 * @date 20250331
 */
@Mapper
public interface FactoryMonthPlanAdjustMapper {

    /**
     * 根据年、月、分厂及物料和模具号，获取对应的月度可用模具列表
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @param mouldNo     模具
     * @param productCode 物料编码
     * @return
     */
    List<MouldProductRelationDto> getMonthEnableMouldConfiguration(@Param("factoryCode") String factoryCode,
                                                                   @Param("year") Integer year,
                                                                   @Param("month") Integer month,
                                                                   @Param("mouldNo") String mouldNo,
                                                                   @Param("productCode") String productCode);

    /**
     * 根据年、月、分厂及物料和模具号，获取对应的月度可用模具列表
     *
     * @param factoryCode     分厂编码
     * @param year            年份
     * @param month           月份
     * @param mouldNo         模具
     * @param productCodeList 物料编码集合
     * @return
     */
    List<MouldProductRelationDto> getMonthShareMouldConfiguration(@Param("factoryCode") String factoryCode,
                                                                  @Param("year") Integer year,
                                                                  @Param("month") Integer month,
                                                                  @Param("mouldNo") String mouldNo,
                                                                  @Param("productCodeList") List<String> productCodeList);

    /**
     * 根据年、月、分厂及物料和模具号，获取对应的模具维修计划列表
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @param mouldNo     模具
     * @param productCode 物料编码
     * @return
     */
    List<MouldProductRelationDto> getFactoryMouldMaintenanceConfiguration(@Param("factoryCode") String factoryCode,
                                                                          @Param("year") Integer year,
                                                                          @Param("month") Integer month,
                                                                          @Param("mouldNo") String mouldNo,
                                                                          @Param("productCode") String productCode);

    /**
     * 根据SAP代码及模具，分厂，获取模具其他共享模具的SAP代码
     *
     * @param factoryCode
     * @param productCode
     * @param mouldNo
     * @return
     */
    List<MouldProductRelationDto> getShareMouldProductList(@Param("factoryCode") String factoryCode,
                                                           @Param("productCode") String productCode,
                                                           @Param("mouldNo") String mouldNo);

    /**
     * 根据年、月、分厂及物料和模具号，获取共用模具的其它物料配置的mouldNo的模具
     * <p>
     * 主要用以计算共用模具的产能
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @param mouldNo     模具
     * @param productCode 物料编码
     * @return
     */
    List<MouldProductRelationDto> getMonthEnableRelationMouldConfiguration(@Param("factoryCode") String factoryCode,
                                                                           @Param("year") Integer year,
                                                                           @Param("month") Integer month,
                                                                           @Param("mouldNo") String mouldNo,
                                                                           @Param("productCode") String productCode);

    /**
     * 根据分厂、排产周期及物料和模具号，获取对应的模具维修计划列表
     *
     * @param factoryCode         分厂编码
     * @param productionStartDate 开始
     * @param productionEndDate   结束
     * @param mouldNo             模具
     * @param productCode         物料编码
     * @return
     */
    List<MouldProductRelationDto> getFactoryMouldMaintenanceConfigurationByCycle(@Param("factoryCode") String factoryCode,
                                                                                 @Param("productionStartDate") Date productionStartDate,
                                                                                 @Param("productionEndDate") Date productionEndDate,
                                                                                 @Param("mouldNo") String mouldNo,
                                                                                 @Param("productCode") String productCode);

    /**
     * 根据分厂、排产周期及物料和模具号，获取共用模具的其它物料配置的mouldNo的模具的维修信息
     *
     * @param factoryCode         分厂编码
     * @param productionStartDate 开始
     * @param productionEndDate   结束
     * @param mouldNo             模具
     * @param productCode         物料编码
     * @return
     */
    List<MouldProductRelationDto> getFactoryMouldRelationMaintenanceConfigurationByCycle(@Param("factoryCode") String factoryCode,
                                                                                         @Param("productionStartDate") Date productionStartDate,
                                                                                         @Param("productionEndDate") Date productionEndDate,
                                                                                         @Param("mouldNo") String mouldNo,
                                                                                         @Param("productCode") String productCode);

    /**
     * 根据年、月、分厂和模具号，获取对应的月度最大可用模具列表
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @param mouldNo     模具
     * @return
     */
    List<MouldProductRelationDto> getMonthMaxEnableMouldConfiguration(@Param("factoryCode") String factoryCode,
                                                                      @Param("year") Integer year,
                                                                      @Param("month") Integer month,
                                                                      @Param("mouldNo") String mouldNo);

    /**
     * 根据分厂、排产周期及物料和模具号，获取对应的模具维修计划列表
     *
     * @param factoryCode         分厂编码
     * @param productionStartDate 开始
     * @param productionEndDate   结束
     * @param mouldNo             模具
     * @return
     */
    List<MouldProductRelationDto> getMaxFactoryMouldMaintenanceConfigurationByCycle(@Param("factoryCode") String factoryCode,
                                                                                    @Param("productionStartDate") Date productionStartDate,
                                                                                    @Param("productionEndDate") Date productionEndDate,
                                                                                    @Param("mouldNo") String mouldNo);

    /**
     * 根据分厂、排产周期及物料和模具号，获取对应的模具维修计划列表
     *
     * @param factoryCode         分厂编码
     * @param productionStartDate 开始
     * @param productionEndDate   结束
     * @param mouldNo             模具
     * @param productCodeList     共用模具的其它SAP代码集合
     * @return
     */
    List<MouldProductRelationDto> getFactoryMouldShareMaintenanceConfigurationByCycle(@Param("factoryCode") String factoryCode,
                                                                                      @Param("productionStartDate") Date productionStartDate,
                                                                                      @Param("productionEndDate") Date productionEndDate,
                                                                                      @Param("mouldNo") String mouldNo,
                                                                                      @Param("productCodeList") List<String> productCodeList);
}
