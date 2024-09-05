package com.zlt.aps.common.engine.mapper;
import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @Entity com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus
 */
public interface TCxEmbryoMonthPlanSurplusMapper {

    int deleteByPrimaryKey(Long id);

    int insert(TCxEmbryoMonthPlanSurplus record);

    int insertSelective(TCxEmbryoMonthPlanSurplus record);

    TCxEmbryoMonthPlanSurplus selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TCxEmbryoMonthPlanSurplus record);

    int updateByPrimaryKey(TCxEmbryoMonthPlanSurplus record);

    int deleteByIds(Long[] ids);

    int insertBatch(@Param("tCxEmbryoMonthPlanSurplusCollection") List<TCxEmbryoMonthPlanSurplus> tCxEmbryoMonthPlanSurplusCollection);

    List<TCxEmbryoMonthPlanSurplus> getByParams(TCxEmbryoMonthPlanSurplus entity);

    int deleteByApsVersion(String apsVersion);

    List<TCxEmbryoMonthPlanSurplus> selectAllByMaterialCodeInAndYearAndMonth(@Param("materialCodeList") Collection<String> materialCodeList, @Param("year") String year, @Param("month") String month);

    List<TCxEmbryoMonthPlanSurplus> selectAllByMaterialCodeInAndMonthPlanApsVersionAndDelFlag(@Param("materialCodeList") Collection<String> materialCodeList, @Param("monthPlanApsVersion") String monthPlanApsVersion);

    void mergeSql(List<TCxEmbryoMonthPlanSurplus> list);

    List<EmbryoVersionVo> getInsertByApsVersion(@Param("apsVersion") String apsVersion);

    /**
     * 根据年月查询所有胎胚对应的月度剩余量
     * @param year 年
     * @param month 月
     * @return 结果
     */
    public List<TCxEmbryoMonthPlanSurplus> selectMonthRemainQtyByYearAndMonthGroupByMaterialCode(@Param("year") String year, @Param("month") String month);
}




