package com.zlt.aps.tc.service.impl;

import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.service.ITcScheduleResultService;
import com.zlt.aps.tc.service.TcAutoScheduleAsyncExecutor;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 胎侧自动排程异步执行实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcAutoScheduleAsyncExecutorImpl implements TcAutoScheduleAsyncExecutor {

    @Lazy
    private final ITcScheduleResultService tcScheduleResultService;

    private final TcAutoScheduleTaskService taskService;

    /**
     * 执行胎侧自动排程任务。
     *
     * @param taskId 对外任务 ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        if (!taskService.start(taskId)) {
            log.warn("[TC_AUTO_PLAN] 自动排程任务启动失败或状态已变化, taskId={}", taskId);
            return;
        }
        try {
            tcScheduleResultService.executeTcAutoPlanTask(taskId);
        } catch (Exception exception) {
            log.error("[TC_AUTO_PLAN] 自动排程异步任务执行失败, taskId={}", taskId, exception);
            TcAutoScheduleTask task = taskService.findByTaskId(taskId);
            List<TcAutoScheduleIssueVo> issues = task == null || taskService.toResponse(task) == null
                    ? Collections.emptyList() : taskService.toResponse(task).getIssues();
            taskService.markFailed(taskId, exception.getMessage(), issues);
        }
    }
}