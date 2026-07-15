package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;

import java.util.List;

/** CD15目标班次后缀滚动Engine入口。 */
public interface Cd15TimedRollingService {

    /**
     * 执行目标班次及后续班次试排。
     *
     * @param target 滚动目标
     * @param inputVersion 输入版本
     * @param agingPeriodHours 大卷成熟期小时数
     * @return 后缀滚动输出
     */
    Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion, int agingPeriodHours);

    /**
     * 执行目标班次及后续班次试排，并扣减目标班次之前保留结果已占用资源。
     *
     * @param target 滚动目标
     * @param inputVersion 输入版本
     * @param agingPeriodHours 大卷成熟期小时数
     * @param prefixResourceUsages 前缀已排资源占用
     * @return 后缀滚动输出
     */
    Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion, int agingPeriodHours,
                                   List<Cd15RollingPrefixResourceUsage> prefixResourceUsages);

    /**
     * 执行目标班次及后续班次试排，并回调班次进度。
     *
     * @param target 滚动目标
     * @param inputVersion 输入版本
     * @param agingPeriodHours 大卷成熟期小时数
     * @param prefixResourceUsages 前缀已排资源占用
     * @param listener 进度监听器
     * @return 后缀滚动输出
     */
    Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion, int agingPeriodHours,
                                   List<Cd15RollingPrefixResourceUsage> prefixResourceUsages,
                                   Cd15ScheduleProgressListener listener);
}