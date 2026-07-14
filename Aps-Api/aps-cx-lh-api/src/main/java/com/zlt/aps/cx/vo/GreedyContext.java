package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 贪心分配前置准备结果 holder。
 *
 * @author APS Team
 */
@Data
public class GreedyContext {
    /** 初始化后的机台状态列表（含续作预扣） */
    private List<MachineState> machineStates;
    /** 胎胚 -> 任务列表映射 */
    private Map<String, List<DailyEmbryoTask>> embryoGroups;
    /** 已排序的胎胚编码列表（小需求优先） */
    private List<String> sortedEmbryos;
    /** 目标种类数（每台机平均种类数的上取整） */
    private int targetTypes;
    /** 目标负荷（含续作预扣的总负荷均摊值） */
    private int targetLoad;
}
