package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Sets;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MpOverdueSkuEntityMapper;
import com.zlt.aps.maindata.service.IMpOverdueSkuService;
import com.zlt.aps.monthplan.api.domain.entity.MpOverdueSku;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpOverdueSkuServiceImpl.java
 * 描    述：MpOverdueSkuServiceImpl超期SKU业务层处理
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class MpOverdueSkuServiceImpl extends AbstractDocService<MpOverdueSku> implements IMpOverdueSkuService
{
    @Autowired
    private MpOverdueSkuEntityMapper mpOverdueSkuEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0218";
    }


    @Override
    public Set<String> excludeOverdueCycleProduction() {
        // 构建查询条件
        QueryWrapper<MpOverdueSku> queryWrapper = buildExtendedRangeQueryOptimized();
        queryWrapper.eq("IS_OVERDUE_CYCLE", YesOrNoEnum.YES.getValue());
        List<MpOverdueSku> list = this.mpOverdueSkuEntityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(list)){
            return Sets.newHashSet();
        }
        return list.stream().map(MpOverdueSku::getMaterialCode).collect(Collectors.toSet());
    }

    @Override
    public Set<String> excludeOverduePrecedentProduction() {
        // 构建查询条件
        QueryWrapper<MpOverdueSku> queryWrapper = buildExtendedRangeQueryOptimized();
        queryWrapper.eq("IS_OVERDUE_REGULAR", YesOrNoEnum.YES.getValue());
        List<MpOverdueSku> list = this.mpOverdueSkuEntityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(list)){
            return Sets.newHashSet();
        }
        return list.stream().map(MpOverdueSku::getMaterialCode).collect(Collectors.toSet());
    }

    @Override
    public boolean checkOverdue(SupplyOrderPool supplyOrderPool) {
        // 构建查询条件
        QueryWrapper<MpOverdueSku> queryWrapper = buildExtendedRangeQueryOptimized();
        queryWrapper.eq("FACTORY_CODE",supplyOrderPool.getFactoryCode());
        queryWrapper.eq("MATERIAL_CODE",supplyOrderPool.getMaterialCode());
        if(ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP.equals(supplyOrderPool.getOrderType())) {
            queryWrapper.eq("IS_OVERDUE_REGULAR", YesOrNoEnum.YES.getValue());
        }else{
            queryWrapper.eq("IS_OVERDUE_CYCLE", YesOrNoEnum.YES.getValue());
        }
        return this.mpOverdueSkuEntityMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 优化的实现：使用数值比较（性能更好）
     */
    public QueryWrapper<MpOverdueSku> buildExtendedRangeQueryOptimized() {
        QueryWrapper<MpOverdueSku> queryWrapper = new QueryWrapper<>();
        //2025-01~2026-01
        YearMonth current = YearMonth.now();
        // 计算最近N个月的起始点
        YearMonth recentStart = current.minusMonths(12);
        // 将年月转换为数值
        // 2025-01
        int recentStartValue = recentStart.getYear() * 100 + recentStart.getMonthValue();
        int currentValue = current.getYear() * 100 + current.getMonthValue();
        // 使用数据库表达式
        String expression = "year * 100 + month";
        // 构建条件：expression < recentStartValue OR expression BETWEEN recentStartValue AND currentValue
        queryWrapper.and(wrapper -> {
            wrapper.or(w -> w.apply(expression + " = {0}", recentStartValue));
            wrapper.or(w -> w.apply(expression + " BETWEEN {0} AND {1}",
                recentStartValue, currentValue));
        });
        return queryWrapper;
    }
}
