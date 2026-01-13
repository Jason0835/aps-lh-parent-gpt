package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.utils.JsonUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultServiceImpl.java
 * 描    述：FactoryMonthPlanProductionFinalResultServiceImpl工厂月生产计划-最终排产计划定稿业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FactoryMonthPlanProductionFinalResultServiceImpl extends ServiceImpl<FactoryMonthPlanProductionFinalResultEntityMapper, FactoryMonthPlanProductionFinalResult> implements IFactoryMonthPlanProductionFinalResultService {
    private final BaseDao baseDao;

    @Override
    public List<FactoryMonthPlanProductionFinalResult> getDataList(FactoryMonthPlanProductionFinalResult condition) {
        QueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, condition);
        List<FactoryMonthPlanProductionFinalResult> dataList = this.baseMapper.selectList(queryWrapper);
        dealList(dataList);
        return dataList;
    }

    @Override
    public Map<String, Integer> calculateStructureFrequency() {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> list = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> structureFrequencyMap = Maps.newHashMap();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> map = list.stream().collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));
        map.forEach((materialCode, value) -> {
            Set<Integer> yearMonths = value.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonth).collect(Collectors.toSet());
            structureFrequencyMap.put(materialCode, yearMonths.size());
        });
        return structureFrequencyMap;
    }

    @Override
    public int calculateStructureFrequency(String materialCode) {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCode)
                .ge(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> list = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return BigDecimal.ZERO.intValue();
        }
        Set<Integer> yearMonths = list.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonth).collect(Collectors.toSet());
        return yearMonths.size();
    }

    @Override
    public Map<String, Integer> calculateMonthSurplus(String requireVersion, List<MdmProductStock> finishedProductStocks) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }
        List<Date> stockDates = finishedProductStocks.stream().map(MdmProductStock::getStockDate).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(stockDates)) {
            return Collections.emptyMap();
        }
        Date maxDate = stockDates.stream()
                .filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        if (null == maxDate) {
            return Collections.emptyMap();
        }
        int year = DateUtils.getYear(maxDate);
        int month = DateUtils.getMonthsByYear(maxDate);
        int stockDay = DateUtils.getDaysByMonth(maxDate);
        // 获取当前年月
        String yearMonth = String.format("%s%02d", year, month);
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> monthSurplusMap = Maps.newHashMap();
        List<MdmMonthSurplus> result = Lists.newArrayList();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
        groupByMaterialCode.forEach((key, value) -> {
            int planSurplusQty = this.calculateMonthSurplus(value, stockDay);
            if (planSurplusQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            MdmMonthSurplus entity = new MdmMonthSurplus();
            entity.setBaseVale(null);
            entity.setIsDelete(ApsConstant.APS_YES_NO_0);
            entity.setPlanSurplusQty(BigDecimal.valueOf(planSurplusQty));
            entity.setFactoryCode(value.get(0).getFactoryCode());
            entity.setYear(value.get(0).getYear());
            entity.setMonth(value.get(0).getMonth());
            entity.setRequireVersion(requireVersion);
            entity.setProductTypeCode(value.get(0).getProductTypeCode());
            entity.setBrand(value.get(0).getBrand());
            entity.setMaterialCode(value.get(0).getMaterialCode());
            entity.setMaterialDesc(value.get(0).getMaterialDesc());
            entity.setStructureName(value.get(0).getStructureName());
            result.add(entity);
            monthSurplusMap.put(key, planSurplusQty);
        });
        if (CollectionUtils.isNotEmpty(result)) {
            this.baseDao.insertBatch(result);
        }
        return monthSurplusMap;
    }


    @Override
    public Map<String, Integer> calculateMonthSurplusNoSave(List<MdmProductStock> finishedProductStocks, String yearMonth, int days) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getYearMonth, Integer.valueOf(yearMonth))
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> monthSurplusMap = Maps.newHashMap();
        Map<String, List<FactoryMonthPlanProductionFinalResult>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
        groupByMaterialCode.forEach((key, value) -> {
            int planSurplusQty = this.calculateMonthSurplus(value, days);
            if (planSurplusQty <= BigDecimal.ZERO.longValue()) {
                return;
            }
            monthSurplusMap.put(key, planSurplusQty);
        });
        return monthSurplusMap;
    }


    private int calculateMonthSurplus(List<FactoryMonthPlanProductionFinalResult> productionFinalResults, int stockDay) {
        int totalMonthSuplus = BigDecimal.ZERO.intValue();
        //统计汇总值
        Integer[] dayList = FactoryConstant.PRODUCTION_CYCLE;
        for (FactoryMonthPlanProductionFinalResult productionFinalResult : productionFinalResults) {
            for (Integer day : dayList) {
                if (day < stockDay) {
                    continue;
                }
                String fieldName = "day".concat(String.valueOf(day));
                int dayValue;
                Object value = productionFinalResult.getFieldValueByFieldName(fieldName);
                if (null == value) {
                    dayValue = BigDecimal.ZERO.intValue();
                } else {
                    dayValue = (Integer) value;
                }
                totalMonthSuplus = totalMonthSuplus + dayValue;
            }
        }
        return totalMonthSuplus;
    }


    @Override
    public List<FactoryMonthPlanProductionFinalResult> findProductionFinalResult(MpFactoryProductionVersion finalVersion) {
        if (null == finalVersion) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = Wrappers.lambdaQuery(FactoryMonthPlanProductionFinalResult.class)
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, finalVersion.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, finalVersion.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, finalVersion.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getMonthPlanVersion, finalVersion.getMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        return this.list(queryWrapper);
    }

    private Map<String, List<FactoryMonthPlanProductionFinalResult>> getGroupMonthProdFinalPlanByMaterialCode(List<FactoryMonthPlanProductionFinalResult> factoryMonthPlanProdFinals) {
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        return factoryMonthPlanProdFinals.stream().collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getGroupKey));
    }

    /**
     * 解析不排产原因
     *
     * @param list
     */
    private void dealList(List<FactoryMonthPlanProductionFinalResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Locale language = SecurityUtils.getUserLang();
        JsonUtils.parseJsonRemarkList(list, language.toString(), "reason");
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper 查询构建器
     * @param condition    查询条件值对象
     */
    protected void builderCondition(QueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper, FactoryMonthPlanProductionFinalResult condition) {
        /**
         * 工厂、年份、月份、需求版本、排产版本、产品品类
         */
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getFactoryCode()), "FACTORY_CODE", condition.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getYear()), "YEAR", condition.getYear());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getMonth()), "MONTH", condition.getMonth());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getMonthPlanVersion()), "MONTH_PLAN_VERSION", condition.getMonthPlanVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProductionVersion()), "PRODUCTION_VERSION", condition.getProductionVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProductTypeCode()), "PRODUCT_TYPE_CODE", condition.getProductTypeCode());
        /**
         * 物料相关
         */
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMaterialCode()), "MATERIAL_CODE", condition.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getMaterialDesc()), "MATERIAL_DESC", condition.getMaterialDesc());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getConstructionStage()), "CONSTRUCTION_STAGE", condition.getConstructionStage());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getBrand()), "BRAND", condition.getBrand());
        queryWrapper.eq(PubUtil.isNotEmpty(condition.getProSize()), "PRO_SIZE", condition.getProSize());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getSpecifications()), "SPECIFICATIONS", condition.getSpecifications());
        queryWrapper.like(PubUtil.isNotEmpty(condition.getPattern()), "PATTERN", condition.getPattern());
    }

}
