package com.zlt.aps.cx.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 机台状态（均衡分配过程中的机台状态记录）。
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
    /** 单台硫化机对应成型耗时（秒/班）= 28800 / 机台配比；null 表示未初始化（不校验耗时） */
    private BigDecimal secondsPerLhMachine;
    /** 机台累计耗时（秒/班），含续作预扣；不得超过单班物理产能 28800s */
    private BigDecimal usedSeconds = BigDecimal.ZERO;
}
