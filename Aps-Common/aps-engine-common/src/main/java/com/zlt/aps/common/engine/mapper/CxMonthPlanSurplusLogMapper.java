package com.zlt.aps.common.engine.mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.common.engine.domain.CxMonthPlanSurplusLog;

/**
 * @Entity com.zlt.aps.common.engine.domain.CxMonthPlanSurplusLog
 */
public interface CxMonthPlanSurplusLogMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CxMonthPlanSurplusLog record);

    int insertSelective(CxMonthPlanSurplusLog record);

    CxMonthPlanSurplusLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CxMonthPlanSurplusLog record);

    int updateByPrimaryKey(CxMonthPlanSurplusLog record);

    int insertBatch(@Param("cxMonthPlanSurplusLogCollection") List<CxMonthPlanSurplusLog> cxMonthPlanSurplusLogCollection);
}




