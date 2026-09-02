package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/** 自动排程质量计算需要的任务字段契约。 */
public interface ScheduleQualityTask {

    String getPlanGroupKey();

    BigDecimal getPlanQty();

    String getMachineCode();

    Integer getShiftOrder();

    BigDecimal getPreviousSpecSwitchHours();

    BigDecimal getPreviousGlueSwitchHours();

    BigDecimal getPreviousGlueSwitchCapacityDeduct();

    BigDecimal getMachineSpeed();

    String getTailFlag();

    Boolean getFormingShutdownCloseOutFlag();

    BigDecimal getFormingShutdownCloseOutDemandQty();

    BigDecimal getStockDeductQty();

    BigDecimal getTailBalanceQty();

    /** @return 产品标准长度 */
    BigDecimal getQualityProductLength();

    /** @return 是否发生口型板切换 */
    Boolean getQualityMouthPlateSwitched();
}

