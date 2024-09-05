package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TTmMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TTmMonthPlanSurplus
 */
public interface TTmMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TTmMonthPlanSurplus record);

    int insertSelective(TTmMonthPlanSurplus record);

    TTmMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TTmMonthPlanSurplus record);

    int updateByPrimaryKey(TTmMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tTmMonthPlanSurplusCollection") List<TTmMonthPlanSurplus> tTmMonthPlanSurplusCollection);

    List<TTmMonthPlanSurplus> getByParams(TTmMonthPlanSurplus entity);

    List<TTmMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TTmMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") List<String> materialCodeList, @Param("delFlag") String delFlag);

    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
}




