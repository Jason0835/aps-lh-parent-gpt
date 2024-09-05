package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TNcMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TNcMonthPlanSurplus
 */
public interface TNcMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TNcMonthPlanSurplus record);

    int insertSelective(TNcMonthPlanSurplus record);

    TNcMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TNcMonthPlanSurplus record);

    int updateByPrimaryKey(TNcMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tNcMonthPlanSurplusCollection") List<TNcMonthPlanSurplus> tNcMonthPlanSurplusCollection);

    List<TNcMonthPlanSurplus> getByParams(TNcMonthPlanSurplus entity);

    List<TNcMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TNcMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
}




