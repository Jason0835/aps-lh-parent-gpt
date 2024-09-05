package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TTcMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TTcMonthPlanSurplus
 */
public interface TTcMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TTcMonthPlanSurplus record);

    int insertSelective(TTcMonthPlanSurplus record);

    TTcMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TTcMonthPlanSurplus record);

    int updateByPrimaryKey(TTcMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tTcMonthPlanSurplusCollection") List<TTcMonthPlanSurplus> tTcMonthPlanSurplusCollection);

    List<TTcMonthPlanSurplus> getByParams(TTcMonthPlanSurplus entity);

    List<TTcMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TTcMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
}




