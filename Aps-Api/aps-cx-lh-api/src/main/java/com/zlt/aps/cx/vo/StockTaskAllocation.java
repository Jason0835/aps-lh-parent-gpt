package com.zlt.aps.cx.vo;

import lombok.Data;

/**
 * 库存分配追踪，用于多任务胎胚的多轮分配
 *
 * @author APS Team
 */
@Data
public class StockTaskAllocation {
    private String taskKey;
    private int demand;
    private String materialCode;
    private int surplus;      // 硫化余量上限
    private int allocated;    // 已分配量

    public StockTaskAllocation(TaskDemand td, int allocated, int surplus) {
        this.taskKey = td.getTaskKey();
        this.demand = td.getDemand();
        this.materialCode = td.getMaterialCode();
        this.allocated = allocated;
        this.surplus = surplus;
    }
}
