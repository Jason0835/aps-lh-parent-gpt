package com.zlt.aps.cx.vo;

import lombok.Data;

/**
 * 单轮均衡执行结果 - 包装 BalancingResult 与完整度信息，供两轮编排决策。
 *
 * @author APS Team
 */
@Data
public class RoundExecutionResult {
    /** 均衡分配结果 */
    private BalancingResult result;
    /** DFS 实际分配的硫化机台数（不含续作预扣/保底预留） */
    private int dfsAssignedCount;
    /** 已满足的预占负荷（仅保底预留部分；续作预扣不代表已满足需求，不计入） */
    private int preOccupiedLoad;
    /** 原始总需求（所有任务 vulcanizeMachineCount 快照之和） */
    private int totalOriginalDemand;
    /** DFS 搜索次数 */
    private int searchCount;
    /** DFS 剪枝次数 */
    private int pruneCount;

    /**
     * 判断本轮是否获得完整解：保底预留已满足负荷 + DFS分配数 >= 原始总需求。
     * <p>完整解 = 所有任务的硫化机台数都已分配到机台（含保底预留/DFS分配）。
     * <p>注意：续作预扣（continuePreloadLoad）不计入已满足负荷，因为续作任务的
     * vulcanizeMachineCount 已在 ContinueTaskProcessor 中扣减保底预留量，
     * 传入 BalancingService 的 totalDemand 已是扣减后的剩余需求。
     */
    public boolean isComplete() {
        return (preOccupiedLoad + dfsAssignedCount) >= totalOriginalDemand;
    }
}
