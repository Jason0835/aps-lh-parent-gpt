package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.model.Cd15TaskRecoveryResult;

/** 斜裁遗留运行中任务补偿服务。 */
public interface Cd15ScheduleTaskRecoveryService {

    /**
     * 补偿心跳超时且执行锁已不存在的运行中任务。
     *
     * @param timeoutMinutes 可选覆盖超时分钟数；为空时读取工厂参数
     * @return 补偿汇总
     */
    Cd15TaskRecoveryResult recover(Integer timeoutMinutes);
}
