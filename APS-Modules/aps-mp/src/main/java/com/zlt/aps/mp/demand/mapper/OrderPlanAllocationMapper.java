package com.zlt.aps.mp.demand.mapper;

import com.zlt.aps.mp.api.domain.entity.OrderPlanAllocation;
import com.zlt.aps.mp.api.domain.vo.OrderPlanAllocationReportVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：OrderPlanAllocationMapper.java
 * 描    述：月度销售计划订单分配结果Mapper接口
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
@Mapper
public interface OrderPlanAllocationMapper extends CommBaseMapper<OrderPlanAllocation> {

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    List<String> versionList(OrderPlanAllocation query);

    Double selectStockSum(OrderPlanAllocation orderPlanAllocation);

    /**
     * 根据查询条件查询统计数据
     *
     * @param orderPlanAllocation 查询条件
     * @return 结果
     */
    OrderPlanAllocationReportVo getSummaryVo(OrderPlanAllocation orderPlanAllocation);
}
