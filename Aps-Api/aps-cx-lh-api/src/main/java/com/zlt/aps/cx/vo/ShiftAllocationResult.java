package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 机台班次计划汇总结构。
 *
 * <p>当前实现中精排结果以 ShiftProductionResult 为主路径输出；
 * 本类保留作班次维度聚合的扩展占位。
 *
 * @author APS Team
 */
@Data
public class ShiftAllocationResult {
    /** 机台编码 */
    private String machineCode;
    /** 机台名称 */
    private String machineName;
    /** 班次编码 -> 计划条数 */
    private Map<String, Integer> shiftPlanQty;
    /** 关联的任务分配列表 */
    private List<TaskAllocation> tasks;
}
