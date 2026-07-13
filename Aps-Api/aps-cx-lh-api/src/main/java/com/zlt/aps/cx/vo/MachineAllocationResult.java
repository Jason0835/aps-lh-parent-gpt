package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;

/**
 * 单台成型机的任务分配结果 - Processor 层输出，精排层输入。
 *
 * <p>一台机一条记录，{@link #taskAllocations} 承载该机本班待精排的多个任务。
 *
 * @author APS Team
 */
@Data
public class MachineAllocationResult {
    /** 成型机台编码 */
    private String machineCode;
    /** 机台名称（展示） */
    private String machineName;
    /** 机型编码 */
    private String machineType;
    /** 日产能（条/天，来自机台主数据） */
    private Integer dailyCapacity;
    /** 已占用容量（硫化机台数或预留负荷） */
    private Integer usedCapacity;
    /** 剩余容量 */
    private Integer remainingCapacity;
    /** 已分配胎胚种类数（DFS 种类槽统计） */
    private Integer assignedTypes;
    /** 本机待精排任务列表 */
    private List<TaskAllocation> taskAllocations;
    /** 当前结构（可选，部分路径标记机台所属结构） */
    private String currentStructure;
}
