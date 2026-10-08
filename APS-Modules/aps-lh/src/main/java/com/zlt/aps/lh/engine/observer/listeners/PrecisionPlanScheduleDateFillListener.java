package com.zlt.aps.lh.engine.observer.listeners;

import com.zlt.aps.lh.api.enums.EventTypeEnum;
import com.zlt.aps.lh.engine.observer.IScheduleEventListener;
import com.zlt.aps.lh.engine.observer.ScheduleEvent;

/**
 * 旧完成监听器兼容类型。精度安排日期已迁入排程原子替换事务，不再注册或处理完成事件。
 * 保留类型以兼容旧调用代码，禁止在这里启动独立回填事务。
 */
@Deprecated
public class PrecisionPlanScheduleDateFillListener implements IScheduleEventListener {

    /** @param event 旧完成事件，不再执行回填 */
    @Override
    public void onEvent(ScheduleEvent event) {
    }

    /** @param eventType 事件类型 @return 旧监听入口始终退出 */
    @Override
    public boolean supports(EventTypeEnum eventType) {
        return false;
    }
}
