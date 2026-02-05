package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月份排产计算，需要实现的接口信息
 *
 * @author ZLT
 * @date 20250220
 */
@Mapper
public interface FactoryProductionSchedulingMapper {

    /**
     * 根据分厂、年份、月份，需求计划版本，获取分厂不排产的物料
     *
     * @param factoryCode 分厂
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<FactoryNoProduction> getFactoryNoProductionConfiguration(@Param("factoryCode") String factoryCode,
                                                                  @Param("year") Integer year,
                                                                  @Param("month") Integer month);

    /**
     * 根据需求版本及排产版本，删除对应的排产初始化数据
     *
     * @param factoryCode       分厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param productionVersion 排产版本号
     * @return
     */
    int deleteProductionInitVersion(@Param("factoryCode") String factoryCode,
                                    @Param("year") Integer year,
                                    @Param("month") Integer month,
                                    @Param("monthPlanVersion") String monthPlanVersion,
                                    @Param("productionVersion") String productionVersion);

    /**
     * 根据需求版本及排产版本，删除排产版本的排产数据信息
     *
     * @param factoryCode       分厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param productionVersion 排产版本号
     * @return
     */
    int deleteProductionMouldVersion(@Param("factoryCode") String factoryCode,
                                     @Param("year") Integer year,
                                     @Param("month") Integer month,
                                     @Param("monthPlanVersion") String monthPlanVersion,
                                     @Param("productionVersion") String productionVersion);

    /**
     * 根据需求版本及排产版本，删除对应结构后的排产数据
     * 模具排产结果，模具排产日志，未排产计划
     *
     * @param factoryCode       分厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param productionVersion 排产版本号
     * @return
     */
    int deleteProductionVersionAfterGroup(@Param("factoryCode") String factoryCode,
                                          @Param("year") Integer year,
                                          @Param("month") Integer month,
                                          @Param("monthPlanVersion") String monthPlanVersion,
                                          @Param("productionVersion") String productionVersion);

    /**
     * 根据分厂，获取分厂的硫化机台数量
     *
     * @param factoryCode 分厂编码
     * @return
     */
    Integer getVulcanizationMachineCount(@Param("factoryCode") String factoryCode);

    /**
     * 根据分厂，获取分厂的成型机台数量
     *
     * @param factoryCode 分厂编码
     * @return
     */
    Integer getFormingMachineCount(@Param("factoryCode") String factoryCode);

}
