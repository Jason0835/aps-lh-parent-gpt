package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;

import java.util.Date;

/** 定时滚动排程任务幂等服务。 */
public interface Cd90RollingScheduleTaskService {

    /** 查询同工厂、同输入版本已成功的滚动任务。 */
    Cd90ScheduleTask findSuccessfulByIdempotencyKey(String factoryCode, String idempotencyKey);

    /** 创建带输入版本幂等键的等待任务。 */
    Cd90ScheduleTask createPending(String factoryCode, Date scheduleDate,
                                   String requestSnapshot, String idempotencyKey);
}
