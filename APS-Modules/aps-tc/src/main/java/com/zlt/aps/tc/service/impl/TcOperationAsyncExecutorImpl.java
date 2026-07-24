package com.zlt.aps.tc.service.impl;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.vo.TcOperationRequestSnapshot;
import com.zlt.aps.tc.service.TcBackgroundTaskService;
import com.zlt.aps.tc.service.TcOperationAsyncExecutor;
import com.zlt.aps.tc.service.TcOperationAuditContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 胎侧人工操作异步执行器实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcOperationAsyncExecutorImpl implements TcOperationAsyncExecutor {

    private final TcBackgroundTaskService backgroundTaskService;

    private final TcManualScheduleApplicationService manualScheduleApplicationService;

    /**
     * 启动后台任务并按统一阶段推进。
     *
     * @param taskId 任务编号
     */
    @Async
    @Override
    public void execute(String taskId) {
        TcAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        if (task == null || !this.backgroundTaskService.startOperation(taskId)) {
            return;
        }
        TcOperationAuditContext.setOperator(task.getCreateBy());
        try {
            TcOperationRequestSnapshot snapshot = JSON.parseObject(task.getRequestSnapshot(),
                    TcOperationRequestSnapshot.class);
            this.backgroundTaskService.updateProgress(taskId, 60, "ROLLING",
                    I18nUtil.getMessage("ui.tc.schedule.operationTaskRolling"), null);
            int affectedCount = this.executeOperation(task.getTaskType(), snapshot);
            this.backgroundTaskService.updateProgress(taskId, 90, "SAVING",
                    I18nUtil.getMessage("ui.tc.schedule.operationTaskSaving"), null);
            this.backgroundTaskService.markOperationSuccess(taskId, affectedCount);
        } catch (Exception exception) {
            log.error("胎侧人工异步任务执行失败，taskId={}，taskType={}", taskId, task.getTaskType(), exception);
            this.backgroundTaskService.markOperationFailed(taskId, exception.getMessage());
        } finally {
            TcOperationAuditContext.clear();
        }
    }

    /**
     * 根据任务类型委托现有人工业务服务。
     *
     * @param taskType 任务类型
     * @param snapshot 请求快照
     * @return 影响行数
     */
    private int executeOperation(String taskType, TcOperationRequestSnapshot snapshot) {
        if (TcBackgroundTaskTypeEnum.MANUAL_INSERT.getCode().equals(taskType)) {
            return this.manualScheduleApplicationService.insertTask(snapshot.getInsertRequest());
        }
        if (TcBackgroundTaskTypeEnum.MANUAL_CHANGE_QTY.getCode().equals(taskType)) {
            return this.manualScheduleApplicationService.changeQty(snapshot.getChangeQtyRequest());
        }
        if (TcBackgroundTaskTypeEnum.MANUAL_CHANGE_MACHINE.getCode().equals(taskType)) {
            return this.manualScheduleApplicationService.changeMachine(snapshot.getChangeMachineRequest());
        }
        if (TcBackgroundTaskTypeEnum.MANUAL_DELETE.getCode().equals(taskType)) {
            return this.manualScheduleApplicationService.remove(snapshot.getResultIdList());
        }
        throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.operationTaskTypeUnsupported"));
    }
}
