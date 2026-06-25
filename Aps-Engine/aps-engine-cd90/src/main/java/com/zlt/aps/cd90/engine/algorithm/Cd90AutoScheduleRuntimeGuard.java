package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/** 检查直裁自动排程运行时间是否超过参数阈值。 */
@Slf4j
@Component
public class Cd90AutoScheduleRuntimeGuard {

    /**
     * 校验当前执行未超时。
     *
     * @param context 自动排程上下文
     * @param stage 当前检查阶段
     */
    public void checkNotTimedOut(Cd90AutoScheduleContext context, String stage) {
        if (context == null || context.getStartTime() == null || context.getParameters() == null) {
            throw new IllegalArgumentException("自动排程超时检查缺少计算上下文");
        }
        int timeoutMinutes = context.getParameters().getTaskTimeoutMinutes();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = context.getStartTime().plusMinutes(timeoutMinutes);
        if (!now.isBefore(deadline)) {
            long runningSeconds = Math.max(0L,
                    Duration.between(context.getStartTime(), now).getSeconds());
            log.error("[直裁自动排程] 任务主动超时中止, factoryCode={}, scheduleDate={}, "
                            + "stage={}, runningSeconds={}, timeoutMinutes={}",
                    context.getFactoryCode(), context.getScheduleDate(), stage,
                    runningSeconds, timeoutMinutes);
            throw new IllegalStateException("自动排程执行超时，阶段=" + stage
                    + "，运行秒数=" + runningSeconds + "，超时分钟数=" + timeoutMinutes);
        }
    }
}
