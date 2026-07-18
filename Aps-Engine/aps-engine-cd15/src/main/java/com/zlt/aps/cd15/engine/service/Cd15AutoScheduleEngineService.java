package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleOutputDraft;

import java.util.Date;

/**
 * 斜裁自动排程引擎入口。
 */
public interface Cd15AutoScheduleEngineService {

    /**
     * 初始化自动排程计算上下文。
     *
     * <p>该入口完成请求校验、启用班次读取和PARAM_CODE参数快照构建，后续第0至16步算法
     * 均在同一上下文内运行。</p>
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 自动排程计算上下文
     */
    Cd15AutoScheduleContext prepare(String factoryCode, Date scheduleDate);

    /**
     * 执行多班排程并构建最终事务可消费的输出草稿。
     *
     * @param context 已冻结参数和输出班次的计算上下文
     * @return 自动排程输出草稿
     */
    Cd15AutoScheduleOutputDraft execute(Cd15AutoScheduleContext context);

    /** 使用进度监听器执行完整多班排程。 */
    Cd15AutoScheduleOutputDraft execute(Cd15AutoScheduleContext context,
                                        Cd15ScheduleProgressListener listener);
}
