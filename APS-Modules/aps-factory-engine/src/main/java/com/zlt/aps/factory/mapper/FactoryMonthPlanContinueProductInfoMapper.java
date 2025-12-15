package com.zlt.aps.factory.mapper;

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
     * 根据需求计划，获取对应的SKU的日硫化量
     * 包含MES日硫化量，标准的日硫化量，APS计算的日硫化量，硫化总时间单位(s)
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

}
