package com.zlt.aps.common.engine.schedule;

import java.util.Date;

/**
 * 任务链节点时间纯计算结果。
 *
 * <p>结果只包含时间计算值，不包含任务、上下文或日志状态；TM/TC 负责将结果写回各自任务链节点。</p>
 */
public class ScheduleTaskTimingResult {

    /** 切换后的预计开始时间。 */
    private final Date startTime;

    /** 生产结束时间。 */
    private final Date endTime;

    /** 切换耗时秒数。 */
    private final long switchSeconds;

    /** 生产耗时秒数。 */
    private final long productionSeconds;

    /**
     * 创建任务链节点时间计算结果。
     *
     * @param startTime 预计开始时间
     * @param endTime 预计结束时间
     * @param switchSeconds 切换耗时秒数
     * @param productionSeconds 生产耗时秒数
     */
    public ScheduleTaskTimingResult(Date startTime, Date endTime, long switchSeconds, long productionSeconds) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.switchSeconds = switchSeconds;
        this.productionSeconds = productionSeconds;
    }

    /**
     * 获取预计开始时间。
     *
     * @return 预计开始时间；输入无效时返回 null
     */
    public Date getStartTime() {
        return this.startTime == null ? null : new Date(this.startTime.getTime());
    }

    /**
     * 获取预计结束时间。
     *
     * @return 预计结束时间；输入无效时返回 null
     */
    public Date getEndTime() {
        return this.endTime == null ? null : new Date(this.endTime.getTime());
    }

    /**
     * 获取切换耗时秒数。
     *
     * @return 切换耗时秒数
     */
    public long getSwitchSeconds() {
        return this.switchSeconds;
    }

    /**
     * 获取生产耗时秒数。
     *
     * @return 生产耗时秒数
     */
    public long getProductionSeconds() {
        return this.productionSeconds;
    }
}
