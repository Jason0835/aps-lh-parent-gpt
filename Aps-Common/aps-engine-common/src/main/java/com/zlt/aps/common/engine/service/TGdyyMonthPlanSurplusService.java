package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.TGdyyMonthPlanSurplus;

import java.util.List;

/**
 * @author Gim
 */
public interface TGdyyMonthPlanSurplusService {

    List<TGdyyMonthPlanSurplus> getByParams(TGdyyMonthPlanSurplus entity);

    List<TGdyyMonthPlanSurplus> getByApsVersion(String apsVersion);

    List<TGdyyMonthPlanSurplus> getByCodeList(String apsVersion, List<String> codeList);

    void deleteByApsVersionAndCodeList(String apsVersion, List<String> codeList);

    int add(TGdyyMonthPlanSurplus entity);

    void addBatch(List<TGdyyMonthPlanSurplus> list);

    int deleteByIds(Long[] ids);

    int deleteByApsVersion(String apsVersion);

    int update(TGdyyMonthPlanSurplus timeLimit);

    TGdyyMonthPlanSurplus getById(Long id);
}
