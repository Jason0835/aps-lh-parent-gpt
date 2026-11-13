package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;

import java.time.LocalDate;

/**
 * 任务链公共引擎上下文访问端口。
 *
 * @param <T> 任务类型
 */
public interface TaskChainContextAccess<T extends ScheduleTaskDraftModel> {

    /**
     * 是否延迟任务链起止时间重算。
     *
     * <p>延迟模式仅自动排程入口显式启用：链变更时由引擎改调换型资源字段维护，
     * 起止时间推迟到分配结束后的统一终算写入。手工上下文及其他实现按模式缺省值
     * 返回 {@code false}，保持既有即时重算行为，不作为异常兜底。</p>
     *
     * @return 延迟时间重算时返回 true
     */
    default boolean isChainTimeDeferred() {
        return false;
    }

    MachineShiftTaskChain<T> getTaskChainGroup();

    LocalDate getScheduleDate();

    String getOperator();

    String getTraceId();

    ScheduleTaskNode<T> getTaskNode(String taskId);

    void registerTaskNode(String taskId, ScheduleTaskNode<T> node);

    void removeTaskNode(String taskId);
}
