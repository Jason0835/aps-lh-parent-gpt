package com.zlt.aps.lh.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.common.engine.utils.LhSkuSurplusCalculator;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.mapper.LhScheFinishQtyMapper;
import com.zlt.aps.lh.service.ILhDayPlanAdjustRequireService;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.engine.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化余量计算：数据加载处理
 * <p>
 * 数据加载，包含 月计划数据，完成量数据，日硫化调整量等
 * </p>
 *
 * @author ZLT
 * @date 20261103
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LhSkuSurplusDataLoadHandler {

    private final FactoryMonthPlanProductionFinalResultMapper monthPlanFinalResultMapper;

    private final LhDayFinishQtyMapper lhDayFinishQtyMapper;

    private final LhScheFinishQtyMapper lhScheFinishQtyMapper;

    private final ILhDayPlanAdjustRequireService lhDayPlanAdjustRequireService;

    /**
     * 获取余量计算相关数据
     *
     * @param factoryCode
     * @param allProductionDate
     * @return
     */
    public LhSkuSurplusDataInfo loadData(String factoryCode, List<Date> allProductionDate) {
        if (StringUtils.isBlank(factoryCode) || CollectionUtils.isEmpty(allProductionDate)) {
            return null;
        }
        List<String> factoryList = Lists.newArrayList();
        factoryList.add(factoryCode);
        return loadData(allProductionDate, factoryList);
    }

    /**
     * 加载余量计算相关数据
     *
     * @param allProductionDate
     * @param factoryList
     * @return
     */
    public LhSkuSurplusDataInfo loadData(List<Date> allProductionDate, List<String> factoryList) {
        if (CollectionUtils.isEmpty(allProductionDate)) {
            return null;
        }
        //加载月计划数据
        allProductionDate.sort(Comparator.naturalOrder());
        Date first = allProductionDate.get(BigDecimal.ZERO.intValue());
        List<FactoryMonthPlanProductionFinalResult> allMonthPlanList = loadMonthPlanList(allProductionDate, factoryList);
        if (CollectionUtils.isEmpty(allMonthPlanList)) {
            return null;
        }
        LhSkuSurplusDataInfo dataInfo = new LhSkuSurplusDataInfo();
        dataInfo.setAllMonthPlanList(allMonthPlanList);
        //按年-月倒序
        Comparator sort = Comparator.comparing(FactoryMonthPlanProductionFinalResult::getYear, Comparator.reverseOrder())
                .thenComparing(FactoryMonthPlanProductionFinalResult::getMonth, Comparator.reverseOrder());
        allMonthPlanList.sort(sort);
        FactoryMonthPlanProductionFinalResult lastMonth = allMonthPlanList.get(BigDecimal.ZERO.intValue());
        //库存抓取日
        dataInfo.setPlanStartDate(lastMonth.getStockCaptureDate());
        //获取硫化日计划调整量
        Set<YearMonth> yearMonthSet = allMonthPlanList.stream().map(FactoryMonthPlanProductionFinalResult::getYearMonthInfo).collect(Collectors.toSet());
        List<LhDayPlanAdjustVo> allLhDayPlanAdjust = loadLhDayPlanAdjust(yearMonthSet, factoryList);
        dataInfo.setAllLhDayPlanAdjustList(allLhDayPlanAdjust);
        //获取日完成量
        loadFinishQtyInfo(dataInfo, first, factoryList);
        return dataInfo;
    }

    /**
     * 加载月计划信息
     *
     * @param allProductionDate
     * @param factoryList
     * @return
     */
    private List<FactoryMonthPlanProductionFinalResult> loadMonthPlanList(List<Date> allProductionDate, List<String> factoryList) {
        allProductionDate.sort(Comparator.naturalOrder());
        Date first = allProductionDate.get(BigDecimal.ZERO.intValue());
        List<FactoryMonthPlanProductionFinalResult> allMonthPlanList = Lists.newArrayList();
        YearMonth firstMonth = LhSkuSurplusCalculator.getFirstYearMonth(allProductionDate);
        YearMonth lastMonth = LhSkuSurplusCalculator.getLastYearMonth(allProductionDate);
        //第一个月必加载
        List<FactoryMonthPlanProductionFinalResult> firstMonthList = getMonthPlanList(firstMonth, factoryList, Lists.newArrayList());
        if (!CollectionUtils.isEmpty(firstMonthList)) {
            allMonthPlanList.addAll(firstMonthList);
        }
        if (!firstMonth.equals(lastMonth)) {
            //跨月，则直接先取跨月的数据
            List<FactoryMonthPlanProductionFinalResult> lastMonthList = getMonthPlanList(lastMonth, factoryList, Lists.newArrayList());
            if (!CollectionUtils.isEmpty(lastMonthList)) {
                allMonthPlanList.addAll(lastMonthList);
                return allMonthPlanList;
            }
            //跨月数据没有，则加载前一个月
            YearMonth previousFirstMonth = firstMonth.minusMonths(BigDecimal.ONE.intValue());
            List<FactoryMonthPlanProductionFinalResult> previousFirstMonthList = getMonthPlanList(previousFirstMonth, factoryList, Lists.newArrayList());
            if (!CollectionUtils.isEmpty(previousFirstMonthList)) {
                allMonthPlanList.addAll(previousFirstMonthList);
            }
            return allMonthPlanList;
        }
        //同一个月，则先看后一个月
        YearMonth nextMonth = firstMonth.plusMonths(BigDecimal.ONE.intValue());
        YearMonth previousFirstMonth = firstMonth.minusMonths(BigDecimal.ONE.intValue());
        List<FactoryMonthPlanProductionFinalResult> lastMonthList = getMonthPlanList(nextMonth, factoryList, Lists.newArrayList());
        if (!CollectionUtils.isEmpty(lastMonthList)) {
            Date stockCaptureDate = lastMonthList.get(BigDecimal.ZERO.intValue()).getStockCaptureDate();
            //排产日在前，则加载前一个月
            if (first.before(stockCaptureDate)) {
                List<FactoryMonthPlanProductionFinalResult> previousFirstMonthList = getMonthPlanList(previousFirstMonth, factoryList, Lists.newArrayList());
                if (!CollectionUtils.isEmpty(previousFirstMonthList)) {
                    allMonthPlanList.addAll(previousFirstMonthList);
                }
                return allMonthPlanList;
            }
            //加载后一个月
            allMonthPlanList.addAll(lastMonthList);
        }
        //后一个月还没有定稿，则加载前一个月
        List<FactoryMonthPlanProductionFinalResult> previousFirstMonthList = getMonthPlanList(previousFirstMonth, factoryList, Lists.newArrayList());
        if (!CollectionUtils.isEmpty(previousFirstMonthList)) {
            allMonthPlanList.addAll(previousFirstMonthList);
        }
        return allMonthPlanList;
    }

    /**
     * 加载日硫化调整量
     *
     * @param yearMonthSet
     * @param factoryList
     * @return
     */
    private List<LhDayPlanAdjustVo> loadLhDayPlanAdjust(Set<YearMonth> yearMonthSet, List<String> factoryList) {
        if (CollectionUtils.isEmpty(yearMonthSet)) {
            return Collections.emptyList();
        }
        List<LhDayPlanAdjustVo> allLhDayPlanAdjust = Lists.newArrayList();
        List<YearMonth> yearMonthList = Lists.newArrayList(yearMonthSet);
        Comparator sort = Comparator.comparing(YearMonth::getYear)
                .thenComparing(YearMonth::getMonthValue);
        yearMonthList.sort(sort);
        YearMonth beforeMonth = yearMonthList.get(BigDecimal.ZERO.intValue());
        List<LhDayPlanAdjustVo> beforeMonth4AdjustList = getYearMonthLhDayPlanAdjust4(beforeMonth, factoryList, Lists.newArrayList());
        if (!CollectionUtils.isEmpty(beforeMonth4AdjustList)) {
            allLhDayPlanAdjust.addAll(beforeMonth4AdjustList);
        }
        if (yearMonthList.size() == BigDecimal.ONE.intValue()) {
            return allLhDayPlanAdjust;
        }
        //获取当前月的硫化日调整
        YearMonth afterMonth = yearMonthList.get(BigDecimal.ONE.intValue());
        allLhDayPlanAdjust.addAll(lhDayPlanAdjustRequireService.getMonthPlanLhDayAdjustList(afterMonth, factoryList, Lists.newArrayList()));
        return allLhDayPlanAdjust;
    }

    /**
     * 加载日完成量
     *
     * @param dataInfo
     * @param first
     * @param factoryList
     */
    private void loadFinishQtyInfo(LhSkuSurplusDataInfo dataInfo, Date first, List<String> factoryList) {
        if (null == dataInfo || null == first || CollectionUtils.isEmpty(factoryList)) {
            return;
        }
        Date previousFirstDate = DateUtils.getPreviousDate(first);
        //planStartDate~previousFirstDate的日完成量
        Map<String, Integer> dayFinishSumInfo = loadFinishQty(dataInfo.getPlanStartDate(), previousFirstDate, factoryList);
        //firt夜班完成量
        Map<String, BigDecimal> loadNightFinishInfo = loadNightFinishInfo(first, factoryList);
        Set<String> allKey = Sets.newHashSet();
        if (!CollectionUtils.isEmpty(dayFinishSumInfo)) {
            allKey.addAll(dayFinishSumInfo.keySet());
        }
        if (!CollectionUtils.isEmpty(loadNightFinishInfo)) {
            allKey.addAll(loadNightFinishInfo.keySet());
        }
        Map<String, Integer> allDayFinishSumInfo = Maps.newHashMap();
        allKey.forEach(key -> {
            Integer finishQty = BigDecimal.ZERO.intValue();
            if (dayFinishSumInfo.containsKey(key)) {
                finishQty = finishQty + dayFinishSumInfo.get(key);
            }
            if (loadNightFinishInfo.containsKey(key)) {
                finishQty = finishQty + loadNightFinishInfo.get(key).intValue();
            }
            allDayFinishSumInfo.put(key, finishQty);
        });
        dataInfo.setDayFinishSumInfo(allDayFinishSumInfo);
        dataInfo.setNightFinishInfo(loadNightFinishInfo);
    }

    /**
     * 获取某个年月的排产月计划信息
     *
     * @param yearMonth        年-月信息
     * @param factoryList      工厂
     * @param materialCodeList 物料编码
     * @return
     */
    private List<FactoryMonthPlanProductionFinalResult> getMonthPlanList(YearMonth yearMonth, List<String> factoryList, List<String> materialCodeList) {
        if (null == yearMonth || CollectionUtils.isEmpty(factoryList)) {
            return Collections.emptyList();
        }
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = Lists.newArrayList();
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        for (String factoryCode : factoryList) {
            LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, factoryCode)
                    .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                    .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                    .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
            if (!CollectionUtils.isEmpty(materialCodeList)) {
                wrapper.in(FactoryMonthPlanProductionFinalResult::getMaterialCode, materialCodeList);
            }
            List<FactoryMonthPlanProductionFinalResult> singleMonthList = monthPlanFinalResultMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(singleMonthList)) {
                continue;
            }
            monthPlanList.addAll(singleMonthList);
        }
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return Collections.emptyList();
        }
        return monthPlanList;
    }

    /**
     * 单独获取日硫化调整计划的调整4
     *
     * @param yearMonth        年月
     * @param factoryCodeList  工厂
     * @param materialCodeList 物料
     * @return
     */
    private List<LhDayPlanAdjustVo> getYearMonthLhDayPlanAdjust4(YearMonth yearMonth, List<String> factoryCodeList, List<String> materialCodeList) {
        if (null == yearMonth || CollectionUtils.isEmpty(factoryCodeList)) {
            return Collections.emptyList();
        }
        //获取年月的硫化日调整量
        List<LhDayPlanAdjustVo> yearMonthResult = lhDayPlanAdjustRequireService.getMonthPlanLhDayAdjustList(yearMonth, factoryCodeList, materialCodeList);
        if (CollectionUtils.isEmpty(yearMonthResult)) {
            return Collections.emptyList();
        }
        List<LhDayPlanAdjustVo> findAdjust4List = yearMonthResult.stream().filter(singleAdjust -> LhScheduleConstant.ADJUST_4.equals(singleAdjust.getAdjustCount())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findAdjust4List)) {
            return Collections.emptyList();
        }
        return findAdjust4List;
    }

    /**
     * 加载日完成量
     *
     * @return
     */
    private Map<String, Integer> loadFinishQty(Date startDate, Date endDate, List<String> factoryCodeList) {
        if (null == startDate || null == endDate || CollectionUtils.isEmpty(factoryCodeList)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<LhDayFinishQty> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LhDayFinishQty::getFactoryCode, factoryCodeList)
                .eq(LhDayFinishQty::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .ge(LhDayFinishQty::getFinishDate, startDate)
                .le(LhDayFinishQty::getFinishDate, endDate);
        List<LhDayFinishQty> dayFinishList = lhDayFinishQtyMapper.selectList(wrapper);
        return SkuMonthPlanCalculator.getDayFinishQty(startDate, dayFinishList);
    }

    /**
     * 加载某日的夜班完成量
     *
     * @param date
     * @param factoryCodeList
     * @return
     */
    private Map<String, BigDecimal> loadNightFinishInfo(Date date, List<String> factoryCodeList) {
        if (null == date || CollectionUtils.isEmpty(factoryCodeList)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<LhScheFinishQty> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LhScheFinishQty::getFactoryCode, factoryCodeList)
                .eq(LhScheFinishQty::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .eq(LhScheFinishQty::getScheduleDate, date);
        List<LhScheFinishQty> classFinishList = lhScheFinishQtyMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(classFinishList)) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> nightFinishMap = Maps.newHashMap();
        for (LhScheFinishQty qty : classFinishList) {
            if (StringUtils.isNotEmpty(qty.getFactoryCode()) && StringUtils.isNotEmpty(qty.getMaterialCode()) && Objects.nonNull(qty.getClass1FinishQty())) {
                String key = qty.getFactoryMaterialStatusKey();
                nightFinishMap.merge(key, qty.getClass1FinishQty(), BigDecimal::add);
            }
        }
        return nightFinishMap;
    }
}
