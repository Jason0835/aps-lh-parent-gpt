package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.dto.ContinueGroupInfo;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
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
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @param lastDay     最后一天
     * @return
     */
    List<ContinueGroupInfo> getContinueGroupInfo(@Param("factoryCode") String factoryCode,
                                                 @Param("year") Integer year,
                                                 @Param("month") Integer month,
                                                 @Param("lastDay") Integer lastDay);
}
