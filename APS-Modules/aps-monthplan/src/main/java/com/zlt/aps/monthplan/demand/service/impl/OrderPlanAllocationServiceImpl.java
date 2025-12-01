package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.monthplan.api.domain.entity.OrderPlanAllocation;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.aps.monthplan.api.domain.vo.OrderPlanAllocationReportVo;
import com.zlt.aps.monthplan.demand.mapper.OrderPlanAllocationMapper;
import com.zlt.aps.monthplan.demand.service.IOrderPlanAllocationService;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：OrderPlanAllocationServiceImpl.java
 * 描    述：OrderPlanAllocationServiceImpl月度销售计划订单分配结果业务层处理
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
@Slf4j
@Service
public class OrderPlanAllocationServiceImpl implements IOrderPlanAllocationService {

    private final OrderPlanAllocationMapper orderPlanAllocationMapper;

    public OrderPlanAllocationServiceImpl(OrderPlanAllocationMapper orderPlanAllocationMapper) {
        this.orderPlanAllocationMapper = orderPlanAllocationMapper;
    }

    @Override
    public List<OrderPlanAllocation> selectList(OrderPlanAllocation queryVO) {
        QueryWrapper<OrderPlanAllocation> wrapper = Wrappers.query();
        builderCondition(wrapper, queryVO);
        return orderPlanAllocationMapper.selectList(wrapper);
    }

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    @Override
    public List<String> versionList(OrderPlanAllocation query) {
        if (query.getYear() == null || query.getMonth() == null || StringUtils.isBlank(query.getFactoryCode())) {
            return Collections.emptyList();
        }
        return orderPlanAllocationMapper.versionList(query);
    }

    /**
     * 查询提报的SAP个数、提报的SAP总量
     */
    @Override
    public void statistics(MonthPlanStatisticsVo statisticsVo, OrderPlanAllocation queryVO) {
        QueryWrapper<OrderPlanAllocation> wrapper = Wrappers.query();
        builderCondition(wrapper, queryVO);

        wrapper.select("count(distinct PRODUCT_CODE) as reportCount,sum(PLAN_QTY) as reportSum");
        List<Map<String, Object>> mapList = orderPlanAllocationMapper.selectMaps(wrapper);
        if (CollectionUtils.isNotEmpty(mapList)) {
            Map<String, Object> resultMap = mapList.get(0);
            if (resultMap != null && resultMap.get("reportCount") != null) {
                statisticsVo.setReportCount(Long.parseLong(resultMap.get("reportCount").toString()));
            }
            if (resultMap != null && resultMap.get("reportSum") != null) {
                statisticsVo.setReportSum(Long.parseLong(resultMap.get("reportSum").toString()));
            }
        }
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void builderCondition(QueryWrapper<OrderPlanAllocation> queryWrapper, OrderPlanAllocation queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customCode")), "CUSTOM_CODE", queryVO.getFieldValueByFieldName("customCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customName")), "CUSTOM_NAME", queryVO.getFieldValueByFieldName("customName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planQty")), "PLAN_QTY", queryVO.getFieldValueByFieldName("planQty"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tradeMode")), "TRADE_MODE", queryVO.getFieldValueByFieldName("tradeMode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salePerson")), "SALE_PERSON", queryVO.getFieldValueByFieldName("salePerson"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImportantCustom")), "IS_IMPORTANT_CUSTOM", queryVO.getFieldValueByFieldName("isImportantCustom"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEnsurePlan")), "IS_ENSURE_PLAN", queryVO.getFieldValueByFieldName("isEnsurePlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEmergency")), "IS_EMERGENCY", queryVO.getFieldValueByFieldName("isEmergency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("contractNo")), "CONTRACT_NO", queryVO.getFieldValueByFieldName("contractNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nation")), "NATION", queryVO.getFieldValueByFieldName("nation"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("submissionDate")), "SUBMISSION_DATE", queryVO.getFieldValueByFieldName("submissionDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryDateDue")), "DELIVERY_DATE_DUE", queryVO.getFieldValueByFieldName("deliveryDateDue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("allocationQty")), "ALLOCATION_QTY", queryVO.getFieldValueByFieldName("allocationQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("produceQtyDue")), "PRODUCE_QTY_DUE", queryVO.getFieldValueByFieldName("produceQtyDue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
    }

    @Override
    public OrderPlanAllocationReportVo getSummaryVo(OrderPlanAllocation orderPlanAllocation) {
        OrderPlanAllocationReportVo reportVo = orderPlanAllocationMapper.getSummaryVo(orderPlanAllocation);
        orderPlanAllocation.setMonth(orderPlanAllocation.getMonth() - 1);
        Double stockSum = orderPlanAllocationMapper.selectStockSum(orderPlanAllocation);
        reportVo.setBeforeHedgingStockQty(stockSum);
        return reportVo;
    }
}
