package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import java.util.Date;

/** 一台物理机台的余量收尾计算结果，零量释放不伪造生产班次。 */
@Data
public class ContinuationMachineFinishPlan {
    /** 物理机台合计计划量。 */
    private int planQty;
    /** 最后正产量班次，零量时为空。 */
    private Integer finishShiftIndex;
    /** 实际生产结束时刻，零量时为空。 */
    private Date finishTime;
    /** 物理机台可交接时刻。 */
    private Date offlineTime;
    /** 本场景要求的收尾截止，承接余量的自由机台仅受窗口限制。 */
    private Date finishDeadline;
}
