package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.TXwyyMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TXwyyMonthPlanSurplusService {

    List<TXwyyMonthPlanSurplus> getByParams(TXwyyMonthPlanSurplus entity);

    List<TXwyyMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TXwyyMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TXwyyMonthPlanSurplus entity);

    void addBatch(List<TXwyyMonthPlanSurplus> list);

    int deleteByApsVersion(String vpsVersion);

    int deleteByIds(Long[] ids);

    int update(TXwyyMonthPlanSurplus timeLimit);

    TXwyyMonthPlanSurplus getById(Long id);
}
