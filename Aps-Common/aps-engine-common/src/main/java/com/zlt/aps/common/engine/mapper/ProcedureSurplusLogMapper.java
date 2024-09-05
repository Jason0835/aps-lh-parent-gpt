package com.zlt.aps.common.engine.mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;

import com.zlt.aps.common.engine.domain.ProcedureSurplusLog;

/**
 * @Entity com.zlt.aps.common.engine.domain.ProcedureSurplusLog
 */
public interface ProcedureSurplusLogMapper {

    int deleteByPrimaryKey(Long id);

    int insert(ProcedureSurplusLog record);

    int insertSelective(ProcedureSurplusLog record);

    ProcedureSurplusLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ProcedureSurplusLog record);

    int updateByPrimaryKey(ProcedureSurplusLog record);

    int insertBatch(@Param("procedureSurplusLogCollection") List<ProcedureSurplusLog> procedureSurplusLogCollection);

    int deleteByApsMonthVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);
}




