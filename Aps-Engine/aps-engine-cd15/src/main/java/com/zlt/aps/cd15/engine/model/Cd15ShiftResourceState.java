package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 当前斜裁班次可原子替换的内存资源状态。 */
@Data
@Builder
public class Cd15ShiftResourceState {
    /** 当前班次库排状态。 */
    private List<Cd15StorageLaneState> lanes;
    /** 工装总数。 */
    private int totalToolingCount;
    /** 当前占用工装数。 */
    private int occupiedToolingCount;
    /** 各机台剩余可用秒数。 */
    private Map<String, Integer> remainingSecondsByMachine;
    /** 各机台任务链尾帘线规格。 */
    private Map<String, String> tailSpecByMachine;
    /** 各机台任务链尾大卷与斜裁规格。 */
    private Map<String, Cd15MachineTailState> tailByMachine;
    /** 当前班次已提交任务链。 */
    private List<Cd15ShiftScheduleTask> tasks;
    /** 当前排程上下文内的大卷成熟流水，提交成功后滚动扣减。 */
    private List<Cd15BigRollAgingStock> bigRollAgingStocks;
}
