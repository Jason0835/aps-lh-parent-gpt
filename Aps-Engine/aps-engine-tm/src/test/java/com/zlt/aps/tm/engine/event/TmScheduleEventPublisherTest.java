package com.zlt.aps.tm.engine.event;

import com.zlt.aps.tm.api.enums.TmScheduleEventTypeEnum;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * 胎面排程事件发布器测试。
 *
 * <p>验证调度事件会同步通知全部监听器，监听器异常不被吞掉。</p>
 */
public class TmScheduleEventPublisherTest {

    @Test
    public void publishShouldNotifyAllListeners() {
        AtomicInteger counter = new AtomicInteger(0);
        TmScheduleEventPublisher publisher = new TmScheduleEventPublisher(Arrays.asList(
                event -> counter.incrementAndGet(),
                event -> counter.addAndGet(10)
        ));

        publisher.publish(TmScheduleEvent.of(new TmScheduleContext(),
                TmScheduleEventTypeEnum.AUTO_SCHEDULE, "AUTO"));

        assertEquals(11, counter.get());
    }

    @Test(expected = IllegalStateException.class)
    public void publishShouldNotSwallowListenerException() {
        TmScheduleEventPublisher publisher = new TmScheduleEventPublisher(java.util.Collections.singletonList(event -> {
            throw new IllegalStateException("listener failed");
        }));

        publisher.publish(TmScheduleEvent.of(new TmScheduleContext(),
                TmScheduleEventTypeEnum.AUTO_SCHEDULE, "AUTO"));
    }
}
