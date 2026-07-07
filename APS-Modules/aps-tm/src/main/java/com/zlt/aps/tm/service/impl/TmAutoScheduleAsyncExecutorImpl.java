package com.zlt.aps.tm.service.impl;

import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleIssueVo;
import com.zlt.aps.tm.domain.TmAutoScheduleTask;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.aps.tm.service.TmAutoScheduleAsyncExecutor;
import com.zlt.aps.tm.service.TmAutoScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 胎面自动排程异步执行实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmAutoScheduleAsyncExecutorImpl implements TmAutoScheduleAsyncExecutor {

    @Lazy
    private final ITmScheduleResultService tmScheduleResultService;

    private final TmAutoScheduleTaskService taskService;

    /**
     * 执行胎面自动排程任务。
     *
     * @param taskId 对外任务 ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!taskService.start(taskId)) {
            log.warn("[TM_AUTO_PLAN] 自动排程任务启动失败或状态已变化, taskId={}", taskId);
            return;
        }
        try {
            tmScheduleResultService.executeTmAutoPlanTask(taskId);
        } catch (Exception exception) {
            log.error("[TM_AUTO_PLAN] 自动排程异步任务执行失败, taskId={}", taskId, exception);
            TmAutoScheduleTask task = taskService.findByTaskId(taskId);
            List<TmAutoScheduleIssueVo> issues = task == null || taskService.toResponse(task) == null
                    ? Collections.emptyList() : taskService.toResponse(task).getIssues();
            taskService.markFailed(taskId, exception.getMessage(), issues);
        }
    }
}