package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import org.junit.Test;

import java.time.LocalDateTime;

/** 自动排程主动超时守卫测试。 */
public class Cd15AutoScheduleRuntimeGuardTest {

    private final Cd15AutoScheduleRuntimeGuard guard = new Cd15AutoScheduleRuntimeGuard();

    @Test
    public void shouldContinueBeforeTimeout() {
        guard.checkNotTimedOut(context(LocalDateTime.now().minusMinutes(29)), "班次开始");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectWhenTimeoutReached() {
        guard.checkNotTimedOut(context(LocalDateTime.now().minusMinutes(30)), "班次开始");
    }

    private Cd15AutoScheduleContext context(LocalDateTime startTime) {
        return Cd15AutoScheduleContext.builder().factoryCode("116")
                .startTime(startTime).parameters(Cd15AutoScheduleParameters.builder()
                        .taskTimeoutMinutes(30).build()).build();
    }
}
