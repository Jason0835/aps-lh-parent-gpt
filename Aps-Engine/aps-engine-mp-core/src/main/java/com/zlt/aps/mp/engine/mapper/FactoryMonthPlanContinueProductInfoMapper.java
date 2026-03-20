package com.zlt.aps.mp.engine.mapper;

import com.zlt.aps.mp.engine.domain.dto.ContinueGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.ContinueProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度计划-续作规格业务SQL接口定义
 *
 * @author ZLT
 * @date 20251210
 */
@Mapper
public interface FactoryMonthPlanContinueProductInfoMapper {
    /**
     * 根据上个月最后一天的排产信息，获得对应在产的Sku信息
     * 即续作Sku信息
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    List<ContinueProductInfo> getContinueProductInfo(@Param("factoryCode") String factoryCode,
                                                     @Param("year") Integer year,
                                                     @Param("month") Integer month,
                                                     @Param("lastDay") Integer lastDay);

    /**
     * 根据上个月最后一天，获取在机结构信息
     *
     * @param factoryCode       工厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求版本号
     * @param productionVersion 排产版本号
     * @param lastDay           最后一天
     * @return
     */
    List<ContinueGroupInfo> getContinueGroupInfo(@Param("factoryCode") String factoryCode,
                                                 @Param("year") Integer year,
                                                 @Param("month") Integer month,
                                                 @Param("monthPlanVersion") String monthPlanVersion,
                                                 @Param("productionVersion") String productionVersion,
                                                 @Param("lastDay") Integer lastDay);
}
