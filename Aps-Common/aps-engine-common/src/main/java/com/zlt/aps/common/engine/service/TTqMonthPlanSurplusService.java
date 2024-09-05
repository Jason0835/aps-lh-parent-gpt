package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TTqMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TTqMonthPlanSurplusService {
    List<TTqMonthPlanSurplus> getByParams(TTqMonthPlanSurplus entity);

    List<TTqMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TTqMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TTqMonthPlanSurplus entity);

    void addBatch(List<TTqMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String vpsVersion);

    int update(TTqMonthPlanSurplus timeLimit);

    TTqMonthPlanSurplus getById(Long id);
}
