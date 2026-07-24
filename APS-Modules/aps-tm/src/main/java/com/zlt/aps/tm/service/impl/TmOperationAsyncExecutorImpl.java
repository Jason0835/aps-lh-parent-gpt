package com.zlt.aps.tm.service.impl;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.enums.TmBackgroundTaskTypeEnum;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.domain.vo.TmOperationRequestSnapshot;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.aps.tm.service.TmOperationAsyncExecutor;
import com.zlt.aps.tm.service.TmOperationAuditContext;
import com.zlt.aps.tm.service.TmOperationTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 胎面人工操作异步执行器实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmOperationAsyncExecutorImpl implements TmOperationAsyncExecutor {

    private final TmOperationTaskService operationTaskService;

    private final TmManualScheduleApplicationService manualScheduleApplicationService;

    private final TmManualOperationFacade manualOperationFacade;

    private final ITmScheduleResultService scheduleResultService;

    /**
     * 启动后台任务并按统一阶段推进。
     *
     * @param taskId 任务编号
     */
    @Async
    @Override
    public void execute(String taskId) {
        TmAutoScheduleTask task = this.operationTaskService.findByTaskId(taskId);
        if (task == null || !this.operationTaskService.start(taskId)) {
            return;
        }
        TmOperationAuditContext.setOperator(task.getCreateBy());
        try {
            TmOperationRequestSnapshot snapshot = JSON.parseObject(task.getRequestSnapshot(),
                    TmOperationRequestSnapshot.class);
            this.operationTaskService.updateProgress(taskId, 60, "ROLLING",
                    I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskRolling"));
            int affectedCount = this.executeOperation(task.getTaskType(), snapshot);
            this.operationTaskService.updateProgress(taskId, 90, "SAVING",
                    I18nUtil.getMessage("ui.data.alert.tm.schedule.operationTaskSaving"));
            this.operationTaskService.markSuccess(taskId, affectedCount);
        } catch (Exception exception) {
            log.error("胎面人工异步任务执行失败，taskId={}，taskType={}", taskId, task.getTaskType(), exception);
            this.operationTaskService.markFailed(taskId, exception.getMessage());
        } finally {
            TmOperationAuditContext.clear();
        }
    }

    /**
     * 根据任务类型委托现有人工业务服务。
     *
     * @param taskType 任务类型
     * @param snapshot 请求快照
     * @return 影响行数
     */
    private int executeOperation(String taskType, TmOperationRequestSnapshot snapshot) {
        if (TmBackgroundTaskTypeEnum.MANUAL_INSERT.getCode().equals(taskType)) {
            return this.manualScheduleApplicationService.insertTask(snapshot.getInsertRequest());
        }
        if (TmBackgroundTaskTypeEnum.MANUAL_CHANGE_QTY.getCode().equals(taskType)) {
            return this.manualOperationFacade.changeQty(snapshot.getScheduleResult());
        }
        if (TmBackgroundTaskTypeEnum.MANUAL_CHANGE_MACHINE.getCode().equals(taskType)) {
            return this.manualOperationFacade.changeMachine(snapshot.getScheduleResult());
        }
        if (TmBackgroundTaskTypeEnum.MANUAL_BATCH_CHANGE_MACHINE.getCode().equals(taskType)) {
            return this.manualOperationFacade.batchChangeMachine(snapshot.getTargetMachineCode(),
                    snapshot.getScheduleResultList());
        }
        if (TmBackgroundTaskTypeEnum.MANUAL_DELETE.getCode().equals(taskType)) {
            return this.manualOperationFacade.deleteTasks(snapshot.getResultIdList());
        }
        if (TmBackgroundTaskTypeEnum.MANUAL_PUBLISH.getCode().equals(taskType)) {
            return this.scheduleResultService.publish(snapshot.getResultIdList());
        }
        throw new IllegalArgumentException(I18nUtil.getMessage(
                "ui.data.alert.tm.schedule.operationTaskTypeUnsupported"));
    }
}
