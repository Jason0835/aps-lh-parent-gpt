package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.zlt.aps.gsq.api.domain.vo.GsqOperationRequestSnapshot;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import com.zlt.aps.gsq.enums.GsqBackgroundTaskTypeEnum;
import com.zlt.aps.gsq.service.GsqBackgroundTaskService;
import com.zlt.aps.gsq.service.GsqOperationAsyncExecutor;
import com.zlt.aps.gsq.service.GsqOperationAuditContext;
import com.zlt.aps.gsq.service.GsqManualScheduleApplicationService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 钢丝圈人工操作异步执行器实现。
 *
 * <p>对齐胎侧 {@code TcOperationAsyncExecutorImpl}，负责将4类人工操作（插单/调量/转机台/删除）
 * 在 {@code @Async} 线程内执行，保证 Web 安全上下文不传播时仍能正确记录操作人。</p>
 *
 * <p>执行流程：</p>
 * <ol>
 *   <li>调用 {@link GsqBackgroundTaskService#startOperation} 将任务状态从 PENDING 置为 RUNNING；</li>
 *   <li>从 {@link GsqAutoScheduleTask#getRequestSnapshot()} 反序列化为
 *       {@link GsqOperationRequestSnapshot}；</li>
 *   <li>通过 {@link GsqOperationAuditContext#setOperator(String)} 传递操作人，
 *       供门面审计日志读取；</li>
 *   <li>按 taskType 委托 {@link GsqManualScheduleApplicationService} 执行；</li>
 *   <li>成功调用 {@link GsqBackgroundTaskService#markOperationSuccess}，异常调用
 *       {@link GsqBackgroundTaskService#markOperationFailed}；</li>
 *   <li>finally 块调用 {@link GsqOperationAuditContext#clear()} 清理线程上下文。</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsqOperationAsyncExecutorImpl implements GsqOperationAsyncExecutor {

    /** 后台任务状态服务 */
    private final GsqBackgroundTaskService backgroundTaskService;

    /** 人工排程应用服务（业务编排委托门面） */
    private final GsqManualScheduleApplicationService gsqManualScheduleApplicationService;

    /**
     * 异步执行人工操作任务。
     *
     * @param taskId 任务ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!this.backgroundTaskService.startOperation(taskId)) {
            return;
        }
        GsqAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        if (task == null) {
            log.warn("钢丝圈人工操作任务不存在，taskId={}", taskId);
            return;
        }
        GsqOperationRequestSnapshot snapshot = this.deserializeSnapshot(task);
        if (snapshot == null) {
            this.backgroundTaskService.markOperationFailed(taskId,
                    I18nUtil.getMessage("ui.gsq.schedule.operationTaskPending"));
            return;
        }
        try {
            // 传递操作人给异步线程，供门面审计日志读取
            GsqOperationAuditContext.setOperator(snapshot.getOperator());
            int affectedCount = this.dispatchByTaskType(task.getTaskType(), snapshot);
            this.backgroundTaskService.markOperationSuccess(taskId, affectedCount);
        } catch (Exception exception) {
            log.error("钢丝圈人工操作执行失败，taskId={}, taskType={}", taskId, task.getTaskType(), exception);
            this.backgroundTaskService.markOperationFailed(taskId, exception.getMessage());
        } finally {
            // 清理线程上下文，避免线程复用造成审计人串用
            GsqOperationAuditContext.clear();
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 反序列化请求快照。
     *
     * @param task 后台任务
     * @return 请求快照，快照为空时返回 null
     */
    private GsqOperationRequestSnapshot deserializeSnapshot(GsqAutoScheduleTask task) {
        String requestSnapshot = task.getRequestSnapshot();
        if (StrUtil.isBlank(requestSnapshot)) {
            return null;
        }
        return JSON.parseObject(requestSnapshot, GsqOperationRequestSnapshot.class);
    }

    /**
     * 按任务类型委托应用服务执行人工操作。
     *
     * @param taskType 任务类型
     * @param snapshot 请求快照
     * @return 受影响行数
     * @throws IllegalArgumentException 不支持的任务类型时抛出
     */
    private int dispatchByTaskType(String taskType, GsqOperationRequestSnapshot snapshot) {
        if (GsqBackgroundTaskTypeEnum.MANUAL_INSERT.getCode().equals(taskType)) {
            return gsqManualScheduleApplicationService.insertTask(snapshot.getInsertRequest());
        }
        if (GsqBackgroundTaskTypeEnum.MANUAL_CHANGE_QTY.getCode().equals(taskType)) {
            return gsqManualScheduleApplicationService.changeQtyBatch(snapshot.getChangeQtyRequestList());
        }
        if (GsqBackgroundTaskTypeEnum.MANUAL_CHANGE_MACHINE.getCode().equals(taskType)) {
            return gsqManualScheduleApplicationService.changeMachine(snapshot.getChangeMachineRequestList());
        }
        if (GsqBackgroundTaskTypeEnum.MANUAL_DELETE.getCode().equals(taskType)) {
            return gsqManualScheduleApplicationService.remove(snapshot.getResultIdList());
        }
        throw new IllegalArgumentException(I18nUtil.getMessage("ui.gsq.schedule.operationTaskTypeUnsupported"));
    }
}
