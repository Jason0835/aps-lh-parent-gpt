package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TLbcdMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TLbcdMonthPlanSurplusService {
    List<TLbcdMonthPlanSurplus> getByParams(TLbcdMonthPlanSurplus entity);

    List<TLbcdMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TLbcdMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TLbcdMonthPlanSurplus entity);

    void addBatch(List<TLbcdMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int update(TLbcdMonthPlanSurplus timeLimit);

    TLbcdMonthPlanSurplus getById(Long id);
}
