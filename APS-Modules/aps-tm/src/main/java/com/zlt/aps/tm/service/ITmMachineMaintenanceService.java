package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;

public interface ITmMachineMaintenanceService extends IDocService<TmMachineMaintenance> {

    /**
     * 根据停机开始时间解析班次
     *
     * @param stopStartTime 停机开始时间
     * @return 班次编码
     */
    String resolveStopShift(Date stopStartTime);
}
