package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Sets;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.MpOverdueSku;
import com.zlt.aps.monthplan.demand.mapper.MpOverdueSkuEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpOverdueSkuService;
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
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        // 构建查询条件
        QueryWrapper<MpOverdueSku> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_OVERDUE_CYCLE", YesOrNoEnum.YES.getValue());
        // 方法1：使用复杂条件构造
        // 方法1：使用复杂条件构造
        queryWrapper.and(wrapper -> {
            // 起始年月之后的数据
            wrapper.or(w -> w
                .eq("year", startYearMonth.getYear())
                .ge("month", startYearMonth.getMonthValue())
            );

            // 中间完整年份
            for (int year = startYearMonth.getYear() + 1;
                 year < currentYearMonth.getYear();
                 year++) {
                int finalYear = year;
                wrapper.or(w -> w.eq("year", finalYear));
            }

            // 结束年月之前的数据
            wrapper.or(w -> w
                .eq("year", currentYearMonth.getYear())
                .le("month", currentYearMonth.getMonthValue())
            );
        });
        List<MpOverdueSku> list = this.mpOverdueSkuEntityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(list)){
            return Sets.newHashSet();
        }
        return list.stream().map(MpOverdueSku::getMaterialCode).collect(Collectors.toSet());
    }

    @Override
    public Set<String> excludeOverduePrecedentProduction() {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        // 构建查询条件
        QueryWrapper<MpOverdueSku> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_OVERDUE_REGULAR", YesOrNoEnum.YES.getValue());
        // 方法1：使用复杂条件构造
        // 方法1：使用复杂条件构造
        queryWrapper.and(wrapper -> {
            // 起始年月之后的数据
            wrapper.or(w -> w
                .eq("year", startYearMonth.getYear())
                .ge("month", startYearMonth.getMonthValue())
            );

            // 中间完整年份
            for (int year = startYearMonth.getYear() + 1;
                 year < currentYearMonth.getYear();
                 year++) {
                int finalYear = year;
                wrapper.or(w -> w.eq("year", finalYear));
            }

            // 结束年月之前的数据
            wrapper.or(w -> w
                .eq("year", currentYearMonth.getYear())
                .le("month", currentYearMonth.getMonthValue())
            );
        });
        List<MpOverdueSku> list = this.mpOverdueSkuEntityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(list)){
            return Sets.newHashSet();
        }
        return list.stream().map(MpOverdueSku::getMaterialCode).collect(Collectors.toSet());
    }
}
