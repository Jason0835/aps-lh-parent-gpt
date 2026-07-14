package com.zlt.aps.cx.vo;

/**
 * 任务分配结果（调拨均衡用）。
 *
 * @author APS Team
 */
public class TaskAllocationR {
    public String taskKey;
    public int demand;
    public String materialCode;
    public int surplus;      // 硫化余量上限
    public int allocated;    // 已分配量

    public TaskAllocationR(TaskDemandSimple td, int allocated, int surplus) {
        this.taskKey = td.taskKey;
        this.demand = td.demand;
        this.materialCode = td.materialCode;
        this.allocated = allocated;
        this.surplus = surplus;
    }
}
