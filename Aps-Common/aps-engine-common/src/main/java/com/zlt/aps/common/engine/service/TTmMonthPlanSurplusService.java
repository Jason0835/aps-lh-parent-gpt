package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TTmMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TTmMonthPlanSurplusService {
    List<TTmMonthPlanSurplus> getByParams(TTmMonthPlanSurplus entity);

    List<TTmMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TTmMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TTmMonthPlanSurplus entity);

    void addBatch(List<TTmMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String vpsVersion);

    int update(TTmMonthPlanSurplus timeLimit);

    TTmMonthPlanSurplus getById(Long id);
}
