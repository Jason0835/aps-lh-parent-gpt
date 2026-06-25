package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import org.junit.Test;

import java.time.LocalDateTime;

/** 自动排程主动超时守卫测试。 */
public class Cd90AutoScheduleRuntimeGuardTest {

    private final Cd90AutoScheduleRuntimeGuard guard = new Cd90AutoScheduleRuntimeGuard();

    @Test
    public void shouldContinueBeforeTimeout() {
        guard.checkNotTimedOut(context(LocalDateTime.now().minusMinutes(29)), "班次开始");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectWhenTimeoutReached() {
        guard.checkNotTimedOut(context(LocalDateTime.now().minusMinutes(30)), "班次开始");
    }

    private Cd90AutoScheduleContext context(LocalDateTime startTime) {
        return Cd90AutoScheduleContext.builder().factoryCode("116")
                .startTime(startTime).parameters(Cd90AutoScheduleParameters.builder()
                        .taskTimeoutMinutes(30).build()).build();
    }
}
