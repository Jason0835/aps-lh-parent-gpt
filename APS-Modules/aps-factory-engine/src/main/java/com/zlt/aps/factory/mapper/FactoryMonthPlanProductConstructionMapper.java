package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.vo.MonthPlanProductConstructionInfoVo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度计划-施工相关业务SQL接口定义
 *
 * @author ZLT
 * @date 20251210
 */
@Mapper
public interface FactoryMonthPlanProductConstructionMapper {


    /**
     * 根据工厂、年份、月份、制造需求计划版本，获取对应制造需求版本对应的物料与施工关系信息
     *
     * @param factoryCode      工厂
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 制造需求计划版本
     * @return
     */
    List<MonthPlanProductConstructionInfoVo> getConstructionByRequire(@Param("factoryCode") String factoryCode,
                                                                      @Param("year") Integer year,
                                                                      @Param("month") Integer month,
                                                                      @Param("monthPlanVersion") String monthPlanVersion);
}
