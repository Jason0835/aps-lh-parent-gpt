package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TGsqMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TGsqMonthPlanSurplusService {
    List<TGsqMonthPlanSurplus> getByParams(TGsqMonthPlanSurplus entity);

    List<TGsqMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TGsqMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TGsqMonthPlanSurplus entity);

    void addBatch(List<TGsqMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String vpsVersion);

    int update(TGsqMonthPlanSurplus timeLimit);

    TGsqMonthPlanSurplus getById(Long id);
}
