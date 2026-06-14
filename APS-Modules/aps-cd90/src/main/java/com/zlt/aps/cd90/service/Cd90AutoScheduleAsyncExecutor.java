package com.zlt.aps.cd90.service;

import java.util.Date;

/** 直裁自动排程异步执行边界。 */
public interface Cd90AutoScheduleAsyncExecutor {

    /** 异步执行指定任务。 */
    void execute(String taskId, String factoryCode, Date scheduleDate);
}
