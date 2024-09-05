package com.zlt.aps.common.engine.mapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.TXwyyMonthPlanSurplus;

import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TXwyyMonthPlanSurplus
 */
public interface TXwyyMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TXwyyMonthPlanSurplus record);

    int insertSelective(TXwyyMonthPlanSurplus record);

    TXwyyMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TXwyyMonthPlanSurplus record);

    int updateByPrimaryKey(TXwyyMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int insertBatch(@Param("tXwyyMonthPlanSurplusCollection") List<TXwyyMonthPlanSurplus> tXwyyMonthPlanSurplusCollection);

    List<TXwyyMonthPlanSurplus> getByParams(TXwyyMonthPlanSurplus entity);

    List<TXwyyMonthPlanSurplus> selectAllByMonthPlanApsVersionAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("delFlag") String delFlag);

    List<TXwyyMonthPlanSurplus> selectAllByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);

    int deleteByMonthPlanApsVersionAndMaterialCodeIn(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList);
    int updateDelFlagByMonthPlanApsVersionAndMaterialCodeInAndDelFlag(@Param("monthPlanApsVersion") String monthPlanApsVersion, @Param("materialCodeList") Collection<String> materialCodeList, @Param("delFlag") String delFlag);
}




