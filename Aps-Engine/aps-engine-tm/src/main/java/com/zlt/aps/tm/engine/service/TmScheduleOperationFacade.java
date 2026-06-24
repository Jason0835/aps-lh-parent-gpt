package com.zlt.aps.tm.engine.service;

import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.tm.api.enums.TmScheduleEventTypeEnum;
import com.zlt.aps.tm.engine.domain.TmInsertPosition;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.domain.TmTransferPosition;
import com.zlt.aps.tm.engine.event.TmScheduleEvent;
import com.zlt.aps.tm.engine.event.TmScheduleEventPublisher;
import com.zlt.aps.tm.engine.service.impl.TmTaskChainScheduleService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 胎面排程操作门面。
 *
 * <p>统一承接插单、删除、转机台、调量等人工操作，内部编排任务链修改、过程日志和事件发布。
 * 当前不处理发布状态回退和数据库事务，事务边界仍由 aps-tm 业务入口控制。</p>
 */
@Service
public class TmScheduleOperationFacade {

    private final TmTaskChainScheduleService taskChainScheduleService;

    private final TmScheduleProcessLogger processLogger;

    private final TmScheduleEventPublisher eventPublisher;

    /**
     * 创建胎面排程操作门面。
     *
     * @param taskChainScheduleService 任务链排程服务
     * @param processLogger            过程日志服务，可为空
     * @param eventPublisher           事件发布器，可为空
     */
    public TmScheduleOperationFacade(TmTaskChainScheduleService taskChainScheduleService,
                                     @Nullable TmScheduleProcessLogger processLogger,
                                     @Nullable TmScheduleEventPublisher eventPublisher) {
        this.taskChainScheduleService = taskChainScheduleService;
        this.processLogger = processLogger;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 人工插单。
     *
     * @param task     插单任务
     * @param position 插入位置
     * @param context  胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> insertTask(TmTaskDraft task, TmInsertPosition position,
                                                             TmScheduleContext context) {
        ScheduleChainChangeResult<TmTaskDraft> result = taskChainScheduleService.insertManualTask(task, position, context);
        afterChainChanged(context, result, TmScheduleEventTypeEnum.MANUAL_INSERT, task == null ? null : task.getBusinessKey());
        return result;
    }

    /**
     * 删除任务。
     *
     * @param taskId  任务标识
     * @param context 胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> removeTask(String taskId, TmScheduleContext context) {
        ScheduleChainChangeResult<TmTaskDraft> result = taskChainScheduleService.removeTask(taskId, context);
        afterChainChanged(context, result, TmScheduleEventTypeEnum.REMOVE_TASK, taskId);
        return result;
    }

    /**
     * 转机台。
     *
     * @param taskId            任务标识
     * @param targetMachineCode 目标机台编码
     * @param position          目标位置
     * @param context           胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> transferMachine(String taskId, String targetMachineCode,
                                                                   TmTransferPosition position, TmScheduleContext context) {
        ScheduleChainChangeResult<TmTaskDraft> result = taskChainScheduleService
                .transferMachine(taskId, targetMachineCode, position, context);
        afterChainChanged(context, result, TmScheduleEventTypeEnum.TRANSFER_MACHINE, taskId);
        return result;
    }

    /**
     * 调整计划量。
     *
     * @param taskId     任务标识
     * @param newPlanQty 新计划量
     * @param shiftOrder 班次顺序
     * @param context    胎面排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TmTaskDraft> changeQty(String taskId, BigDecimal newPlanQty, Integer shiftOrder,
                                                             TmScheduleContext context) {
        ScheduleChainChangeResult<TmTaskDraft> result = taskChainScheduleService
                .changeQty(taskId, newPlanQty, shiftOrder, context);
        afterChainChanged(context, result, TmScheduleEventTypeEnum.CHANGE_QTY, taskId);
        return result;
    }

    private void afterChainChanged(TmScheduleContext context, ScheduleChainChangeResult<TmTaskDraft> result,
                                   TmScheduleEventTypeEnum eventType, String summary) {
        if (processLogger != null) {
            processLogger.logChainChange(context, result);
        }
        if (eventPublisher != null) {
            eventPublisher.publish(TmScheduleEvent.of(context, eventType, summary));
        }
    }
}
