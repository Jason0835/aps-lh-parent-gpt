package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动排程前置滚动的内部执行结果，不作为独立接口契约。
 */
@Data
public class AutoSchedulePreRollingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本次工厂是否启用前置滚动。
     */
    private boolean enabled;

    /**
     * 目标早班归属排程日期。
     */
    private LocalDate targetScheduleDate;

    /**
     * 目标早班班序，固定为3。
     */
    private Integer targetShiftOrder = 3;

    /**
     * 现有自动滚动执行编号。
     */
    private String executionId;

    /**
     * 现有自动滚动技术执行状态。
     */
    private String executionState;

    /**
     * 现有自动滚动业务状态。
     */
    private String status;

    /**
     * 当前自动排程是否可以继续。
     */
    private boolean allowedToContinue = true;

    /**
     * 可供任务失败提示或成功警告使用的说明。
     */
    private String message;

    /**
     * 需要写入自动排程任务的问题。
     */
    private List<AutoScheduleIssueVo> issues = new ArrayList<>();
}
