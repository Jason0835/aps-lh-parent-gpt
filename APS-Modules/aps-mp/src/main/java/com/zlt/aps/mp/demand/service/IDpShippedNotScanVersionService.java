package com.zlt.aps.mp.demand.service;

import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface IDpShippedNotScanVersionService extends IDocService<DpShippedNotScanVersion> {

    void generateShippedNotScanVersion(DpShippedNotScanVersion queryCondition);

    List<String> findMonthPlanVersion(DpShippedNotScanVersion queryCondition);
}
