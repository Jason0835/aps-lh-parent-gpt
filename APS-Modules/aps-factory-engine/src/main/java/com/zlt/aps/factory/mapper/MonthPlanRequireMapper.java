package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 月度需求计划业务SQL接口定义
 *
 * @author ZLT
 * @date 20251210
 */
@Mapper
public interface MonthPlanRequireMapper extends CommBaseMapper<DpDemandPlan> {

}