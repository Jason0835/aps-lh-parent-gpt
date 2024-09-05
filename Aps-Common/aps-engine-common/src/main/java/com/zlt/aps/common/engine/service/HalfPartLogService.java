package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.CxMonthPlanSurplusLog;
import com.zlt.aps.common.engine.domain.ProcedureSurplusLog;

import java.util.List;

/**
 * @author Gim
 */
public interface HalfPartLogService {
    // 半部件+胎胚
    void addCxHalfPartLog(List<ProcedureSurplusLog> list);

    // 外胎
    void addLhLog(List<CxMonthPlanSurplusLog> list);

    /**
     * 将原有版本在历史表中的数据进行逻辑删除
     * @param toDeleteVersion
     */
    void removeHistoryVersion(String toDeleteVersion);
}
