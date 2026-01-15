package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;

import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.PredictionContext;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IMpSimulatedResultService;

import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSimulatedResultServiceImpl.java
 * 描    述：MpSimulatedResultServiceImplS2-1004.实单模拟排产业务层处理
 *@author yelq
 *@date 2025-12-31
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
public class MpSimulatedResultServiceImpl extends AbstractDocService<MpSimulatedResult>  implements IMpSimulatedResultService {
    private static final String PREFIX = "VM";
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;
    // 需求计划
    private final IDpDemandPlanService dpDemandPlanService;
    // 排产
    private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

    @Override
    protected String getDocTypeCode() {
        return "2025123114";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2025123114");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpSimulatedResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpSimulatedResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public AjaxResult createVmMonthPrediction(MpSimulatedResult createCondition) {
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRange = MonthCalculator.calculateMonthRanges();
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(monthRange.getTMonth());
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
        PredictionContext predictionContext = dpDemandPlanService.buildPredictionContext();
        Map<YearMonth,MpFactoryProductionVersion> productionVersions = Maps.newHashMap();
        productionVersions.put(monthRange.getTMonth(),finalVersion);
        // 生成T月模拟需求计划
        // T月需求要生成,订单-库存冲减-月底计划余量(T-1月)+T月（快照周期+常规)
        // T+1月需求生成：T月需求-T月已排+T+1（周期+常规）
        // 对冲规则：供应链优先级+提报日期逐笔扣除(先冲实单)
        DpDemandPlan param = new DpDemandPlan();
        param.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        param.setPlanType(ProductionPlanType.SIMULATE.getPlanType());
        param.setPrefix(PREFIX);
        List<DpDemandPlan> tMonthDemands =  dpDemandPlanService.createInitPredictionRequire(param,finalVersion,predictionContext);
        // 定义要处理的所有月份
        YearMonth[] monthsToProcess = {
            monthRange.getTPlus1Month(),
            monthRange.getTPlus2Month(),
            monthRange.getTPlus3Month(),
            monthRange.getTPlus4Month(),
            monthRange.getTPlus5Month(),
            monthRange.getTPlus6Month(),
            monthRange.getTPlus7Month(),
            monthRange.getTPlus8Month(),
            monthRange.getTPlus9Month(),
            monthRange.getTPlus10Month(),
            monthRange.getTPlus11Month(),
            monthRange.getTPlus12Month(),
            monthRange.getTPlus13Month(),
            monthRange.getTPlus14Month(),
            monthRange.getTPlus15Month(),
            monthRange.getTPlus16Month(),
            monthRange.getTPlus17Month(),
            monthRange.getTPlus18Month(),
            monthRange.getTPlus19Month(),
            monthRange.getTPlus20Month(),
            monthRange.getTPlus21Month(),
            monthRange.getTPlus22Month(),
            monthRange.getTPlus23Month()
        };
          MpFactoryProductionVersion currentFinalVersion = finalVersion;
          List<DpDemandPlan> currentMonthDemands;
          // 对应的历史数据偏移量（从5开始递减）
          for (YearMonth currentMonth : monthsToProcess) {
              // 	12、以第11步的T+1月的需求量，按月度排产逻辑进行排产(此时暂缓订单需要排产)，得到T+1月的月排产计划
              currentMonthDemands =  dpDemandPlanService.createPredictionRequire(param,currentFinalVersion,predictionContext);
              // 排产汇总
              if(!org.springframework.util.CollectionUtils.isEmpty(currentMonthDemands)) {
                /*  Context context = buildContext(currentMonthDemands);
                  monthPlanProductionSchedulingService.general(context);*/
                  currentFinalVersion = createProductionVersion(currentMonthDemands);
                  productionVersions.put(currentMonth,currentFinalVersion);
              }
          }
        Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfo();
        List<MpSimulatedResult> list = buildSimulatedResult(monthRange,productionVersions,tMonthDemands,materialInfoMap);
        if(CollectionUtils.isNotEmpty(list)) {
            this.baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    private Context buildContext(List<DpDemandPlan> tPlus1MonthDemands) {
        Context context = new Context();
        context.setFactoryCode(tPlus1MonthDemands.get(0).getFactoryCode());
        context.setYear(tPlus1MonthDemands.get(0).getYear());
        context.setMonth(tPlus1MonthDemands.get(0).getMonth());
        context.setMonthPlanVersion(tPlus1MonthDemands.get(0).getMonthPlanVersion());
        context.setPrefixVersion(PREFIX);
        context.setProductType(ProductTypeEnum.getEnumByValue(tPlus1MonthDemands.get(0).getProductTypeCode()));
        return context;
    }

    private List<MpSimulatedResult> buildSimulatedResult(MonthCalculator.MonthRangeResult monthRange,Map<YearMonth, MpFactoryProductionVersion> productionVersions,List<DpDemandPlan> tMonthDemands, Map<String, MdmMaterialInfo> materialInfoMap) {
        MpFactoryProductionVersion  finalVersion = productionVersions.get(monthRange.getTMonth());
        List<FactoryMonthPlanProductionFinalResult> productionFinalResults =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(finalVersion);
        if(CollectionUtils.isEmpty(productionFinalResults)) {
            return Collections.emptyList();
        }
        List<MpSimulatedResult> list = Lists.newArrayList();
        Map<String,Integer> tMonthDemandQty = this.getMonthQty(productionFinalResults);
        Map<String,Integer> tPlus1MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus1Month());
        Map<String,Integer> tPlus2MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus2Month());
        Map<String,Integer> tPlus3MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus3Month());
        Map<String,Integer> tPlus4MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus4Month());
        Map<String,Integer> tPlus5MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus5Month());
        Map<String,Integer> tPlus6MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus6Month());
        Map<String,Integer> tPlus7MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus7Month());
        Map<String,Integer> tPlus8MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus8Month());
        Map<String,Integer> tPlus9MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus9Month());
        Map<String,Integer> tPlus10MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus10Month());
        Map<String,Integer> tPlus11MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus11Month());
        Map<String,Integer> tPlus12MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus12Month());
        Map<String,Integer> tPlus13MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus13Month());
        Map<String,Integer> tPlus14MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus14Month());
        Map<String,Integer> tPlus15MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus15Month());
        Map<String,Integer> tPlus16MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus16Month());
        Map<String,Integer> tPlus17MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus17Month());
        Map<String,Integer> tPlus18MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus18Month());
        Map<String,Integer> tPlus19MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus19Month());
        Map<String,Integer> tPlus20MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus20Month());
        Map<String,Integer> tPlus21MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus21Month());
        Map<String,Integer> tPlus22MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus22Month());
        Map<String,Integer> tPlus23MonthDemandQty = getProductionQty(productionVersions,monthRange.getTPlus23Month());
        YearMonth yearMonth = YearMonth.now();
        DpDemandPlan tMonthDemandPlan = tMonthDemands.get(0);
        tMonthDemandQty.forEach((materialCode, productionQty) -> {
            if(!materialInfoMap.containsKey(materialCode)) {
                return;
            }
            MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
            MpSimulatedResult productionPrediction = new MpSimulatedResult();
            BeanUtils.copyProperties(materialInfo,productionPrediction);
            productionPrediction.setId(null);
            productionPrediction.setBaseVale(null);
            productionPrediction.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            productionPrediction.setYear(yearMonth.getYear());
            productionPrediction.setMonth(yearMonth.getMonthValue());
            productionPrediction.setMonthPlanVersion(tMonthDemandPlan.getMonthPlanVersion());
            productionPrediction.setProductionVersion(finalVersion.getProductionVersion());
            productionPrediction.setMonth1(productionQty);
            productionPrediction.setMonth2(tPlus1MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth3(tPlus2MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth4(tPlus3MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth5(tPlus4MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth6(tPlus5MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth7(tPlus6MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth8(tPlus7MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth9(tPlus8MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth10(tPlus9MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth11(tPlus10MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth12(tPlus11MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth13(tPlus12MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth14(tPlus13MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth15(tPlus14MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth16(tPlus15MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth17(tPlus16MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth18(tPlus17MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth19(tPlus18MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth20(tPlus19MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth21(tPlus20MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));

            productionPrediction.setMonth22(tPlus21MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth23(tPlus22MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            productionPrediction.setMonth24(tPlus23MonthDemandQty.getOrDefault(materialCode,BigDecimal.ZERO.intValue()));
            list.add(productionPrediction);
        });
        return list;
    }

    private Map<String,Integer> getProductionQty(Map<YearMonth, MpFactoryProductionVersion> productionVersions, YearMonth yearMonth) {
         if(!productionVersions.containsKey(yearMonth)) {
             return Collections.emptyMap();
         }
         MpFactoryProductionVersion productionVersion = productionVersions.get(yearMonth);
         List<FactoryMonthPlanProductionFinalResult> productionFinalResults =   this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(productionVersion);
         if(CollectionUtils.isEmpty(productionFinalResults)) {
             return Collections.emptyMap();
         }
         return this.getMonthQty(productionFinalResults);
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

}
