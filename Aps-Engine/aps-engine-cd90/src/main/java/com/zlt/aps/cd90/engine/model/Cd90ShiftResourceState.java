package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 当前直裁班次可原子替换的内存资源状态。 */
@Data
@Builder
public class Cd90ShiftResourceState {
    /** 当前班次库排状态。 */
    private List<Cd90StorageLaneState> lanes;
    /** 工装总数。 */
    private int totalToolingCount;
    /** 当前占用工装数。 */
    private int occupiedToolingCount;
    /** 各机台剩余可用秒数。 */
    private Map<String, Integer> remainingSecondsByMachine;
    /** 各机台任务链尾帘线规格。 */
    private Map<String, String> tailSpecByMachine;
    /** 当前班次已提交任务链。 */
    private List<Cd90ShiftScheduleTask> tasks;
}
