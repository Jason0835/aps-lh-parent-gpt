package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;

import java.util.Date;

/** 定时滚动排程任务幂等服务。 */
public interface Cd15RollingScheduleTaskService {

    /** 查询同工厂、同输入版本已成功的滚动任务。 */
    Cd15ScheduleTask findSuccessfulByIdempotencyKey(String factoryCode, String idempotencyKey);

    /** 创建带输入版本幂等键的等待任务。 */
    Cd15ScheduleTask createPending(String factoryCode, Date scheduleDate,
                                   String requestSnapshot, String idempotencyKey);
}
