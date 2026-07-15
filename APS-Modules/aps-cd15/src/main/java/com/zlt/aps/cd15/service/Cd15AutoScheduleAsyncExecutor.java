package com.zlt.aps.cd15.service;

import java.util.Date;

/** CD15自动排程异步执行入口。 */
public interface Cd15AutoScheduleAsyncExecutor {

    void execute(String taskId, String factoryCode, Date scheduleDate);
}