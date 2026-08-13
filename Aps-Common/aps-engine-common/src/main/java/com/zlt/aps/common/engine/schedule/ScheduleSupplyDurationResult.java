package com.zlt.aps.common.engine.schedule;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存供应时长逐班计算结果。
 *
 * <p>用于在胎面、胎侧之间共享计算结果，不包含具体业务对象。</p>
 */
@Data
public class ScheduleSupplyDurationResult {

    /** 库存供应时长，单位小时；无法计算时为空。 */
    private BigDecimal supplyHours;

    /** 逐班计算的中文代入过程。 */
    private String calculationDetail;

    /** 是否完整覆盖全部需求窗口。 */
    private boolean fullWindowCovered;

    /** 无法计算的原因编码；计算成功时为空。 */
    private String invalidReason;
}
