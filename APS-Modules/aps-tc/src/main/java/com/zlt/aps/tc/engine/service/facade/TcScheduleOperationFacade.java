package com.zlt.aps.tc.engine.service.facade;

import com.zlt.aps.common.engine.schedule.ScheduleChainChangeResult;
import com.zlt.aps.tc.api.enums.TcScheduleEventTypeEnum;
import com.zlt.aps.tc.engine.domain.TcInsertPosition;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import com.zlt.aps.tc.engine.domain.TcTransferPosition;
import com.zlt.aps.tc.engine.event.TcScheduleEvent;
import com.zlt.aps.tc.engine.event.TcScheduleEventPublisher;
import com.zlt.aps.tc.engine.service.impl.TcScheduleProcessLogger;
import com.zlt.aps.tc.engine.service.impl.TcTaskChainScheduleService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 胎侧排程操作门面。
 *
 * <p>统一承接插单、删除、转机台、调量等人工操作，内部编排任务链修改、过程日志和事件发布。
 * 当前不处理发布状态回退和数据库事务，事务边界仍由 aps-tm 业务入口控制。</p>
 */
@Service
public class TcScheduleOperationFacade {

    private final TcTaskChainScheduleService taskChainScheduleService;

    private final TcScheduleProcessLogger processLogger;

    private final TcScheduleEventPublisher eventPublisher;

    /**
     * 创建胎侧排程操作门面。
     *
     * @param taskChainScheduleService 任务链排程服务
     * @param processLogger            过程日志服务，可为空
     * @param eventPublisher           事件发布器，可为空
     */
    public TcScheduleOperationFacade(TcTaskChainScheduleService taskChainScheduleService,
                                     @Nullable TcScheduleProcessLogger processLogger,
                                     @Nullable TcScheduleEventPublisher eventPublisher) {
        this.taskChainScheduleService = taskChainScheduleService;
        this.processLogger = processLogger;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 人工插单。
     *
     * @param task     插单任务
     * @param position 插入位置
     * @param context  胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> insertTask(TcTaskDraft task, TcInsertPosition position,
                                                             TcScheduleContext context) {
        ScheduleChainChangeResult<TcTaskDraft> result = taskChainScheduleService.insertManualTask(task, position, context);
        afterChainChanged(context, result, TcScheduleEventTypeEnum.MANUAL_INSERT, task == null ? null : task.getBusinessKey());
        return result;
    }

    /**
     * 删除任务。
     *
     * @param taskId  任务标识
     * @param context 胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> removeTask(String taskId, TcScheduleContext context) {
        ScheduleChainChangeResult<TcTaskDraft> result = taskChainScheduleService.removeTask(taskId, context);
        afterChainChanged(context, result, TcScheduleEventTypeEnum.REMOVE_TASK, taskId);
        return result;
    }

    /**
     * 转机台。
     *
     * @param taskId            任务标识
     * @param targetMachineCode 目标机台编码
     * @param position          目标位置
     * @param context           胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> transferMachine(String taskId, String targetMachineCode,
                                                                   TcTransferPosition position, TcScheduleContext context) {
        ScheduleChainChangeResult<TcTaskDraft> result = taskChainScheduleService
                .transferMachine(taskId, targetMachineCode, position, context);
        afterChainChanged(context, result, TcScheduleEventTypeEnum.TRANSFER_MACHINE, taskId);
        return result;
    }

    /**
     * 调整计划量。
     *
     * @param taskId     任务标识
     * @param newPlanQty 新计划量
     * @param shiftOrder 班次顺序
     * @param context    胎侧排程上下文
     * @return 链表变更结果
     */
    public ScheduleChainChangeResult<TcTaskDraft> changeQty(String taskId, BigDecimal newPlanQty, Integer shiftOrder,
                                                             TcScheduleContext context) {
        ScheduleChainChangeResult<TcTaskDraft> result = taskChainScheduleService
                .changeQty(taskId, newPlanQty, shiftOrder, context);
        afterChainChanged(context, result, TcScheduleEventTypeEnum.CHANGE_QTY, taskId);
        return result;
    }

    private void afterChainChanged(TcScheduleContext context, ScheduleChainChangeResult<TcTaskDraft> result,
                                   TcScheduleEventTypeEnum eventType, String summary) {
        if (processLogger != null) {
            processLogger.logChainChange(context, result);
        }
        if (eventPublisher != null) {
            eventPublisher.publish(TcScheduleEvent.of(context, eventType, summary));
        }
    }
}
