package com.zlt.aps.mp.demand.mapper;

import com.zlt.aps.mp.api.domain.entity.DpDemandPlanSum;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanSumMapper.java
 * 描    述：需求计划汇总Mapper接口
 *@author yelq
 *@date 2026-01-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Mapper
public interface DpDemandPlanSumEntityMapper extends CommBaseMapper<DpDemandPlanSum> {
  /**
   * 直接查询去重后的month_plan_version列表
   */
  @Select("SELECT MONTH_PLAN_VERSION " +
      " FROM T_DP_DEMAND_PLAN_SUM " +
      " WHERE FACTORY_CODE = #{factoryCode} " +
      " AND `YEAR` = #{year} " +
      " AND `MONTH` = #{month} " +
      " AND `PLAN_TYPE` = #{planType} " +
      " AND IS_DELETE = #{isDelete} " +
      " GROUP BY MONTH_PLAN_VERSION " +
      " ORDER BY MAX(CREATE_TIME) DESC ")
  List<String> selectDistinctMonthPlanVersion(@Param("factoryCode") String factoryCode,
                                              @Param("year") Integer year,
                                              @Param("month") Integer month,
                                              @Param("month") String planType,
                                              @Param("isDelete") Integer isDelete);
}
