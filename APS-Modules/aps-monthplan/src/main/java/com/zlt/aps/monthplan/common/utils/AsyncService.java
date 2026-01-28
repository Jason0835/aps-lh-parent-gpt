package com.zlt.aps.monthplan.common.utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionPlanType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
import com.zlt.aps.monthplan.demand.service.IMpPredictionDetailService;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 异步任务执行
 * @author Yelq
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AsyncService {
  private static final String PREFIX = "VM";
  // 需求计划
  private final IDpDemandPlanService dpDemandPlanService;
  private final IMpPredictionDetailService mpPredictionDetailService;
  private final IDpOrderOffsetDetailService dpOrderOffsetDetailService;
  private final ProductionSchedulingService productionSchedulingService;
  // 定稿的月度排产计划
  private final IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;
  private final BaseDao baseDao;

  @Async("taskExecutor")
  public void executeAsyncTaskForSimulatedProduction(MpFactoryProductionVersion finalVersion,MonthCalculator.MonthRangeResult monthRange) throws Exception {
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
      currentMonthDemands =  dpDemandPlanService.createPredictionRequire(currentMonth,param,currentFinalVersion,predictionContext);
      // 排产汇总
      if(!org.springframework.util.CollectionUtils.isEmpty(currentMonthDemands)) {
        try{
          Context context = buildContext(currentMonthDemands);
          productionSchedulingService.executeSchedulingInNewTransaction(param,context);
          currentFinalVersion = createProductionVersion(context,currentMonthDemands);
          productionVersions.put(currentMonth,currentFinalVersion);
        }catch (Exception e){
            DpDemandPlan demandPlan = currentMonthDemands.get(0);
            log.info("=====工厂{}, 计划年月：{}-{}, 需求计划版本：{},异常原因:{}====",demandPlan.getFactoryCode(),demandPlan.getYear(),demandPlan.getMonth(),demandPlan.getMonthPlanVersion(),e.getMessage());
            break;
        }
      }
    }
    Map<String, MdmMaterialInfo> materialInfoMap = predictionContext.getMaterialInfoMap();
    List<MpSimulatedResult> list = buildSimulatedResult(monthRange,productionVersions,tMonthDemands,materialInfoMap);
    if(CollectionUtils.isNotEmpty(list)) {
      this.baseDao.insertBatch(list);
    }
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
    MpFactoryProductionVersion currentFinalVersion = productionVersions.get(monthRange.getTMonth());
    Set<String> monthPlanVersions = productionVersions.values().stream().map(MpFactoryProductionVersion::getMonthPlanVersion).filter(monthPlanVersion -> !currentFinalVersion.getMonthPlanVersion().equals(monthPlanVersion)).collect(Collectors.toSet());
    List<FactoryMonthPlanMouldDayResult> list = this.factoryMonthPlanProductionFinalResultService.findProductionFinalResult(currentFinalVersion,monthPlanVersions);
    if(CollectionUtils.isEmpty(list)) {
      return Collections.emptyList();
    }
    DpDemandPlan tMonthDemandPlan = tMonthDemands.get(0);
    monthPlanVersions.add(tMonthDemandPlan.getMonthPlanVersion());
    List<DpOrderOffsetDetail> predictOffsetDetails = dpOrderOffsetDetailService.findPredictOffsetDetail(tMonthDemandPlan.getMonthPlanVersion());
    Map<String,List<DpOrderOffsetDetail>> netDemandsGroupByMaterialCode = predictOffsetDetails.stream().collect(Collectors.groupingBy(DpOrderOffsetDetail::getMaterialCode));
    Map<String,List<FactoryMonthPlanMouldDayResult>>  map =   list.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getMaterialCode));
    List<MpSimulatedResult> result = Lists.newArrayList();
    YearMonth yearMonth = YearMonth.now();
    map.forEach((materialCode, value) -> {
      if(!materialInfoMap.containsKey(materialCode)) {
        return;
      }
      List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode = map.get(materialCode);
      List<DpOrderOffsetDetail> netDemands = netDemandsGroupByMaterialCode.get(materialCode);
      MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
      MpSimulatedResult productionPrediction = new MpSimulatedResult();
      BeanUtils.copyProperties(materialInfo,productionPrediction);
      productionPrediction.setId(null);
      productionPrediction.setBaseVale(null);
      productionPrediction.setFactoryCode(currentFinalVersion.getFactoryCode());
      productionPrediction.setYear(yearMonth.getYear());
      productionPrediction.setMonth(yearMonth.getMonthValue());
      productionPrediction.setMonthPlanVersion(tMonthDemandPlan.getMonthPlanVersion());
      productionPrediction.setEmbryoCode(listGroupByMaterialCode.get(0).getEmbryoCode());
      productionPrediction.setMainMaterialDesc(listGroupByMaterialCode.get(0).getMainMaterialDesc());
      productionPrediction.setProductionVersion(currentFinalVersion.getProductionVersion());
      productionPrediction.setMouldQty(calculateMouldQty(listGroupByMaterialCode));
      productionPrediction.setTypeBlockQty(calculateTypeBlockQty(listGroupByMaterialCode));
      productionPrediction.setNetQty(calculateNetQty(netDemands));
      productionPrediction.setHeightQty(calculateHeightQty(netDemands));
      productionPrediction.setProductionQty(calculateProductionQty(listGroupByMaterialCode));
      productionPrediction.setMonth1(calculateProductionQty(listGroupByMaterialCode,monthRange.getTMonth()));
      productionPrediction.setMonth2(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus1Month()));
      productionPrediction.setMonth3(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus2Month()));

      productionPrediction.setMonth4(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus3Month()));
      productionPrediction.setMonth5(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus4Month()));

      productionPrediction.setMonth6(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus5Month()));
      productionPrediction.setMonth7(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus6Month()));

      productionPrediction.setMonth8(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus7Month()));
      productionPrediction.setMonth9(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus8Month()));

      productionPrediction.setMonth10(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus9Month()));
      productionPrediction.setMonth11(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus10Month()));

      productionPrediction.setMonth12(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus11Month()));
      productionPrediction.setMonth13(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus12Month()));

      productionPrediction.setMonth14(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus13Month()));
      productionPrediction.setMonth15(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus14Month()));

      productionPrediction.setMonth16(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus15Month()));
      productionPrediction.setMonth17(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus16Month()));

      productionPrediction.setMonth18(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus17Month()));
      productionPrediction.setMonth19(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus18Month()));

      productionPrediction.setMonth20(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus19Month()));
      productionPrediction.setMonth21(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus20Month()));

      productionPrediction.setMonth22(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus21Month()));
      productionPrediction.setMonth23(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus22Month()));
      productionPrediction.setMonth24(calculateProductionQty(listGroupByMaterialCode,monthRange.getTPlus23Month()));
      result.add(productionPrediction);
    });
    this.mpPredictionDetailService.batchInsert(tMonthDemandPlan,productionVersions);
    return result;
  }

  private int calculateProductionQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
    if(CollectionUtils.isEmpty(listGroupByMaterialCode)){
      return 0;
    }
    return listGroupByMaterialCode.stream().filter(item -> null != item.getTotalQty()).mapToInt(FactoryMonthPlanMouldDayResult::getTotalQty).sum();
  }

  private int calculateHeightQty(List<DpOrderOffsetDetail> netDemands) {
    if(CollectionUtils.isEmpty(netDemands)){
      return 0;
    }
    return netDemands.stream().filter(item -> ApsConstant.SAL_PRIORITY_HIGHT.equals(item.getScmPriority()) && null != item.getProduceQtyDue()).mapToInt(DpOrderOffsetDetail::getProduceQtyDue).sum();
  }

  private int calculateNetQty(List<DpOrderOffsetDetail> netDemands) {
    if(CollectionUtils.isEmpty(netDemands)){
      return 0;
    }
    return netDemands.stream().filter(item -> null != item.getProduceQtyDue()).mapToInt(DpOrderOffsetDetail::getProduceQtyDue).sum();
  }

  private int calculateTypeBlockQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
    if(CollectionUtils.isEmpty(listGroupByMaterialCode)){
      return 0;
    }
    FactoryMonthPlanMouldDayResult mouldDayResult = listGroupByMaterialCode.stream().filter(item -> null != item.getMouldCavityQty()).findFirst().orElse(null);
    return null == mouldDayResult?0:mouldDayResult.getTypeBlockQty();
  }

  private int calculateMouldQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode) {
    if(CollectionUtils.isEmpty(listGroupByMaterialCode)){
      return 0;
    }
    FactoryMonthPlanMouldDayResult mouldDayResult = listGroupByMaterialCode.stream().filter(item -> null != item.getMouldCavityQty()).findFirst().orElse(null);
    return null == mouldDayResult?0:mouldDayResult.getMouldCavityQty();
  }

  private int calculateProductionQty(List<FactoryMonthPlanMouldDayResult> listGroupByMaterialCode, YearMonth yearMonth) {
    if(CollectionUtils.isEmpty(listGroupByMaterialCode)){
      return 0;
    }
    return listGroupByMaterialCode.stream().filter(item -> yearMonth.getYear() == item.getYear() && yearMonth.getMonthValue() == item.getMonth() && null != item.getTotalQty()).mapToInt(FactoryMonthPlanMouldDayResult::getTotalQty).sum();
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
}
