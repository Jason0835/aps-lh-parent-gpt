package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 精度计划Mapper
 *
 * @author APS Team
 */
@Mapper
public interface CxPrecisionPlanMapper extends CommBaseMapper<CxPrecisionPlan> {

    List<CxPrecisionPlan> selectPendingWarningPlans(@Param("daysToDue") Integer daysToDue);

    int batchUpdateDaysToDue();
}
