package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TCxMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TCxMonthPlanSurplusService {

    List<TCxMonthPlanSurplus> getByParams(TCxMonthPlanSurplus entity);

    int add(TCxMonthPlanSurplus entity);

    void addBatch(List<TCxMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int update(TCxMonthPlanSurplus entity);

    TCxMonthPlanSurplus getById(Long id);

    int deleteByApsVersion(String apsVersion);

    List<TCxMonthPlanSurplus> getBySapCodeAndYearAndMonth(List<String> sapCodeList, String year, String month);

    List<TCxMonthPlanSurplus> getBySapCodeAndApsVersion(List<String> sapCodeList, String apsVersion);

    void mergeSql(List<TCxMonthPlanSurplus> list);
}
