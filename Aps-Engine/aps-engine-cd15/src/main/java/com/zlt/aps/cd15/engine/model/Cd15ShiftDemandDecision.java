package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 当前班次单个候选规格的需求决策输入。 */
@Data
@Builder
public class Cd15ShiftDemandDecision {

    /** 当前班次需要继续安排的净需求量。 */
    private BigDecimal netDemandQuantity;
    /** 月计划剩余量；为空时按非收尾继续并记录告警。 */
    private BigDecimal planSurplusQuantity;
    /** 当前选中需求窗口是否包含停产班次。 */
    private boolean stopAffected;
}
