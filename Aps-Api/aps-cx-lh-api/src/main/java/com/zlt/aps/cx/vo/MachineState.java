package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 机台状态（DFS 均衡分配过程中的机台状态记录）。
 *
 * @author APS Team
 */
@Data
public class MachineState {
    private String machineCode;
    private int maxCapacity;
    private int maxTypes;
    private int currentLoad;
    private int currentTypes;
    private List<EmbryoAssignment> assignedEmbryos;
    private Set<String> historyEmbryos;
    /** 已分配的硫化机台号集合（按 lhMachineCode 去重负荷） */
    private Set<String> assignedLhMachineCodes;
}
