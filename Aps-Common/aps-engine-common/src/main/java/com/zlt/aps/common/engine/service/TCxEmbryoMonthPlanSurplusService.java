package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.EmbryoVersionVo;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Gim
 */
public interface TCxEmbryoMonthPlanSurplusService {
    List<TCxEmbryoMonthPlanSurplus> getByParams(TCxEmbryoMonthPlanSurplus entity);

    List<EmbryoVersionVo> getEmbryoInsertVo(String apsVersion);

    int add(TCxEmbryoMonthPlanSurplus entity);

    void addBatch(List<TCxEmbryoMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int update(TCxEmbryoMonthPlanSurplus entity);

    TCxEmbryoMonthPlanSurplus getById(Long id);

    int deleteByApsVersion(String apsVersion);

    List<TCxEmbryoMonthPlanSurplus> getByEmbryoListAndYearAndMonth(List<String> embryoList, String year, String month);

    List<TCxEmbryoMonthPlanSurplus> getByEmbryoListAndApsVersion(List<String> embryoList, String apsVersion);

    void mergeSql(List<TCxEmbryoMonthPlanSurplus> list);

    /**
     * 根据年月查询所有胎胚对应的月度剩余量
     * @param year 年
     * @param month 月
     * @return 结果
     */
    public Map<String, BigDecimal> selectMonthRemainQtyByYearAndMonthGroupByMaterialCode(String year, String month);
}
