package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 通过硬约束过滤后的候选机台。
 */
@Data
@Builder
public class Cd90MachineCandidate {

    /** 机台编码。 */
    private String machineCode;
    /** 满班生产定额。 */
    private BigDecimal quota;
    /** 是否为JOB_TYPE=0定点优先机台。 */
    private boolean preferredMachine;
    /** 参数机台优先顺序，未配置时排在末尾。 */
    private int priorityOrder;
}
