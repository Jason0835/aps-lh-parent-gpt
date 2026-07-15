package com.zlt.aps.cx.vo;

/**
 * 硫化任务需求（简化版）。
 *
 * @author APS Team
 */
public class TaskDemandSimple {
    public String taskKey;
    public int demand;
    public String materialCode;
    public String productStatus;

    public TaskDemandSimple(Long lhId, int demand, String materialCode, String productStatus) {
        this.taskKey = String.valueOf(lhId);
        this.demand = demand;
        this.materialCode = materialCode;
        this.productStatus = productStatus;
    }
}
