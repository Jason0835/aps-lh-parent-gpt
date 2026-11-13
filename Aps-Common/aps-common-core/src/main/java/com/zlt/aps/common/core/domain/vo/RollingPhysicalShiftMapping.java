package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 定时触发当班及其主侧、同步侧结果定位，并保存下一班需求窗口起点。
 */
@Data
public class RollingPhysicalShiftMapping {

    /**
     * 待调整物理班次开始时间。
     */
    private Date targetStartTime;

    /**
     * 待调整物理班次结束时间。
     */
    private Date targetEndTime;

    /**
     * 紧接目标当班的下一物理班开始时间，也是需求窗口起点。
     */
    private Date demandWindowStartTime;

    /**
     * 主侧排程日期和前三班班序。
     */
    private RollingPhysicalShiftRepresentation primary;

    /**
     * 同步侧前一排程日期和后三班班序。
     */
    private RollingPhysicalShiftRepresentation secondary;

    /**
     * 映射缺失或两侧物理时间不一致时为 false。
     */
    private boolean valid;

    /**
     * 新定时映射无效时的明确原因码，便于日志和任务审计定位。
     */
    private String invalidReason;
}
