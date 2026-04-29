package com.zlt.aps.mp.factory.event;

import com.zlt.aps.maindata.event.BaseEvent;
import com.zlt.aps.mp.api.domain.dto.MonthPlanFinalizedEventDto;
import lombok.Getter;

/**
 * 月计划调整确认事件
 *
 * @author zlt
 * @since 2026/4/29
 */
@Getter
public class MonthPlanAdjustedEvent extends BaseEvent {

    /**
     * 事件参数，复用月计划定稿事件参数承载推送所需的年月、工厂、版本和最终计划数据。
     */
    private final MonthPlanFinalizedEventDto eventDto;

    /**
     * 构造月计划调整确认事件。
     *
     * @param source          事件源
     * @param eventModuleType 事件模块类型
     * @param operator        操作人
     * @param eventDto        事件参数
     * @throws IllegalArgumentException 当父类事件参数校验不通过时抛出
     */
    public MonthPlanAdjustedEvent(Object source, String eventModuleType, String operator, MonthPlanFinalizedEventDto eventDto) {
        super(source, eventModuleType, operator);
        this.eventDto = eventDto;
    }
}
