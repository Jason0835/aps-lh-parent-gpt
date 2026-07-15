package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.engine.constant.Cd15ScheduleTaskStage;
import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15RollingPrefixResourceUsage;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleTaskService;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import com.zlt.aps.cd15.engine.service.Cd15TimedRollingService;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.service.Cd15TimedRollingExecutionService;
import com.zlt.aps.cd15.service.Cd15TimedRollingPrefixResourceService;
import com.zlt.aps.cd15.service.Cd15TimedRollingPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.List;

/** CD15定时滚动排程锁、Engine和持久化编排。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingExecutionServiceImpl implements Cd15TimedRollingExecutionService {

    private static final String AGING_PERIOD_PARAM_CODE = "SYS0601032";
    private static final int DEFAULT_AGING_PERIOD_HOURS = 24;

    private final Cd15AutoScheduleLockService lockService;
    private final Cd15ScheduleTaskService taskService;
    private final Cd15TimedRollingService rollingService;
    private final Cd15TimedRollingPersistService persistService;
    private final Cd15TimedRollingPrefixResourceService prefixResourceService;
    private final Cd15ParamsMapper paramsMapper;

    @Override
    public void execute(String taskId, Cd15RollingTarget target, String inputVersion) {
        RLock lock = lockService.getLock(target.getFactoryCode(), target.getScheduleDate());
        try {
            if (!lock.tryLock()) {
                taskService.markFailed(taskId, "同排程日已有任务持有执行锁");
                return;
            }
            if (!taskService.start(taskId)) {
                log.warn("[斜裁定时滚动] 任务已被其它执行者处理 taskId={}", taskId);
                return;
            }
            this.updateProgress(taskId, 20, Cd15ScheduleTaskStage.LOAD_INPUT, "加载定时滚动输入");
            List<Cd15RollingPrefixResourceUsage> prefixResourceUsages = prefixResourceService.loadPrefixResourceUsages(target);
            Cd15ScheduleProgressListener listener = (progress, stage, stageName) ->
                    this.updateProgress(taskId, progress, stage, stageName);
            Cd15TimedRollingOutput output = rollingService.execute(target, inputVersion,
                    this.resolveAgingPeriodHours(target.getFactoryCode()), prefixResourceUsages, listener);
            this.updateProgress(taskId, 85, Cd15ScheduleTaskStage.SAVE_RESULT, "保存定时滚动结果");
            persistService.persist(taskId, target, output, lock);
        } catch (Exception exception) {
            log.error("[斜裁定时滚动] 异步任务执行失败, taskId={}, factoryCode={}, scheduleDate={}",
                    taskId, target == null ? null : target.getFactoryCode(),
                    target == null ? null : target.getScheduleDate(), exception);
            taskService.markFailed(taskId, this.safeMessage(exception));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void updateProgress(String taskId, int progress, String stage, String stageName) {
        Cd15ScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null) {
            throw new IllegalStateException("CD15定时滚动排程任务不存在");
        }
        if (!taskService.updateProgress(taskId, progress, stage, stageName)) {
            log.warn("[斜裁定时滚动] 任务进度更新失败但继续执行 taskId={}, progress={}, stage={}",
                    taskId, progress, stage);
        }
    }

    private int resolveAgingPeriodHours(String factoryCode) {
        Cd15Params param = paramsMapper.selectOne(Wrappers.<Cd15Params>lambdaQuery()
                .eq(Cd15Params::getFactoryCode, factoryCode)
                .eq(Cd15Params::getParamCode, AGING_PERIOD_PARAM_CODE)
                .last("limit 1"));
        if (param == null || param.getParamValue() == null || param.getParamValue().trim().isEmpty()) {
            return DEFAULT_AGING_PERIOD_HOURS;
        }
        try {
            return Math.max(0, Integer.parseInt(param.getParamValue().trim()));
        } catch (NumberFormatException exception) {
            return DEFAULT_AGING_PERIOD_HOURS;
        }
    }

    private String safeMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return "CD15定时滚动排程执行失败";
        }
        return exception.getMessage();
    }
}