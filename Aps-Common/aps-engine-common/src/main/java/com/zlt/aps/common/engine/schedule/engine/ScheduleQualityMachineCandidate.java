package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.Map;

/** 自动排程质量计算需要的候选机台契约。 */
public interface ScheduleQualityMachineCandidate {

    String getMachineCode();

    Boolean getEnabled();

    BigDecimal getMaxCapacity();

    BigDecimal getMachineSpeed();

    Map<Integer, BigDecimal> getMaintenanceHoursByShift();
}

