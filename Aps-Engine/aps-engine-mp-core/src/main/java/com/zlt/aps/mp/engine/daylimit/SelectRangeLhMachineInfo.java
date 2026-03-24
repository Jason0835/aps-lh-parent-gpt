package com.zlt.aps.mp.engine.daylimit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 选择一段排产的硫化组信息
 *
 * @author ZLT
 * @date 20260323
 */
@Slf4j
@Getter
public class SelectRangeLhMachineInfo {

    private GroupPlanCxLhCapacityLimitHelper startDayLimit;

    private GroupPlanCxLhCapacityLimitHelper endDayLimit;

    /**
     * 构造函数
     *
     * @param startDayLimit
     * @param endDayLimit
     */
    public SelectRangeLhMachineInfo(GroupPlanCxLhCapacityLimitHelper startDayLimit, GroupPlanCxLhCapacityLimitHelper endDayLimit) {
        this.startDayLimit = startDayLimit;
        this.endDayLimit = endDayLimit;
    }
}
