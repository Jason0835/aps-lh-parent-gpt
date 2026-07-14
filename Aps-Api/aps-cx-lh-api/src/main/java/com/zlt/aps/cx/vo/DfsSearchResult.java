package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;

/**
 * DFS 搜索过程的全局最优解记录（单次 balance 调用内共享，由 dfsAssign 读写）。
 *
 * @author APS Team
 */
@Data
public class DfsSearchResult {
    /** 最优解均衡分数（越小越优，仅同完整度/均衡等级内比较） */
    public int bestScore;
    /** 最优解已分配硫化机台数（完整度第一优先） */
    public int bestAssignedCount;
    /** 最优解是否满足 isBalanced 阈值 */
    public boolean bestIsBalanced;
    /** 最优解各机台分配明细（按机台索引，与 bestMachineCodes 对齐） */
    public List<List<EmbryoAssignment>> bestAssignments;
    /** 与 bestAssignments 对应的机台编码列表 */
    public List<String> bestMachineCodes;
    public int searchCount;
    public int pruneCount;
    public int callCount;
    /** DFS 新分配的 assignedQty 之和（不含续作预扣/保底预留），替代 currentLoad 计算 totalAssigned */
    public int dfsAssignedQty;
    /**
     * 当前轮次配置 - DFS 内部通过此字段读取轮次策略（容量管控/历史偏好等），
     * 避免在 dfsAssign 递归签名中增加参数，6处递归调用无需改动。
     */
    public BalancingRoundConfig config;
}
