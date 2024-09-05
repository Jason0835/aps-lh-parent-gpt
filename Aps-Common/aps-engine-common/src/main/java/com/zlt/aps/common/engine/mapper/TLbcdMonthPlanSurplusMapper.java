package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TLbcdMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TLbcdMonthPlanSurplus
 */
public interface TLbcdMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TLbcdMonthPlanSurplus record);

    int insertSelective(TLbcdMonthPlanSurplus record);

    TLbcdMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TLbcdMonthPlanSurplus record);

    int updateByPrimaryKey(TLbcdMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tLbcdMonthPlanSurplusCollection") List<TLbcdMonthPlanSurplus> tLbcdMonthPlanSurplusCollection);

    List<TLbcdMonthPlanSurplus> getByParams(TLbcdMonthPlanSurplus entity);

    List<TLbcdMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TLbcdMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);

}




