package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.ProductMinConfiguration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
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
     * 根据排产版本号，更新排产月份模式及排产开始、结束日信息
     *
     * @param updateVersion
     * @return
     */
    int updateProductionVersionInfo(FactoryProductionVersion updateVersion);

    /**
     * 获取所有施工信息，
     * 主要为胚胎代码及胎体布层级
     *
     * @return
     */
    List<BaseConstructionVersionInfoVo> getBaseConstructionInfo();

    /**
     * 根据分厂、年份、月份，制造需求计划版本，获取分厂不排产的物料
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
     * 根据分厂、年份、月份，制造需求计划版本，获取分厂维修返厂的模具
     *
     * @param factoryCode      分厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MouldMaintenanceConfigurationVo> getFactoryMouldMaintenanceConfiguration(@Param("factoryCode") String factoryCode,
                                                                                  @Param("year") Integer year,
                                                                                  @Param("month") Integer month,
                                                                                  @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 根据分厂、年份、月份，制造需求计划版本，获取分厂维修返厂的模具
     *
     * @param factoryCode      分厂编码
     * @param startDate        开始日期
     * @param endDate          结束日期
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MouldMaintenanceConfigurationVo> getFactoryMouldMaintenanceConfigurationByDateRange(@Param("factoryCode") String factoryCode,
                                                                                             @Param("startDate") Date startDate,
                                                                                             @Param("endDate") Date endDate,
                                                                                             @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 根据制造需求计划版本，获取对应的月度可用模具列表
     *
     * @param factoryCode      分厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MouldInfoVO> getMonthEnableMouldConfiguration(@Param("factoryCode") String factoryCode,
                                                       @Param("year") Integer year,
                                                       @Param("month") Integer month,
                                                       @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 根据制造需求版本及排产版本，删除对应的排产初始化数据
     *
     * @param factoryCode       分厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  制造需求计划版本
     * @param productionVersion 排产版本号
     * @return
     */
    int deleteProductionInitVersion(@Param("factoryCode") String factoryCode,
                                    @Param("year") Integer year,
                                    @Param("month") Integer month,
                                    @Param("monthPlanVersion") String monthPlanVersion,
                                    @Param("productionVersion") String productionVersion);

    /**
     * 根据制造需求版本及排产版本，删除排产版本的排产数据信息
     *
     * @param factoryCode       分厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  制造需求计划版本
     * @param productionVersion 排产版本号
     * @return
     */
    int deleteProductionMouldVersion(@Param("factoryCode") String factoryCode,
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
     * 根据分厂，获取分厂的排产分组信息集合
     *
     * @param factoryCode 分厂编码
     * @return
     */
    List<ProductionGroupVo> getFactoryProductionGroupConfiguration(@Param("factoryCode") String factoryCode);

    /**
     * 根据分厂，获取分厂的成型机台数量
     *
     * @param factoryCode 分厂编码
     * @return
     */
    Integer getFormingMachineCount(@Param("factoryCode") String factoryCode);

    /**
     * 获取分厂在年月上月均销量大于等于averageValue的物料集合
     *
     * @param factoryCode  分厂编码
     * @param year         年份
     * @param month        月份
     * @param averageValue 月均销量
     * @return
     */
    List<ProductAverageSaleVo> getFactoryAverageSaleProduct(@Param("factoryCode") String factoryCode,
                                                            @Param("year") Integer year,
                                                            @Param("month") Integer month,
                                                            @Param("averageValue") Integer averageValue);

    /**
     * 根据需求，获取其最小批量配置
     *
     * @param factoryCode      分厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 销售需求计划版本
     * @return
     */
    List<ProductMinConfiguration> getRequireMinConfiguration(@Param("factoryCode") String factoryCode,
                                                             @Param("year") Integer year,
                                                             @Param("month") Integer month,
                                                             @Param("monthPlanVersion") String monthPlanVersion);
}
