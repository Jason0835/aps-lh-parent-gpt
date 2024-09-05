package com.zlt.aps.common.engine.service;


import com.zlt.aps.common.engine.domain.MdmMonthPlanAnalysis;

import java.util.List;

/**
 * @author Gim
 */
public interface MdmMonthPlanAnalysisService {


    List<MdmMonthPlanAnalysis> getByParams(MdmMonthPlanAnalysis entity);

    int add(MdmMonthPlanAnalysis entity);

    void addBatch(List<MdmMonthPlanAnalysis> list);

    int deleteByIds(Long[] ids);

    int update(MdmMonthPlanAnalysis timeLimit);

    MdmMonthPlanAnalysis getById(Long id);

    void deleteByApsVersion(String apsVersion);
}
