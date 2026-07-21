package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 插单滚动重排的内存输出。
 */
@Data
@Builder
public class Cd15InsertRollingOutput {

    /** 本次计算使用的不可变上下文，最终事务据此复核参数和输入版本。 */
    private Cd15AutoScheduleContext context;

    /** 原排程批次号。 */
    private String batchNo;
    /** 待新增的插单或转机台主记录；分裁组合固定包含两条。 */
    private List<Cd15ScheduleResult> insertedResults;
    /** 原批次内需要更新的排程结果。 */
    private List<Cd15ScheduleResult> updatedResults;
    /** 原批次内需要删除的排程结果。*/
    private List<Cd15ScheduleResult> deletedResults;
    /** 受影响主结果的完整库排明细替换草稿。 */
    private List<Cd15InsertLaneAllocationDraft> laneAllocations;
    /** 窗口结束后仍未容纳的任务。 */
    private List<Cd15UnscheduleResult> unscheduledResults;
    /** 预演过程中逐班产生的跨班顺延影响。 */
    private List<Cd15InsertCarryoverImpact> carryoverImpacts;
}
