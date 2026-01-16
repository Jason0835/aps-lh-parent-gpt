package com.zlt.aps.maindata.event.publisher;

import com.zlt.aps.maindata.enums.EventModuleTypeEnum;
import com.zlt.aps.maindata.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事件发布
 *
 * @author zlt
 */
@Slf4j
@Component
public class EventPublisher {

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 发布事件的统一方法
     */
    public void publish(BaseEvent event) {
        // 1、日志记录（事件发布前）
        log.info("发布事件：{}, 事件ID：{}, 事件模块类型：{}, 操作人：{}",
                event.getClass().getSimpleName(),
                event.getEventId(),
                EventModuleTypeEnum.getByCode(event.getEventModuleType()),
                event.getOperator());
        // 2、发布事件
        publisher.publishEvent(event);
    }
}
