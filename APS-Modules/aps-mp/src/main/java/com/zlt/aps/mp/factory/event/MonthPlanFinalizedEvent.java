package com.zlt.aps.mp.factory.event;

import com.zlt.aps.maindata.event.BaseEvent;
import com.zlt.aps.monthplan.api.domain.dto.MonthPlanFinalizedEventDto;
import lombok.Getter;

/**
 * 月计划定稿事件
 *
 * @author Chen
 * @since 2026/1/15
 */
@Getter
public class MonthPlanFinalizedEvent extends BaseEvent {

    /**
     * 事件参数
     */
    private final MonthPlanFinalizedEventDto eventDto;

    /**
     * 事件参数
     *
     * @param source          事件源
     * @param eventModuleType 事件模块类型
     * @param operator        操作人
     * @param eventDto        事件参数
     */
    public MonthPlanFinalizedEvent(Object source, String eventModuleType, String operator, MonthPlanFinalizedEventDto eventDto) {
        super(source, eventModuleType, operator);
        this.eventDto = eventDto;
    }
}
