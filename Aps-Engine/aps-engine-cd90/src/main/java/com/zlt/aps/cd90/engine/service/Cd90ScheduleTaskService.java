package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;

import java.util.Date;
import java.util.List;

/**
 * 直裁自动排程任务状态服务。
 */
public interface Cd90ScheduleTaskService {

    Cd90ScheduleTask createPending(String factoryCode, Date scheduleDate, String triggerType,
                                   String requestSnapshot, String createBy);

    Cd90ScheduleTask findByTaskId(String taskId);

    Cd90ScheduleTask findLatest(String factoryCode, Date scheduleDate);

    Cd90ScheduleTask findActive(String factoryCode, Date scheduleDate);

    boolean start(String taskId);

    boolean updateProgress(String taskId, int progress, String stage, String stageName);

    boolean markSuccess(String taskId, String batchNo);

    /** 在调用方当前事务内更新成功状态，用于最终结果原子提交。 */
    boolean markSuccessInCurrentTransaction(String taskId, String batchNo);

    boolean markFailed(String taskId, String errorMessage);

    /** 查询待补偿检查的运行中任务，逻辑删除由框架处理。 */
    List<Cd90ScheduleTask> findRunningTasks(int limit);

    /** 仅将仍为RUNNING的指定任务标记为超时失败。 */
    boolean markTimeoutFailed(String taskId, String errorMessage);
}
