package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.model.Cd90TaskRecoveryResult;

/** 供外部Job调用的自动排程遗留任务补偿服务。 */
public interface Cd90ScheduleTaskRecoveryService {

    /**
     * 扫描并补偿心跳超时且执行锁已不存在的RUNNING任务。
     *
     * @param timeoutMinutes 覆盖超时分钟数；为空时按任务工厂PARAM_CODE读取
     * @return 补偿汇总
     */
    Cd90TaskRecoveryResult recover(Integer timeoutMinutes);
}
