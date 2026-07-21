package com.zlt.aps.cd15.service.impl;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.service.Cd15TimedRollingAsyncExecutor;
import com.zlt.aps.cd15.service.Cd15TimedRollingExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** CD15定时滚动排程异步派发器。 */
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingAsyncExecutorImpl implements Cd15TimedRollingAsyncExecutor {

    private final ObjectProvider<Cd15TimedRollingExecutionService> executionServiceProvider;
    private final Cd15ScheduleTaskService taskService;

    @Async
    @Override
    public void execute(String taskId, Cd15RollingTarget target, String inputVersion) {
        Cd15TimedRollingExecutionService executionService =
                executionServiceProvider.getIfAvailable();
        if (executionService == null) {
            taskService.markFailed(taskId, "定时滚动排程执行服务未就绪");
            return;
        }
        executionService.execute(taskId, target, inputVersion);
    }
}
