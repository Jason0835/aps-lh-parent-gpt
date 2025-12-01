package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 制造需求计划SQL接口
 *
 * @author ZLT
 * @date 20250308
 */
@Mapper
public interface MonthPlanRequireMapper extends CommBaseMapper<SaleMonthPlanRequire> {

}