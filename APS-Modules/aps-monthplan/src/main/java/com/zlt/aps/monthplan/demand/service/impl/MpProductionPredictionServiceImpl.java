package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.maindata.mapper.MpProductionPredictionEntityMapper;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.common.utils.MonthCalculator;
import com.zlt.aps.monthplan.common.utils.PredictionContext;
import com.zlt.aps.monthplan.common.utils.ProductionSchedulingService;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IMpPredictionDetailService;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
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
    private final MpProductionPredictionEntityMapper mpProductionPredictionEntityMapper;
    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;
    // 定稿的月度排产计划
    private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
    // 物料信息
    private final IMdmMaterialInfoService materialInfoService;
    // 需求计划
    private final IDpDemandPlanService dpDemandPlanService;
    // 预测明细
    private final IMpPredictionDetailService mpPredictionDetailService;

    private final ProductionSchedulingService productionSchedulingService;


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
    public AjaxResult createMonthPrediction(MpProductionPrediction createCondition) throws InterruptedException {
        // 获取操作日所在月份
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        // 2、得到T月、T+1月、T+2月。T月 = 当前操作日所在年月(当月) +1 ；T+1月 = 在T月的基础上+1个月；T+2月 = 在T月的基础上+2个月
        MonthCalculator.MonthRangeResult monthRangeResult = MonthCalculator.calculateMonthRanges(currentMonth);
        // 3、检查是否已有T月月度计划(定稿)
        //   (1) 若 不存在T月月度计划，则提示"T月月度生产计划还未定稿，请先生成及定稿！"，系统不做任何处理。
        List<MpFactoryProductionVersion> finalVersions =  validateProductionVersionFinalized(monthRangeResult.getTMonth());
        if (CollectionUtils.isEmpty(finalVersions)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.productionPrediction.checkFinal"));
        }
        MpFactoryProductionVersion finalVersion =  finalVersions.get(0);
        Map<YearMonth,MpFactoryProductionVersion> productionVersions = Maps.newHashMap();
        productionVersions.put(monthRangeResult.getTMonth(),finalVersion);
        PredictionContext predictionContext = dpDemandPlanService.buildPredictionContext();
        // 生成T月模拟需求计划
        // T月需求要生成,订单-库存冲减-月底计划余量(T-1月)+T月（快照周期+常规)
        // T+1月需求生成：T月需求-T月已排+T+1（周期+常规）
        // 对冲规则：供应链优先级+提报日期逐笔扣除(先冲实单)
        DpDemandPlan param = new DpDemandPlan();
        param.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        param.setPlanType(ProductionPlanType.PREDICTION.getPlanType());
        param.setPrefix(PREFIX);
        List<DpDemandPlan> tMonthDemands =  dpDemandPlanService.createInitPredictionRequire(param,finalVersion,predictionContext);
        // 	12、以第11步的T+1月的需求量，按月度排产逻辑进行排产(此时暂缓订单需要排产)，得到T+1月的月排产计划
        List<DpDemandPlan> tPlus1MonthDemands =  dpDemandPlanService.createPredictionRequire(param,finalVersion,predictionContext);
        List<DpDemandPlan> tPlus2MonthDemands = Lists.newArrayList();
        // 排产汇总
        if(!CollectionUtils.isEmpty(tPlus1MonthDemands)) {
            Context context = buildContext(tPlus1MonthDemands);
            productionSchedulingService.executeSchedulingInNewTransaction(param,context);
            MpFactoryProductionVersion finalVersionByTplus1Month = createProductionVersion(context,tPlus1MonthDemands);
            productionVersions.put(monthRangeResult.getTPlus1Month(),finalVersionByTplus1Month);
            tPlus2MonthDemands = dpDemandPlanService.createPredictionRequire(param,finalVersionByTplus1Month,predictionContext);
        }
        if(!CollectionUtils.isEmpty(tPlus2MonthDemands)) {
            Context context = buildContext(tPlus2MonthDemands);
            productionSchedulingService.executeSchedulingInNewTransaction(param,context);
            MpFactoryProductionVersion finalVersionByTplus2Month = createProductionVersion(context,tPlus2MonthDemands);
            productionVersions.put(monthRangeResult.getTPlus2Month(),finalVersionByTplus2Month);
        }
        Map<String, MdmMaterialInfo> materialInfoMap = fetchMaterialInfo();
        List<MpProductionPrediction> list = buildProductionPrediction(monthRangeResult,tMonthDemands,productionVersions,materialInfoMap);
        if(!CollectionUtils.isEmpty(list)) {
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


    @Override
    public List<String> findPredictionVersion(MpProductionPrediction queryCondition) {
        LambdaQueryWrapper<MpProductionPrediction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpProductionPrediction::getFactoryCode, queryCondition.getFactoryCode());
        wrapper.eq(MpProductionPrediction::getYear, queryCondition.getYear());
        wrapper.eq(MpProductionPrediction::getMonth, queryCondition.getMonth());
        wrapper.eq(MpProductionPrediction::getIsDelete, YesOrNoEnum.NO.getValue());
        wrapper.isNotNull(MpProductionPrediction::getPredictionVersion);
        wrapper.orderByDesc(MpProductionPrediction::getPredictionVersion);
        List<MpProductionPrediction> list =  this.mpProductionPredictionEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyList();
        }
        return list.stream().map(MpProductionPrediction::getPredictionVersion).distinct().collect(Collectors.toList());
    }

    private MpFactoryProductionVersion createProductionVersion(Context context,List<DpDemandPlan> tPlus1MonthDemands) {
        if(CollectionUtils.isEmpty(tPlus1MonthDemands)) {
            return null;
        }
        MpFactoryProductionVersion productionVersion = new MpFactoryProductionVersion();
        productionVersion.setFactoryCode(tPlus1MonthDemands.get(0).getFactoryCode());
        productionVersion.setYear(tPlus1MonthDemands.get(0).getYear());
        productionVersion.setMonth(tPlus1MonthDemands.get(0).getMonth());
        productionVersion.setMonthPlanVersion(tPlus1MonthDemands.get(0).getMonthPlanVersion());
        productionVersion.setProductionVersion(context.getProductionVersion());
        return productionVersion;
    }


    private List<MpProductionPrediction> buildProductionPrediction(MonthCalculator.MonthRangeResult monthRangeResult,List<DpDemandPlan> tMonthDemands,Map<YearMonth,MpFactoryProductionVersion> productionVersions, Map<String, MdmMaterialInfo> materialInfoMap) {
        MpFactoryProductionVersion currentFinalVersion = productionVersions.get(monthRangeResult.getTMonth());
        Set<String> monthPlanVersions = productionVersions.values().stream().map(MpFactoryProductionVersion::getMonthPlanVersion).filter(monthPlanVersion -> !currentFinalVersion.getMonthPlanVersion().equals(monthPlanVersion)).collect(Collectors.toSet());
        List<FactoryMonthPlanMouldDayResult> list = this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(currentFinalVersion,monthPlanVersions);
        if(CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<String,List<FactoryMonthPlanMouldDayResult>>  map =   list.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getMaterialCode));

        List<MpProductionPrediction> result = Lists.newArrayList();
        YearMonth yearMonth = YearMonth.now();
        DpDemandPlan demandPlan = tMonthDemands.get(0);

        map.forEach((materialCode, value) -> {
                if(!materialInfoMap.containsKey(materialCode)) {
                    return;
                }
                List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode = map.get(materialCode);
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                MpProductionPrediction productionPrediction = new MpProductionPrediction();
                BeanUtils.copyProperties(materialInfo,productionPrediction);
                productionPrediction.setId(null);
                productionPrediction.setBaseVale(null);
                productionPrediction.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                productionPrediction.setYear(yearMonth.getYear());
                productionPrediction.setMonth(yearMonth.getMonthValue());
                productionPrediction.setLocationType(materialInfo.getCommonType());
                productionPrediction.setPredictionVersion(demandPlan.getMonthPlanVersion());
                productionPrediction.setMonthPlanVersion(currentFinalVersion.getMonthPlanVersion());
                productionPrediction.setProductionVersion(currentFinalVersion.getProductionVersion());
                productionPrediction.setMouldQty(calculateMouldQty(listGroupByMaterialCode));
                productionPrediction.setTypeBlockQty(calculateTypeBlockQty(listGroupByMaterialCode));
                productionPrediction.setNetQty(calculateNetQty(listGroupByMaterialCode));
                productionPrediction.setHeightQty(calculateHeightQty(listGroupByMaterialCode));
                productionPrediction.setProductionQty(calculateProductionQty(listGroupByMaterialCode));
                productionPrediction.setMonth1(calculateProductionQty(listGroupByMaterialCode,monthRangeResult.getTMonth()));
                productionPrediction.setMonth2(calculateProductionQty(listGroupByMaterialCode,monthRangeResult.getTPlus1Month()));
                productionPrediction.setMonth3(calculateProductionQty(listGroupByMaterialCode,monthRangeResult.getTPlus2Month()));
                result.add(productionPrediction);
        });
        this.mpPredictionDetailService.batchInsert(demandPlan,currentFinalVersion,productionVersions);
        return result;
    }

    private int calculateProductionQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
        if(org.apache.commons.collections.CollectionUtils.isEmpty(listGroupByMaterialCode)){
            return 0;
        }
        return listGroupByMaterialCode.stream().filter(item -> null != item.getTotalQty()).mapToInt(FactoryMonthPlanMouldDayResult::getTotalQty).sum();
    }

    private int calculateHeightQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
        if(org.apache.commons.collections.CollectionUtils.isEmpty(listGroupByMaterialCode)){
            return 0;
        }
        return listGroupByMaterialCode.stream().filter(item -> null != item.getHeightQty()).mapToInt(FactoryMonthPlanMouldDayResult::getHeightQty).sum();
    }

    private int calculateNetQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
        if(org.apache.commons.collections.CollectionUtils.isEmpty(listGroupByMaterialCode)){
            return 0;
        }
        return listGroupByMaterialCode.stream().filter(item -> null != item.getProdReqPlan()).mapToInt(FactoryMonthPlanMouldDayResult::getProdReqPlan).sum();
    }

    private int calculateTypeBlockQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
        if(org.apache.commons.collections.CollectionUtils.isEmpty(listGroupByMaterialCode)){
            return 0;
        }
        FactoryMonthPlanMouldDayResult mouldDayResult = listGroupByMaterialCode.stream().filter(item -> null != item.getMouldCavityQty()).findFirst().orElse(null);
        return null == mouldDayResult?0:mouldDayResult.getTypeBlockQty();
    }

    private int calculateMouldQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
        if(org.apache.commons.collections.CollectionUtils.isEmpty(listGroupByMaterialCode)){
            return 0;
        }
        FactoryMonthPlanMouldDayResult mouldDayResult = listGroupByMaterialCode.stream().filter(item -> null != item.getMouldCavityQty()).findFirst().orElse(null);
        return null == mouldDayResult?0:mouldDayResult.getMouldCavityQty();
    }

    private int calculateProductionQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode, YearMonth yearMonth) {
        if(org.apache.commons.collections.CollectionUtils.isEmpty(listGroupByMaterialCode)){
            return 0;
        }
        return listGroupByMaterialCode.stream().filter(item -> yearMonth.getYear() == item.getYear() && yearMonth.getMonthValue() == item.getMonth() && null != item.getTotalQty()).mapToInt(FactoryMonthPlanMouldDayResult::getTotalQty).sum();
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
