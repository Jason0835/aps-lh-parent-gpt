package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;

public interface ITcMachineMaintenanceService extends IDocService<TcMachineMaintenance> {

    /**
     * 根据停机开始时间解析班次
     *
     * @param stopStartTime 停机开始时间
     * @return 班次编码
     */
    String resolveStopShift(Date stopStartTime);
}