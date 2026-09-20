package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 未来停产需求前移分配明细。
 *
 * <p>该模型记录需求来源班次和实际承接班次，不使用目标任务被复制后的班次反推
 * 来源收尾，避免前移后把目标班次误当成成型需求班次。</p>
 */
@Data
public class ScheduleFutureDemandAllocationModel {

    /**
     * 已分配状态。
     */
    public static final String STATUS_ALLOCATED = "ALLOCATED";
    /**
     * 无可用承接班次状态。
     */
    public static final String STATUS_NO_TARGET = "NO_TARGET";

    /**
     * 来源唯一键。
     */
    private String sourceKey;
    /**
     * 胎面或胎侧代码。
     */
    private String processCode;
    /**
     * 本笔需求实际对应的成型班次。
     */
    private List<ScheduleFormingShiftKey> sourceDemandShiftKeys = new ArrayList<>();
    /**
     * 当前排程生产班次；无承接班次时为空。
     */
    private ScheduleFormingShiftKey targetProductionShiftKey;
    /**
     * 分配给目标班次的数量，单位米。
     */
    private BigDecimal allocatedQty;
    /**
     * 分配状态。
     */
    private String allocationStatus;

    /**
     * 设置来源需求班次并复制容器。
     *
     * @param sourceDemandShiftKeys 来源需求班次
     */
    public void setSourceDemandShiftKeys(List<ScheduleFormingShiftKey> sourceDemandShiftKeys) {
        this.sourceDemandShiftKeys = sourceDemandShiftKeys == null
                ? new ArrayList<>() : new ArrayList<>(sourceDemandShiftKeys);
    }
}
