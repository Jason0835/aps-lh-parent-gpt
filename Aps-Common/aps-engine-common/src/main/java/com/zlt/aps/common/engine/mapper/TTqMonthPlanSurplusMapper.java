package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TTqMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TTqMonthPlanSurplus
 */
public interface TTqMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TTqMonthPlanSurplus record);

    int insertSelective(TTqMonthPlanSurplus record);

    TTqMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TTqMonthPlanSurplus record);

    int updateByPrimaryKey(TTqMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tTqMonthPlanSurplusCollection") List<TTqMonthPlanSurplus> tTqMonthPlanSurplusCollection);

    List<TTqMonthPlanSurplus> getByParams(TTqMonthPlanSurplus entity);

    List<TTqMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TTqMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
}




