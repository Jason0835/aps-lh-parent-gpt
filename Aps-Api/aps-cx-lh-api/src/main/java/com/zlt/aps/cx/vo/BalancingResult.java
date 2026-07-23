package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;

/**
 * 均衡分配对外返回结构：机台 -> 多条 EmbryoAssignment。
 *
 * @author APS Team
 */
@Data
public class BalancingResult {
    private List<MachineAssignment> assignments;

    /** 均衡算法路径摘要，如 GREEDY_R1 / GREEDY_R2 */
    private String algorithmPath;
    /** 新增任务硫化机台数是否全部分配完 */
    private Boolean allAssigned;
    /** 分配后机台间最大负荷差（含续作预扣） */
    private Integer loadGap;
    /** 分配后机台间最大种类差（含续作预扣） */
    private Integer typeGap;
    /** P1 是否满足：全部分配 + 负荷差≤阈值 + 种类差≤阈值 */
    private Boolean p1Satisfied;
}
