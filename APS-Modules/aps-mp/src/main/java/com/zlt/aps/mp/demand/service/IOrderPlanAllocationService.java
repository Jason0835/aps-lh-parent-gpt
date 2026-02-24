package com.zlt.aps.mp.demand.service;


import com.zlt.aps.monthplan.api.domain.entity.OrderPlanAllocation;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.api.domain.vo.OrderPlanAllocationReportVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IOrderPlanAllocationService.java
 * 描    述：IOrderPlanAllocationService月度销售计划订单分配结果后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
public interface IOrderPlanAllocationService {

    List<OrderPlanAllocation> selectList(OrderPlanAllocation queryVO);

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    List<String> versionList(OrderPlanAllocation query);

    /**
     * 查询提报的SAP个数、提报的SAP总量
     */
    void statistics(MonthPlanStatisticsVo statisticsVo, OrderPlanAllocation orderPlanAllocation);

    OrderPlanAllocationReportVo getSummaryVo(OrderPlanAllocation orderPlanAllocation);
}
