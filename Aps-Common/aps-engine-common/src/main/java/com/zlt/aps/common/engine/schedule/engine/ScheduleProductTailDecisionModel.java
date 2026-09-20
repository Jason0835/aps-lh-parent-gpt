package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/**
 * 产品级最终收尾判定结果。
 *
 * <p>该结果由完整来源和未来前移分配完成后统一生成，TM、TC只消费同一套判定，
 * 各自继续保留库存、工装和数量公式差异。</p>
 */
@Data
public class ScheduleProductTailDecisionModel {

    /**
     * 所有有效来源是否均可确定收尾。
     */
    private Boolean decisionAvailable;
    /**
     * 产品最终成型收尾班次。
     */
    private ScheduleFormingShiftKey finalFormingTailShiftKey;
    /**
     * 最终成型收尾班次对应需求是否参与未来前移。
     */
    private Boolean futureTailDemandPresent;
    /**
     * 最终需求实际分配到的最后承接生产班次。
     */
    private ScheduleFormingShiftKey lastTailReceivingShiftKey;
    /**
     * 判定原因。
     */
    private String decisionReason;
}
