package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * CD15 多班滚动试排结果，正式落库前在 Engine 内部流转。
 */
@Data
@Builder
public class Cd15MultiShiftScheduleResult {

    /** 已试排成功的结果草稿。 */
    private List<Cd15ScheduleResultDraft> scheduledDrafts;
    /** 未排结果，后续 Task 5 转换为 t_cd15_unschedule_result。 */
    private List<Cd15SingleShiftScheduleResult> unscheduledResults;
}