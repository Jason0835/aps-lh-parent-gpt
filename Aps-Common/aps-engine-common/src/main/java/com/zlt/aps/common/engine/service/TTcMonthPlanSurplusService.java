package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TTcMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TTcMonthPlanSurplusService {
    List<TTcMonthPlanSurplus> getByParams(TTcMonthPlanSurplus entity);

    List<TTcMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TTcMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TTcMonthPlanSurplus entity);

    void addBatch(List<TTcMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String vpsVersion);

    int update(TTcMonthPlanSurplus timeLimit);

    TTcMonthPlanSurplus getById(Long id);
}
