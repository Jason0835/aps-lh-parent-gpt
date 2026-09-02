package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;

import java.util.Date;

/**
 * 任务链公共引擎上下文访问端口。
 *
 * @param <T> 任务类型
 */
public interface TaskChainContextAccess<T extends ScheduleTaskDraftModel> {

    MachineShiftTaskChain<T> getTaskChainGroup();

    Date getScheduleDate();

    String getOperator();

    String getTraceId();

    ScheduleTaskNode<T> getTaskNode(String taskId);

    void registerTaskNode(String taskId, ScheduleTaskNode<T> node);

    void removeTaskNode(String taskId);
}
