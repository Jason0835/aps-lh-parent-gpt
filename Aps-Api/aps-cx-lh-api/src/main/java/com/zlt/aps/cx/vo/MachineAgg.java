package com.zlt.aps.cx.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机台汇总信息（机台级三轮精排用）。
 *
 * <p>维度：结构 -> 机台 -> 胎胚任务。试制/量试/收尾任务不参与精排
 * （{@link #excludedTasks}），但其排量占用机台物理产能。
 *
 * @author APS Team
 */
public class MachineAgg {
    /** 机台编码 */
    public final String machineCode;
    /** 实际排量（条，含所有任务） */
    public int actualQty;
    /** 硫化机台数（负荷，含所有任务） */
    public int load;
    /** 可参与精排任务的总排量（条，排除试制/量试/收尾） */
    public int eligibleQty;
    /** 可参与任务列表 */
    public final List<TaskAllocation> eligibleTasks = new ArrayList<>();
    /** 排除任务列表（试制/量试/收尾，量不变但占用机台产能） */
    public final List<TaskAllocation> excludedTasks = new ArrayList<>();
    /** 胎胚编码 -> 可参与任务（同胎胚取首条） */
    public final Map<String, TaskAllocation> embryoTaskMap = new LinkedHashMap<>();

    public MachineAgg(String machineCode) {
        this.machineCode = machineCode;
    }
}
