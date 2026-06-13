package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收尾规格判定结果。
 */
@Data
@Builder
public class Cd90CloseOutDecision {

    /** 是否为收尾规格。 */
    private boolean closeOut;
    /** 是否需要记录月计划剩余量缺失告警。 */
    private boolean missingPlanSurplusWarning;
}
