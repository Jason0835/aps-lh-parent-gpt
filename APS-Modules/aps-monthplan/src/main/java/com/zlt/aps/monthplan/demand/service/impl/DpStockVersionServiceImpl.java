package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpStockVersion;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.demand.mapper.DpStockVersionEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpStockVersionServiceImpl.java
 * 描    述：DpStockVersionServiceImpl需求计划_版本库存业务层处理
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DpStockVersionServiceImpl extends AbstractDocService<DpStockVersion>  implements IDpStockVersionService {
    private final DpStockVersionEntityMapper dpStockVersionEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "2025122021";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122021");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpStockVersion docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpStockVersion.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<DpStockVersion> insertBatchData(DpDemandPlan createCondition, String monthPlanVersion, Map<String, List<MdmProductStock>> finishedProductStockMap) {
        if (CollectionUtils.isEmpty(finishedProductStockMap)) {
            return Collections.emptyList();
        }
        List<DpStockVersion> list = Lists.newArrayList();
        List<MdmProductStock> finishedProductStocks = flattenStockMap(finishedProductStockMap);
        finishedProductStocks.forEach(finishedProductStock -> {
            DpStockVersion requireStock = this.buildRequireStock(createCondition,monthPlanVersion,finishedProductStock);
            list.add(requireStock);
        });
        this.baseDao.insertBatch(list);
        return list;
    }

    @Override
    public Map<String, Map<String, Integer>> calculateStockQty() {
        List<DpStockVersion> list = this.findCurrentStockVersion();
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyMap();
        }
        YearMonth now = YearMonth.now();
        YearMonth lastOneYear = now.minusYears(BigDecimal.ONE.intValue());
        YearMonth lastTwoYear = now.minusYears(BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue());
        Map<String, Map<String, Integer>> result = new HashMap<>();
        Map<String,List<DpStockVersion>> stockMap =   list.stream().collect(Collectors.groupingBy(DpStockVersion::getMonthPlanVersionKey));
        stockMap.forEach((key,value)->{
            Map<String, Integer> map = Maps.newHashMap();
            int totalStockQty = value.stream().filter(item -> null != item.getStockQty()).mapToInt(DpStockVersion::getStockQty).sum();
            int currentStockQty = value.stream().filter(item -> filter(item,now)).mapToInt(DpStockVersion::getStockQty).sum();
            int lastOneYearStockQty = value.stream().filter(item -> filter(item,lastOneYear)).mapToInt(DpStockVersion::getStockQty).sum();
            int lastTwoYearStockQty = value.stream().filter(item -> filter(item,lastTwoYear)).mapToInt(DpStockVersion::getStockQty).sum();
            map.put(StringConstant.ZERO,totalStockQty);
            map.put(StringConstant.ONE,currentStockQty);
            map.put(StringConstant.TWO,lastOneYearStockQty);
            map.put(StringConstant.THREE,lastTwoYearStockQty);
            result.put(key, map);
        });
        return result;
    }

    private List<DpStockVersion> findCurrentStockVersion() {
        LambdaQueryWrapper<DpStockVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DpStockVersion::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.dpStockVersionEntityMapper.selectList(wrapper);
    }

    private boolean filter(DpStockVersion item, YearMonth yearMonth) {
        if(StringUtils.isBlank(item.getWeekYear()) || null == item.getStockQty()){
            return false;
        }
        if(yearMonth.equals(YearMonth.now())){
            String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(yearMonth.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
            String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
            int yearWeek = Integer.parseInt(transformed);
            return yearWeek >= Integer.parseInt(currentYearMonthStr);
        }
        if(yearMonth.equals(YearMonth.now().minusYears(BigDecimal.ONE.intValue()))){
            String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(yearMonth.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
            String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
            int yearWeek = Integer.parseInt(transformed);
            YearMonth now = YearMonth.now();
            String nowYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(now.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
            return yearWeek >= Integer.parseInt(currentYearMonthStr) && yearWeek < Integer.parseInt(nowYearMonthStr);
        }
        YearMonth lastOneYearWeek = YearMonth.now().minusYears(BigDecimal.ONE.intValue());
        String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(lastOneYearWeek.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
        String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
        int yearWeek = Integer.parseInt(transformed);
        return yearWeek < Integer.parseInt(currentYearMonthStr);
    }

    private DpStockVersion buildRequireStock(DpDemandPlan createCondition, String monthPlanVersion, MdmProductStock finishedProductStock) {
        DpStockVersion requireStock = new DpStockVersion();
        BeanUtils.copyProperties(finishedProductStock, requireStock);
        requireStock.setId(null);
        requireStock.setRequireVersion(monthPlanVersion);
        requireStock.setIsDelete(YesOrNoEnum.NO.getValue());
        requireStock.setRemainingQty(finishedProductStock.getLeftOverQty());
        requireStock.setBaseVale(null);
        requireStock.setYear(createCondition.getYear());
        requireStock.setMonth(createCondition.getMonth());
        return requireStock;
    }

    /**
     * 将Map转换为List<MpFinishedProductStock>
     */
    public List<MdmProductStock> flattenStockMap(
        Map<String, List<MdmProductStock>> finishedProductStockMap) {
        // 使用Stream扁平化转换
        return finishedProductStockMap.values().stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
