package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.vo.MatchingProductionConfigurationVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.domain.vo.MouldMaintenanceConfigurationVo;
import com.zlt.aps.factory.domain.vo.ProductMouldConfigurationVo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 月份排产-搭配排产需求接口信息
 *
 * @author ZLT
 * @date 20250829
 */
@Mapper
public interface FactoryMatchingProductionMapper {
    /**
     * 根据分厂、年份、月份、制造需求计划版本，获取分厂搭配排产配置信息
     *
     * @param factoryCode      分厂
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MatchingProductionConfigurationVo> getMatchingConfiguration(@Param("factoryCode") String factoryCode,
                                                                     @Param("year") Integer year,
                                                                     @Param("month") Integer month,
                                                                     @Param("monthPlanVersion") String monthPlanVersion);


    /**
     * 根据分厂、年份、月份、制造需求计划版本，获取对应搭配排产物料与施工关系信息
     *
     * @param factoryCode      分厂
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MdmProductConstruction> getConstructionByMatchingRequire(@Param("factoryCode") String factoryCode,
                                                          @Param("year") Integer year,
                                                          @Param("month") Integer month,
                                                          @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 根据分厂、年份、月份，制造需求计划版本，获取对应搭配排产物料对应的分厂维修返厂的模具
     *
     * @param factoryCode      分厂编码
     * @param startDate        开始日期
     * @param endDate          结束日期
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MouldMaintenanceConfigurationVo> getMouldMaintenanceConfigurationByMatchingRequireDateRange(@Param("factoryCode") String factoryCode,
                                                                                             @Param("startDate") Date startDate,
                                                                                             @Param("endDate") Date endDate,
                                                                                             @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 根据制造需求计划版本，获取搭配需求对应的月度可用模具列表
     *
     * @param factoryCode      分厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MouldInfoVO> getMonthEnableMouldConfigurationByMatchingRequire(@Param("factoryCode") String factoryCode,
                                                       @Param("year") Integer year,
                                                       @Param("month") Integer month,
                                                       @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 根据制造需求计划版本，获取对应的物料、模具关系
     *
     * @param factoryCode      分厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<ProductMouldConfigurationVo> getProductionMouldRelationByMatchingRequire(@Param("factoryCode") String factoryCode,
                                                                 @Param("year") Integer year,
                                                                 @Param("month") Integer month,
                                                                 @Param("monthPlanVersion") String monthPlanVersion);
}
