package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpProductionPredictionServiceImpl.java
 * 描    述：MpProductionPredictionServiceImplS2-1002.未来产量预测业务层处理
 *@author yelq
 *@date 2025-12-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MpProductionPredictionServiceImpl extends AbstractDocService<MpProductionPrediction>  implements IMpProductionPredictionService {
    // 预测需求计划
    private static final String PREFIX  = "PRE";

    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;
    // 需求计划
    private final IDpDemandPlanService dpDemandPlanService;



    @Override
    protected String getDocTypeCode() {
        return "2025122822";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025122822");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpProductionPrediction docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpProductionPrediction.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public AjaxResult createMonthPrediction(MpProductionPrediction createCondition) {
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRangeResult = MonthCalculator.calculateMonthRanges();
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(monthRangeResult.getTMonth());
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
        // 生成T月模拟需求计划
        // T月需求要生成,订单-库存冲减-月底计划余量(T-1月)+T月（快照周期+常规)
        // T+1月需求生成：T月需求-T月已排+T+1（周期+常规）
        // 对冲规则：供应链优先级+提报日期逐笔扣除(先冲实单)
        DpDemandPlan param = new DpDemandPlan();
        param.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        param.setYear(monthRangeResult.getTMonth().getYear());
        param.setMonth(monthRangeResult.getTMonth().getMonthValue());
        param.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        param.setPrefix(PREFIX);
        List<DpDemandPlan> tMonthDemands =  dpDemandPlanService.createPredictionRequire(param,finalVersion);
        // 剩余需求量
        List<DpDemandPlan> leftDemands = calculateLeftDemand(tMonthDemands,finalVersion);
        List<DpDemandPlan> tPlus1MonthDemands = createDemandPlan(leftDemands,monthRangeResult.getTPlus1Month());
        MpFactoryProductionVersion finalVersionByTplus1Month = createProductionVersion(tPlus1MonthDemands);
        // 剩余需求量
        List<DpDemandPlan> leftDemandsByTplus1Month = calculateLeftDemand(tPlus1MonthDemands,finalVersionByTplus1Month);
        List<DpDemandPlan> tPlus2MonthDemands = createDemandPlan(leftDemandsByTplus1Month,monthRangeResult.getTPlus2Month());
        Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfo();
        List<MpProductionPrediction> list = buildProductionPrediction(finalVersion,tPlus1MonthDemands,tPlus2MonthDemands,materialInfoMap);
        if(CollectionUtils.isNotEmpty(list)) {
            this.baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    private MpFactoryProductionVersion createProductionVersion(List<DpDemandPlan> tPlus1MonthDemands) {
        if(CollectionUtils.isEmpty(tPlus1MonthDemands)) {
            return null;
        }
        MpFactoryProductionVersion productionVersion = new MpFactoryProductionVersion();
        productionVersion.setFactoryCode(tPlus1MonthDemands.get(0).getFactoryCode());
        productionVersion.setYear(tPlus1MonthDemands.get(0).getYear());
        productionVersion.setMonth(tPlus1MonthDemands.get(0).getMonth());
        productionVersion.setMonthPlanVersion(tPlus1MonthDemands.get(0).getMonthPlanVersion());
        return productionVersion;
    }

    private List<DpDemandPlan> createDemandPlan(List<DpDemandPlan> leftDemands, YearMonth yearMonth) {
        DpDemandPlan createCondition = new DpDemandPlan();
        createCondition.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        createCondition.setYear(yearMonth.getYear());
        createCondition.setMonth(yearMonth.getMonthValue());
        createCondition.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        createCondition.setPrefix(PREFIX);
        return dpDemandPlanService.createPredictionRequire(createCondition,leftDemands);
    }

    private List<DpDemandPlan> calculateLeftDemand(List<DpDemandPlan> tMonthDemands, MpFactoryProductionVersion finalVersion) {
        if(CollectionUtils.isEmpty(tMonthDemands)  || null == finalVersion) {
            return Collections.emptyList();
        }
        Map<String, List<FactoryMonthPlanProductionFinalResult>> productionFinalResults  = calculateMonthDemandQty(finalVersion);
        if(org.springframework.util.CollectionUtils.isEmpty(productionFinalResults)) {
            return tMonthDemands;
        }
        // 获取排序配置并排序订单
        List<DpDemandPlan> sortedDemandPlans = getSortedDemandPlans(tMonthDemands);
        sortedDemandPlans.forEach(plan -> calculdateDemandQty(plan,productionFinalResults));
        return sortedDemandPlans;
    }

    private void calculdateDemandQty(DpDemandPlan plan, Map<String, List<FactoryMonthPlanProductionFinalResult>> productionFinalResults) {
        List<FactoryMonthPlanProductionFinalResult> list = productionFinalResults.get(plan.getMaterialCode());
        if(CollectionUtils.isEmpty(list)) {
            return;
        }
        int heightQty =  null == plan.getHeightQty()?BigDecimal.ZERO.intValue():plan.getHeightQty();
        int leftHeightQty = calculateHeightQty(heightQty,list);

        int midQty = null == plan.getMidQty()?BigDecimal.ZERO.intValue():plan.getMidQty();
        int leftMidQty = calculateMidQty(midQty,list);

        int postponeQty = null == plan.getPostponeQty()?BigDecimal.ZERO.intValue():plan.getPostponeQty();
        int leftPostponeQty = calculatePostponeQty(postponeQty,list);

        int cycleReserveQty = null == plan.getCycleReserveQty()?BigDecimal.ZERO.intValue():plan.getCycleReserveQty();
        int leftCycleReserveQty = calculateCycleReserveQty(cycleReserveQty,list);

        int conventionReserveQty = null == plan.getConventionReserveQty()?BigDecimal.ZERO.intValue():plan.getConventionReserveQty();
        int leftConventionReserveQty = calculateConventionReserveQty(conventionReserveQty,list);

        int netQty =  null == plan.getNetQty()?BigDecimal.ZERO.intValue():plan.getNetQty();
        int leftNetQty = calculateNetQty(netQty,list);
        plan.setHeightQty(leftHeightQty);
        plan.setMidQty(leftMidQty);
        plan.setPostponeQty(leftPostponeQty);
        plan.setCycleReserveQty(leftCycleReserveQty);
        plan.setConventionReserveQty(leftConventionReserveQty);
        plan.setNetQty(leftNetQty);
        // (8)净需求(含暂缓) = 高优先级净需求量 + 中优先级净需求量+暂缓订单需求量
        plan.setPostponeQty(leftHeightQty + leftMidQty + leftPostponeQty);
        // (9)净需求(不含暂缓) = 高优先级净需求量 + 中优先级净需求量
        plan.setUnPostponeNetQty(leftHeightQty + leftMidQty);
    }

    private int calculateNetQty(int netQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (netQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(netQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getTotalQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getTotalQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setTotalQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setTotalQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateHeightQty(int heightQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (heightQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(heightQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getHeightProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getHeightProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setHeightProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setHeightProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateMidQty(int midQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (midQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(midQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getMidProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getMidProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setMidProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setMidProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculatePostponeQty(int postponeQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (postponeQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(postponeQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getPostponeProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getPostponeProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setPostponeProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setPostponeProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateCycleReserveQty(int cycleReserveQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (cycleReserveQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(cycleReserveQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getCycleProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getCycleProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setCycleProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setCycleProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private int calculateConventionReserveQty(int conventionReserveQty, List<FactoryMonthPlanProductionFinalResult> list) {
        if (conventionReserveQty  <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal remainingQty = BigDecimal.valueOf(conventionReserveQty);
        for (FactoryMonthPlanProductionFinalResult stock : list) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            // 获取当前库存数量
            BigDecimal stockQty = stock.getConventionProductionQty() == null?BigDecimal.ZERO:BigDecimal.valueOf(stock.getConventionProductionQty());
            if(stockQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (stockQty.compareTo(remainingQty) >= 0) {
                // 当前库存足够冲减
                stock.setConventionProductionQty(stockQty.subtract(remainingQty).intValue());
                remainingQty = BigDecimal.ZERO;
            } else {
                // 当前库存不足，全部冲减
                stock.setConventionProductionQty(0);
                remainingQty = remainingQty.subtract(stockQty);
            }
        }
        return remainingQty.intValue();
    }

    private List<DpDemandPlan> getSortedDemandPlans(List<DpDemandPlan> tMonthDemands) {
        return tMonthDemands.stream()
            .sorted(getHighPerformanceComparator())
            .collect(Collectors.toList());
    }

    /**
     * 高性能自定义比较器（适用于大数据量）
     */
    private static Comparator<DpDemandPlan> getHighPerformanceComparator() {
        return new DemandPlanComparator();
    }


    private List<MpProductionPrediction> buildProductionPrediction(MpFactoryProductionVersion finalVersion,List<DpDemandPlan> tPlus1MonthDemands, List<DpDemandPlan> tPlus2MonthDemands, Map<String, MdmMaterialInfo> materialInfoMap) {
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyList();
        }
        Map<String,Integer> tMonthDemandQty = this.getMonthQty(productionFinalResults);
        MpFactoryProductionVersion productionVersionByTplus1Month = this.createProductionVersion(tPlus1MonthDemands);
        MpFactoryProductionVersion productionVersionByTplus2Month = this.createProductionVersion(tPlus2MonthDemands);
        List<FactoryMonthPlanProductionFinalResult> productionFinalResultsByTplus1Month =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(productionVersionByTplus1Month);
        List<FactoryMonthPlanProductionFinalResult> productionFinalResultsTplus2Month =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(productionVersionByTplus2Month);
        Map<String,Integer> tPlus1MonthDemandQty = this.getMonthQty(productionFinalResultsByTplus1Month);
        Map<String,Integer> tPlus2MonthDemandQty = this.getMonthQty(productionFinalResultsTplus2Month);
        List<MpProductionPrediction> list = Lists.newArrayList();
        YearMonth yearMonth = YearMonth.now();
        tMonthDemandQty.forEach((materialCode, productionQty) -> {
                if(!materialInfoMap.containsKey(materialCode)) {
                    return;
                }
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                MpProductionPrediction productionPrediction = new MpProductionPrediction();
                BeanUtils.copyProperties(materialInfo,productionPrediction);
                productionPrediction.setId(null);
                productionPrediction.setBaseVale(null);
                productionPrediction.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                productionPrediction.setYear(yearMonth.getYear());
                productionPrediction.setMonth(yearMonth.getMonthValue());
                productionPrediction.setLocationType(materialInfo.getCommonType());
                productionPrediction.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
                productionPrediction.setProductionVersion(finalVersion.getProductionVersion());
                productionPrediction.setMonth1(productionQty);
                productionPrediction.setMonth2(tPlus1MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
                productionPrediction.setMonth3(tPlus2MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
                list.add(productionPrediction);
        });
        return list;
    }

    private Map<String, Integer> getMonthQty(List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {
        if (CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) && productionFinalResult.getTotalQty() != null)
            .collect(Collectors.groupingBy(
                FactoryMonthPlanProductionFinalResult::getMaterialCode,
                Collectors.summingInt(FactoryMonthPlanProductionFinalResult::getTotalQty)
            ));
    }

    private Map<String, List<FactoryMonthPlanProductionFinalResult>> calculateMonthDemandQty(MpFactoryProductionVersion finalVersion) {
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults = factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyMap();
        }
        return productionFinalResults.stream()
            .filter(Objects::nonNull)
            .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()))
            .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));
    }


    private Map<String, MdmMaterialInfo> fetchMaterialInfo() {
        return materialInfoService.skuToMaterialInfo();
    }

    /**
     *   3、检查是否已有T月月度计划(定稿)
     *       (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
     * @param tMonth T月
     */
    private List<MpFactoryProductionVersion> validateProductionVersionFinalized(YearMonth tMonth) {
        return factoryProductionVersionMapper.selectList(
            Wrappers.<MpFactoryProductionVersion>lambdaQuery()
                .eq(MpFactoryProductionVersion::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE)
                .eq(MpFactoryProductionVersion::getYear, tMonth.getYear())
                .eq(MpFactoryProductionVersion::getMonth, tMonth.getMonthValue())
                .eq(MpFactoryProductionVersion::getIsFinal,YesOrNoEnum.YES.getCode())
        );
    }

    /**
     * 自定义高性能比较器实现
     * 避免重复解析和lambda开销
     */
    private static class DemandPlanComparator implements Comparator<DpDemandPlan> {

        @Override
        public int compare(DpDemandPlan o1, DpDemandPlan o2) {
            // 1. 比较供应链优先级
            int scmPriorityCompare = compareScmPriority(o1, o2);
            if (scmPriorityCompare != 0) {
                return scmPriorityCompare;
            }

            // 2. 比较提报日期
            int dateCompare = compareYearWeek(o1, o2);
            if (dateCompare != 0) {
                return dateCompare;
            }

            // 3. 比较提报量
            return compareOrdQty(o1, o2);
        }

        private int compareScmPriority(DpDemandPlan o1, DpDemandPlan o2) {
            Integer p1 = parseScmPriority(o1.getScmPriority());
            Integer p2 = parseScmPriority(o2.getScmPriority());

            if (p1 == null && p2 == null) {
                return 0;
            }
            if (p1 == null) {
                return 1; // null排最后
            }
            if (p2 == null) {
                return -1;
            }

            return Integer.compare(p1, p2);
        }

        private int compareYearWeek(DpDemandPlan o1, DpDemandPlan o2) {
            Integer d1 = parseYearWeek(o1.getYearWeek()) ;
            Integer d2 = parseYearWeek(o2.getYearWeek()) ;

            if (d1 == null && d2 == null) {
                return 0;
            }
            if (d1 == null) {
                return 1; // null排最后
            }
            if (d2 == null) {
                return -1;
            }
            return Integer.compare(d1, d2);
        }

        private int compareOrdQty(DpDemandPlan o1, DpDemandPlan o2) {
            Integer q1 = o1.getPostponeQty();
            Integer q2 = o2.getPostponeQty();

            if (q1 == null && q2 == null) {
                return 0;
            }
            // null排最后
            if (q1 == null) {
                return 1;
            }
            if (q2 == null) {
                return -1;
            }
            return q1.compareTo(q2);
        }

        private Integer parseScmPriority(String scmPriority) {
            if (scmPriority == null || scmPriority.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(scmPriority.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Integer parseYearWeek(String yearWeek) {
            if (StringUtils.isEmpty(yearWeek)) {
                return null;
            }
            try {
                return Integer.parseInt(yearWeek.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
