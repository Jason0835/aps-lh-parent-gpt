package com.zlt.aps.cx.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机台汇总信息（调拨均衡用）。
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
    /** 合理量（条，整车对齐） */
    public int reasonable;
    /** 调拨目标（条）= 合理量钳制到跨班次合法区间，无历史时=合理量 */
    public int target;
    /** 跨班次合法区间下界（条），hasHistory=true 时有效 */
    public int validLow;
    /** 跨班次合法区间上界（条），hasHistory=true 时有效 */
    public int validHigh;
    /** 是否有可比较的历史班次（同机台+同结构+同负荷） */
    public boolean hasHistory;
    /** 历史中是否存在该机台+结构（负荷可能不同），用于判断负荷是否变化 */
    public boolean historyExisted;
    /** 可参与调拨任务的总排量（条，排除试制/量试/收尾） */
    public int eligibleQty;
    /** 该机台可参与任务中最小 stockHours（代表值） */
    public BigDecimal minStockHours;
    /** 该机台可参与任务中最大 stockHours */
    public BigDecimal maxStockHours;
    /** 可参与任务列表 */
    public final List<TaskAllocation> eligibleTasks = new ArrayList<>();
    /** 胎胚编码 -> 可参与任务（同胎胚取首条） */
    public final Map<String, TaskAllocation> embryoTaskMap = new LinkedHashMap<>();

    public MachineAgg(String machineCode) {
        this.machineCode = machineCode;
    }
}
