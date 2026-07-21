package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 收尾规格判定结果。
 */
@Data
@Builder
public class Cd15CloseOutDecision {

    /** 是否为收尾规格。 */
    private boolean closeOut;
    /** 是否需要记录月计划剩余量缺失告警。 */
    private boolean missingPlanSurplusWarning;
    /** 各胎胚收尾比较明细。 */
    private List<Cd15EmbryoCloseOutItem> embryoItems;
}
