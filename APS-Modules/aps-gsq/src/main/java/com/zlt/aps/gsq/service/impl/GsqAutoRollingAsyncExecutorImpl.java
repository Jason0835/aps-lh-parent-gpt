package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingTaskVo;
import com.zlt.aps.gsq.constant.GsqScheduleConstants;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import com.zlt.aps.gsq.engine.vo.GsqRollingUpdateResult;
import com.zlt.aps.gsq.service.GsqAutoRollingAsyncExecutor;
import com.zlt.aps.gsq.service.GsqBackgroundTaskService;
import com.zlt.aps.gsq.service.IGsqRollingUpdateService;
import com.zlt.aps.gsq.engine.event.GsqScheduleEventPublisher;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈自动滚动异步执行器实现。
 *
 * <p>异步任务由 {@link com.zlt.aps.gsq.service.GsqAutoRollingApplicationService} 派发，
 * 本执行器负责：</p>
 * <ol>
 *   <li>将任务状态从 PENDING -> RUNNING；</li>
 *   <li>调用 {@link IGsqRollingUpdateService#autoRollingUpdate} 完成滚动更新；</li>
 *   <li>更新任务进度并最终标记为 SUCCESS/FAILED。</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsqAutoRollingAsyncExecutorImpl implements GsqAutoRollingAsyncExecutor {

    private final GsqBackgroundTaskService backgroundTaskService;
    private final IGsqRollingUpdateService gsqRollingUpdateService;
    private final GsqScheduleEventPublisher scheduleEventPublisher;

    /**
     * 异步执行自动滚动任务。
     *
     * @param taskId 任务ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!this.backgroundTaskService.start(taskId, GsqScheduleConstants.ROLLING_STAGE_CALCULATING,
                I18nUtil.getMessage("ui.gsq.schedule.rolling.calculating"))) {
            return;
        }
        GsqAutoScheduleTask task = this.backgroundTaskService.findByTaskId(taskId);
        try {
            GsqRollingTaskVo result = this.calculateAndAdjust(task);
            this.backgroundTaskService.markSuccess(taskId, result, result.getSummary(), result.getIssues());
        } catch (Exception exception) {
            log.error("钢丝圈自动滚动执行失败，taskId={}", taskId, exception);
            List<String> issues = new ArrayList<>();
            issues.add(exception.getMessage());
            this.backgroundTaskService.markFailed(taskId, exception.getMessage(),
                    new LinkedHashMap<>(), issues);
        }
    }

    /**
     * 调用业务层自动滚动，封装响应。
     *
     * @param task 自动滚动任务
     * @return 滚动任务响应
     */
    private GsqRollingTaskVo calculateAndAdjust(GsqAutoScheduleTask task) {
        if (task == null || task.getTargetShiftOrder() == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.gsq.schedule.rolling.taskInvalid"));
        }
        this.backgroundTaskService.updateProgress(task.getTaskId(), 30,
                GsqScheduleConstants.ROLLING_STAGE_CALCULATING,
                I18nUtil.getMessage("ui.gsq.schedule.rolling.calculating"), null);

        GsqRollingUpdateResult updateResult = this.gsqRollingUpdateService.autoRollingUpdate(
                task.getScheduleDate(), task.getTargetShiftOrder(), task.getFactoryCode());

        // 发布自动滚动完成事件（供下游监听器消费，解耦通知/审计/下游排程）
        this.scheduleEventPublisher.publishAutoRollingEvent(
                task.getFactoryCode(), task.getScheduleDate(), task.getTargetShiftOrder(),
                updateResult.getBatchNo(), updateResult.isSuccess() ? "SUCCESS" : "FAILED",
                updateResult.getAffectedCount(), updateResult.getBeforeStockQty(),
                updateResult.getAfterStockQty());

        this.backgroundTaskService.updateProgress(task.getTaskId(), 90,
                GsqScheduleConstants.ROLLING_STAGE_PERSISTING,
                I18nUtil.getMessage("ui.gsq.schedule.rolling.persisting"), null);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("targetShiftOrder", task.getTargetShiftOrder());
        summary.put("affectedCount", updateResult.getAffectedCount());
        summary.put("beforeStockQty", updateResult.getBeforeStockQty());
        summary.put("afterStockQty", updateResult.getAfterStockQty());
        summary.put("success", updateResult.isSuccess());
        if (StrUtil.isNotBlank(updateResult.getErrorMsg())) {
            summary.put("errorMsg", updateResult.getErrorMsg());
        }

        GsqRollingTaskVo response = new GsqRollingTaskVo();
        response.setTaskId(task.getTaskId());
        response.setTaskStatus("SUCCESS");
        response.setProgress(100);
        response.setCurrentStage(GsqScheduleConstants.ROLLING_STAGE_COMPLETE);
        response.setFactoryCode(task.getFactoryCode());
        response.setTargetShiftOrder(task.getTargetShiftOrder());
        response.setBatchNo(updateResult.getBatchNo());
        response.setInputVersion(task.getInputVersion());
        response.setAffectedCount(updateResult.getAffectedCount());
        response.setBeforeStockQty(updateResult.getBeforeStockQty());
        response.setAfterStockQty(updateResult.getAfterStockQty());
        response.setSummary(summary);
        if (StrUtil.isNotBlank(updateResult.getErrorMsg())) {
            response.getIssues().add(updateResult.getErrorMsg());
        }
        return response;
    }
}
