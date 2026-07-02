package com.zlt.aps.cd90.engine.model;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 插单滚动重排的内存输出。
 */
@Data
@Builder
public class Cd90InsertRollingOutput {

    /** 本次计算使用的不可变上下文，最终事务据此复核参数和输入版本。 */
    private Cd90AutoScheduleContext context;

    /** 原排程批次号。 */
    private String batchNo;
    /** 待新增的插单主记录。 */
    private Cd90ScheduleResult insertResult;
    /** 原批次内需要更新的排程结果。 */
    private List<Cd90ScheduleResult> updatedResults;
    /** 受影响主结果的完整库排明细替换草稿。 */
    private List<Cd90InsertLaneAllocationDraft> laneAllocations;
    /** 窗口结束后仍未容纳的任务。 */
    private List<Cd90UnscheduleResult> unscheduledResults;
}
