package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis
 */
public interface MdmMonthPlanAnalysisMapper {

    int deleteByPrimaryKey(Long id);

    int insert(MdmMonthPlanAnalysis record);

    int insertBatch(@Param("mdmMonthPlanAnalysisCollection") List<MdmMonthPlanAnalysis> mdmMonthPlanAnalysisCollection);

    int insertSelective(MdmMonthPlanAnalysis record);

    MdmMonthPlanAnalysis selectByPrimaryKey(Long id);

    List<MdmMonthPlanAnalysis> getByParams(MdmMonthPlanAnalysis entity);

    int updateByPrimaryKeySelective(MdmMonthPlanAnalysis record);

    int updateByPrimaryKey(MdmMonthPlanAnalysis record);

    int deleteByIds(Long[] ids);

    int deleteByMonthPlanApsVersion(@Param("monthPlanApsVersion") String monthPlanApsVersion);

}




