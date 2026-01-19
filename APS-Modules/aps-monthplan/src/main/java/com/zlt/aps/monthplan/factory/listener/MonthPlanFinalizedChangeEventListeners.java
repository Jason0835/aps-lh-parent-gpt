package com.zlt.aps.monthplan.factory.listener;

import com.alibaba.fastjson.JSONObject;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.monthplan.api.domain.dto.MonthPlanFinalizedEventDto;
import com.zlt.aps.monthplan.demand.service.impl.OrderAllocationServiceImpl;
import com.zlt.aps.monthplan.factory.event.MonthPlanFinalizedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * 月计划定稿事件监听器
 *
 * @author Chen
 */
@Slf4j
@Component
public class MonthPlanFinalizedChangeEventListeners {

    @Autowired
    private OrderAllocationServiceImpl orderAllocationService;

    @Autowired
    private IMpMonthPlanMonitorService mpMonthPlanMonitorService;

    /**
     * 异步处理月计划定稿事件
     */
    @Async
    @EventListener
    public void handleMonthPlanFinalizedEvent(MonthPlanFinalizedEvent event) {
        try {
            MonthPlanFinalizedEventDto eventDto = event.getEventDto();
            log.info("月计划定稿事件开始执行，事件ID：{}，事件参数：{}", event.getEventId(), JSONObject.toJSONString(eventDto));
            // 4、调用世超的分摊接口
            // 4.1、OrderAllocationServiceImpl.allocateProductionByMonth
            orderAllocationService.allocateProductionByMonth(eventDto.getYear(), eventDto.getMonth(),
                    eventDto.getFactoryCode(), eventDto.getMonthPlanVersion(), eventDto.getMaterialTotalQtyMap());
            // 4.2、调用生成原材料需求计划 -- TODO

            // 5、写入月度硫化监控表
            // t_mp_month_plan_monitor
            // 上机日期 = 排产周期的开始日 +  (startDay -1 )
            mpMonthPlanMonitorService.insertMonitorByFinalList(eventDto.getParam(), eventDto.getFinalList());
            log.info("月计划定稿事件执行完成");
        } catch (Exception e) {
            log.error("月计划定稿事件执行失败，事件ID：{}", event.getEventId(), e);
        }
    }
}
