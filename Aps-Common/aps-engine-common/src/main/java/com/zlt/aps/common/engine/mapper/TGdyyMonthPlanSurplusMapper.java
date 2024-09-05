package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;


import com.zlt.aps.common.engine.domain.TGdyyMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TGdyyMonthPlanSurplus
 */
public interface TGdyyMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TGdyyMonthPlanSurplus record);

    int insertSelective(TGdyyMonthPlanSurplus record);

    TGdyyMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TGdyyMonthPlanSurplus record);

    int updateByPrimaryKey(TGdyyMonthPlanSurplus record);
    
    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tGdyyMonthPlanSurplusCollection") List<TGdyyMonthPlanSurplus> tGdyyMonthPlanSurplusCollection);

    List<TGdyyMonthPlanSurplus> getByParams(TGdyyMonthPlanSurplus entity);

    List<TGdyyMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TGdyyMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
}




