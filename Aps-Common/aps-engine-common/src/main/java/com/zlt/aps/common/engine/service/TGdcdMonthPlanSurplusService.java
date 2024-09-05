package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TGdcdMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TGdcdMonthPlanSurplusService {
    List<TGdcdMonthPlanSurplus> getByParams(TGdcdMonthPlanSurplus entity);

    List<TGdcdMonthPlanSurplus> getByApsVersion(String vpsVersion);

    List<TGdcdMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TGdcdMonthPlanSurplus entity);

    void addBatch(List<TGdcdMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int update(TGdcdMonthPlanSurplus timeLimit);

    TGdcdMonthPlanSurplus getById(Long id);
}
