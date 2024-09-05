package com.zlt.aps.common.engine.mapper;
import java.util.Collection;

import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus
 */
public interface TCxMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TCxMonthPlanSurplus record);

    int insertSelective(TCxMonthPlanSurplus record);

    TCxMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TCxMonthPlanSurplus record);

    int updateByPrimaryKey(TCxMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int insertBatch(@Param("tCxMonthPlanSurplusCollection") List<TCxMonthPlanSurplus> tCxMonthPlanSurplusCollection);

    List<TCxMonthPlanSurplus> getByParams(TCxMonthPlanSurplus entity);

    List<TCxMonthPlanSurplus> selectAllBySapCodeInAndYearAndMonth(@Param("sapCodeList") Collection<String> sapCodeList, @Param("year") String year, @Param("month") String month);

    List<TCxMonthPlanSurplus> selectAllBySapCodeInAndMonthPlanApsVersionAndDelFlag(@Param("sapCodeList") Collection<String> sapCodeList, @Param("monthPlanApsVersion") String monthPlanApsVersion);

    int deleteByApsVersion(String apsVersion);

    void mergeSql(List<TCxMonthPlanSurplus> list);
}




