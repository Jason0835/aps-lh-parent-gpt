package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TGsqMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TGsqMonthPlanSurplus
 */
public interface TGsqMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TGsqMonthPlanSurplus record);

    int insertSelective(TGsqMonthPlanSurplus record);

    TGsqMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TGsqMonthPlanSurplus record);

    int updateByPrimaryKey(TGsqMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tGsqMonthPlanSurplusCollection") List<TGsqMonthPlanSurplus> tGsqMonthPlanSurplusCollection);

    List<TGsqMonthPlanSurplus> getByParams(TGsqMonthPlanSurplus entity);

    List<TGsqMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TGsqMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);

}




