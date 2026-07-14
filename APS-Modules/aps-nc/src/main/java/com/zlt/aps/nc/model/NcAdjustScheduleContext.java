package com.zlt.aps.nc.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.api.domain.entity.NcSpecifyMachine;

import lombok.Data;

/**
 * 内衬排程调整上下文：公共数据预加载结果
 *
 * @author zlt
 */
@Data
public class NcAdjustScheduleContext {
    private String factoryCode;
    private Date scheduleDate;
    private List<NcMachineInfo> machineList;
    private Map<String, NcMachineInfo> machineMap;
    private List<NcScheduleResult> scheduleResults;
    private List<NcSpecifyMachine> specifyMachines;
    private int publishRecordCount;
}
