package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TGdcdMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TGdcdMonthPlanSurplus
 */
public interface TGdcdMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TGdcdMonthPlanSurplus record);

    int insertSelective(TGdcdMonthPlanSurplus record);

    TGdcdMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TGdcdMonthPlanSurplus record);

    int updateByPrimaryKey(TGdcdMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tGdcdMonthPlanSurplusCollection") List<TGdcdMonthPlanSurplus> tGdcdMonthPlanSurplusCollection);

    List<TGdcdMonthPlanSurplus> getByParams(TGdcdMonthPlanSurplus entity);

    List<TGdcdMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TGdcdMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
}




