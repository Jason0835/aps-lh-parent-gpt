package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.vo.ProductBaseInfoVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度计划-物料基础业务SQL接口定义
 *
 * @author ZLT
 * @date 20251211
 */
@Mapper
public interface FactoryMonthPlanProductInfoMapper {


    /**
     * 根据工厂、年份、月份、制造需求计划版本，获取对应物料基础数据信息
     *
     * @param factoryCode      工厂
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<ProductBaseInfoVo> getProductionMaterialInfo(@Param("factoryCode") String factoryCode,
                                                      @Param("year") Integer year,
                                                      @Param("month") Integer month,
                                                      @Param("monthPlanVersion") String monthPlanVersion);
}
