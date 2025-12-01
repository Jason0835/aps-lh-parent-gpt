package com.zlt.aps.job.task;

import com.zlt.aps.job.service.IMonthPlanTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 月计划任务
 *
 * @author zlt
 */
@Component("monthPlanTask")
public class MonthPlanTask {

    @Autowired
    private IMonthPlanTaskService iMonthPlanTaskService;

    /**
     * 内销历史销售订单同步
     */
    public void syncInHisSaleOrder() {
        iMonthPlanTaskService.syncInHisSaleOrder();
    }

    /**
     * 同步MES的SAP与施工关系
     */
    public void syncProductConstructionInfo() {
        iMonthPlanTaskService.syncProductConstructionInfo();
    }
}
