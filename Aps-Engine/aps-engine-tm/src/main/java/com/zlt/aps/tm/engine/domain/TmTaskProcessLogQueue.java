package com.zlt.aps.tm.engine.domain;

import com.zlt.aps.common.engine.schedule.ScheduleProcessTraceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 胎面任务关联过程日志延后队列。
 *
 * <p>仅负责记录原始发生顺序和保存摘要或 FULL 事件；任务链排序、日志分区和最终渲染
 * 仍由 {@link TmScheduleContext} 在原调用点处理，避免改变延后日志的展示语义。</p>
 */
final class TmTaskProcessLogQueue {

    /** 待按最终任务链输出的任务关联过程日志。 */
    private List<TmTaskProcessLogEntry> entryList = new ArrayList<>();

    /** 任务关联过程日志的原始发生序号。 */
    private Long occurrenceSequence = 0L;

    /**
     * 暂存一条任务关联摘要日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param logCategory 日志执行阶段类别
     * @param format 日志格式
     * @param args 日志参数
     */
    void appendSummary(String taskBusinessKey, Integer shiftOrder, String logCategory,
                       String format, Object... args) {
        TmTaskProcessLogEntry entry = this.newEntry(taskBusinessKey, shiftOrder, logCategory);
        entry.setFormat(format);
        entry.setArgs(args == null ? new Object[0] : args.clone());
        this.getEntries().add(entry);
    }

    /**
     * 暂存一条任务关联 FULL 事件。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param logCategory 日志执行阶段类别
     * @param event 完整过程事件
     */
    void appendFull(String taskBusinessKey, Integer shiftOrder, String logCategory,
                    ScheduleProcessTraceEvent event) {
        TmTaskProcessLogEntry entry = this.newEntry(taskBusinessKey, shiftOrder, logCategory);
        entry.setFullEvent(event);
        this.getEntries().add(entry);
    }

    /**
     * 获取非空的延后日志集合。
     *
     * @return 延后日志集合
     */
    List<TmTaskProcessLogEntry> getEntries() {
        if (this.entryList == null) {
            this.entryList = new ArrayList<>();
        }
        return this.entryList;
    }

    /**
     * 创建带稳定发生序号的日志条目。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param logCategory 日志执行阶段类别
     * @return 新日志条目
     */
    private TmTaskProcessLogEntry newEntry(String taskBusinessKey, Integer shiftOrder, String logCategory) {
        TmTaskProcessLogEntry entry = new TmTaskProcessLogEntry();
        entry.setTaskBusinessKey(taskBusinessKey);
        entry.setShiftOrder(shiftOrder);
        entry.setLogCategory(logCategory);
        entry.setOccurrenceOrder(this.nextOccurrenceSequence());
        return entry;
    }

    /**
     * 获取下一个从 1 开始递增的发生序号。
     *
     * @return 日志发生序号
     */
    private long nextOccurrenceSequence() {
        this.occurrenceSequence = Optional.ofNullable(this.occurrenceSequence).orElse(0L) + 1L;
        return this.occurrenceSequence;
    }
}
