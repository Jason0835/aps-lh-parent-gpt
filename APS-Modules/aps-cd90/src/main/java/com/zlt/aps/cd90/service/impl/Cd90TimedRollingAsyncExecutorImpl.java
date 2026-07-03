package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.service.Cd90TimedRollingAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90TimedRollingExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** CD90定时滚动排程异步派发器。 */
@Service
@RequiredArgsConstructor
public class Cd90TimedRollingAsyncExecutorImpl implements Cd90TimedRollingAsyncExecutor {

    private final ObjectProvider<Cd90TimedRollingExecutionService> executionServiceProvider;
    private final Cd90ScheduleTaskService taskService;

    @Async
    @Override
    public void execute(String taskId, Cd90RollingTarget target, String inputVersion) {
        Cd90TimedRollingExecutionService executionService =
                executionServiceProvider.getIfAvailable();
        if (executionService == null) {
            taskService.markFailed(taskId, "定时滚动排程执行服务未就绪");
            return;
        }
        executionService.execute(taskId, target, inputVersion);
    }
}
