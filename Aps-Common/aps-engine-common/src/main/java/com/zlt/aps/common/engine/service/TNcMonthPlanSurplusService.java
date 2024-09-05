package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TNcMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TNcMonthPlanSurplusService {
    List<TNcMonthPlanSurplus> getByParams(TNcMonthPlanSurplus entity);

    List<TNcMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TNcMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TNcMonthPlanSurplus entity);

    void addBatch(List<TNcMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String vpsVersion);

    int update(TNcMonthPlanSurplus timeLimit);

    TNcMonthPlanSurplus getById(Long id);
}
