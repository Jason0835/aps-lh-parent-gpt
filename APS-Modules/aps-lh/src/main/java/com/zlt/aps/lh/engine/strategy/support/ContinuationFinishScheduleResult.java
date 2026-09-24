package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import java.util.List;

/** 整组只读计算的输出，由续作统一提交阶段消费。 */
@Data
public class ContinuationFinishScheduleResult {
    /** 与前置机台顺序严格对应的计算结果。 */
    private List<ContinuationMachineFinishPlan> machinePlans;
    /** 尚未消化的真实余量，禁止为凑节点超排。 */
    private int remainingQty;
    /** 数量归整组合搜索未完成时的明确诊断。 */
    private String diagnostic;
}
