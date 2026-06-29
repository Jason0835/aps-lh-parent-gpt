package com.zlt.aps.dj.model;

import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.api.domain.entity.DjSpecifyMachine;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 垫胶排程调整上下文：公共数据预加载结果
 *
 * @author zlt
 */
@Data
public class DjAdjustScheduleContext {
    private String factoryCode;
    private Date scheduleDate;
    private List<DjMachineInfo> machineList;
    private Map<String, DjMachineInfo> machineMap;
    private List<DjScheduleResult> scheduleResults;
    private List<DjSpecifyMachine> specifyMachines;
    private int publishRecordCount;
}
