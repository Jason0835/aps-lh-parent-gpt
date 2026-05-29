package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.deduct.DailyScheduleVo;
import com.zlt.aps.mp.api.domain.deduct.DeductMouldVo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.BeforeSkuProductionInfo;
import com.zlt.aps.mp.engine.daylimit.MouldProductionLimitTypeEnum;
import com.zlt.aps.mp.engine.deduct.DeductMouldScheduler;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.mp.engine.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.ContinueSkuNextSkuInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.handler.*;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在机结构，续作模具排产业务处理
 * 1、续作Sku使用续作模具数进行排产
 * 2、与续作Sku为同规格，同花纹的其他Sku使用续作模具数进行排产
 * 3、与续作Sku为共生胎、同模具的其他Sku使用续作模具进行排产
 *
 * @author ZLT
 * @date 20251229
 */
@Slf4j
public class CxContinueProductionHandler {

    /**
     * 续作Sku使用续作模具排产
     * 可能需要进行降膜排产
     *
     * @param context            排产上下文
     * @param productionStage    排产阶段
     * @param groupPlanInfo      分组计划信息对象
     * @param continueSkuInfoMap 续作Sku信息
     */
    public static void productionContinueSku(TbrProductionContext context, ProductionStageEnum productionStage, ProductionPlanGroupInfo groupPlanInfo, Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap) {
        Set<Integer> stopDays = context.getStopDays();
        Integer continueSkuDeadLineDays = groupPlanInfo.getContinueSkuDeadLineDay(context);
        if (null == continueSkuDeadLineDays) {
            return;
        }
        ProductionCapacityParamConfiguration paramConfiguration = context.getBaseDataContainer().getParamConfiguration();
        //续作Sku轮询排产
        String groupName = groupPlanInfo.getGroupName();
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            TbrMouldProductionLogRecorder.addContinueSkuStartMouldLog(context, groupName, materialDesc);
            if (!cxContinueSkuInfo.hasProduction()) {
                TbrMouldProductionLogRecorder.addContinueSkuNoProductionQtyLog(context, groupName, materialDesc);
                return;
            }
            Integer maxDayQty = cxContinueSkuInfo.getMaxDaySingleLhMachineQty();
            //挑选的模具 本次使用最多模具数，不一定与续作模具数相等，但不会超
            Integer theoryMaxMouldNumber = cxContinueSkuInfo.getMouldNumber();
            List<ProductionMouldInfoVo> selectMouldList = SkuMouldSelector.getContinueSkuMouldNumberInit(context, productionStage, materialDesc, theoryMaxMouldNumber);
            if (CollectionUtils.isEmpty(selectMouldList)) {
                return;
            }
            cxContinueSkuInfo.setMouldNumber(selectMouldList.size());
            //1、降膜排产
            DeductMouldVo deductMould = DeductMouldScheduler.createDeductMouldBySku(continueSkuDeadLineDays, stopDays, new HashSet<>(), paramConfiguration, cxContinueSkuInfo);
            //20260421+ 降膜排产信息调整
            setDeductInfo(context, groupPlanInfo, deductMould, cxContinueSkuInfo);
            Integer needProductionQty = deductMould.getRemainingQty();
            TbrMouldProductionLogRecorder.addSkuNeedProductionInfoLog(context, groupName, materialDesc, ContinueTypeEnum.SAME_SKU.getDesc(), needProductionQty);
            if (needProductionQty <= BigDecimal.ZERO.intValue()) {
                return;
            }
            List<DailyScheduleVo> resultList = DeductMouldScheduler.scheduleProduction(deductMould);
            //分配结果
            if (CollectionUtils.isEmpty(resultList)) {
                //记录日志
                TbrMouldProductionLogRecorder.addContinueSkuNoProductionResultLog(context, groupName, materialDesc);
                return;
            }
            String mouldInfo = selectMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.joining(StringConstant.COMMA));
            TbrMouldProductionLogRecorder.addContinueSkuMouldProductionByMouldLog(context, groupName, materialDesc, mouldInfo);
            //2、将排产结果，逐日分配到模具上，按排产日由小到大排序
            resultList.sort(Comparator.comparing(DailyScheduleVo::getScheduleDate));
            resultList.forEach(dailySchedule -> {
                //使用的硫化机台数-即模具数
                Integer lhMachineCount = dailySchedule.getSkuMachines();
                Integer sumProductionQty = dailySchedule.getSkuQuantity();
                Integer productionDay = dailySchedule.getScheduleDate();
                //按双模放置
                for (int lhGroupNo = BigDecimal.ONE.intValue(); lhGroupNo <= lhMachineCount; lhGroupNo++) {
                    //20260326 检测是否超周期储备量
                    if (!CycleGroupCalculateHandler.checkCycleGroupHasProductionQty(context, materialDesc, groupPlanInfo)) {
                        TbrMouldProductionLogRecorder.addExceedCycleQtyLog(context, groupName, materialDesc, ContinueTypeEnum.SAME_SKU);
                        break;
                    }
                    Integer productionQty = Math.min(sumProductionQty, maxDayQty);
                    Integer startIndex = (lhGroupNo - BigDecimal.ONE.intValue()) * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    Integer endIndex = lhGroupNo * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    List<ProductionMouldInfoVo> doubleMouldList = selectMouldList.subList(startIndex, endIndex);
                    CxLhMouldProductionCalculator.continueSkuLhProductionHandler(context, groupPlanInfo, cxContinueSkuInfo, productionDay, productionQty, doubleMouldList);
                    sumProductionQty = sumProductionQty - productionQty;
                }
            });
        });
    }

    /**
     * 排产续作排产
     * 1、同规格同花纹
     * 2、共生胎、同模具
     *
     * @param context            排产上下文
     * @param productionStage    排产阶段
     * @param productionPlanInfo 分组排产计划
     * @param continueType       续作类型 同规格同花纹 共生胎同模具
     * @param endDay             结束日
     * @param continueSkuMap     分组计划中续作Sku信息集合
     * @param excludeDaySet      需要剔除的天
     * @param excludeSkuSet      需要剔除的Sku
     */
    public static void productionContinueByType(Context context, ProductionStageEnum productionStage, ProductionPlanGroupInfo productionPlanInfo, ContinueTypeEnum continueType, Integer endDay, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Set<Integer> excludeDaySet, Set<String> excludeSkuSet) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = productionPlanInfo.getGroupName();
        Set<String> cxMachineCodeInfo = continueSkuMap.values().stream().collect(Collectors.toList()).get(BigDecimal.ZERO.intValue()).getOnLineCxMachineSet();
        String onLineMachineInfo = String.join(StringConstant.COMMA, cxMachineCodeInfo);
        //取得最早收尾的续作硫化组
        EarliestConclusionLhGroupHelper earliestConclusionLhGroup = productionPlanInfo.getEarliestConclusionLhInfo(context, null, excludeDaySet);
        if (null == earliestConclusionLhGroup) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueSkuNoLhGroupLog(context, productionStage, groupName, onLineMachineInfo, continueType));
            return;
        }
        Integer startDay = earliestConclusionLhGroup.getClosingDay();
        //20260109 使用判断的结束日
        Integer realEndDay = earliestConclusionLhGroup.getEndDay();
        if (startDay > realEndDay) {
            //todo 记录日志
            return;
        }
        TbrMouldProductionLogRecorder.addContinueSkuEarliestConclusionLhGroupLog(context, productionStage, groupName, onLineMachineInfo, continueType, startDay, endDay);
        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            TbrMouldProductionLogRecorder.addNoMatchPlanLog(context, productionStage, groupName, onLineMachineInfo, continueType);
            return;
        }
        //获取同规格同花纹或是同生胎同模具的其它sku排产计划
        List<MonthPlanProductionRequirePlanVo> matchList = ContinueSkuPrioritySelector.getContinueSkuPlanByType(context, productionStage, productionPlanInfo, continueType, continueSkuMap);
        if (CollectionUtils.isEmpty(matchList)) {
            TbrMouldProductionLogRecorder.addNoMatchPlanLog(context, productionStage, groupName, onLineMachineInfo, continueType);
            return;
        }
        //挑选下一个同规格同花纹的sku进行排产
        ContinueSkuNextSkuInfo selectSkuInfo = getNextSku(productionContext, earliestConclusionLhGroup, productionPlanInfo, productionStage, continueType, matchList, excludeSkuSet);
        if (null == selectSkuInfo) {
            excludeDaySet.add(startDay);
            //递归迭代下一个硫化组
            productionContinueByType(productionContext, productionStage, productionPlanInfo, continueType, endDay, continueSkuMap, excludeDaySet, new HashSet<>());
        }
        if (null == selectSkuInfo && excludeDaySet.contains(startDay)) {
            return;
        }
        String selectedMaterialDesc = selectSkuInfo.getMaterialDesc();
        //选择模具
        List<ProductionMouldInfoVo> selectedMouldList = selectSkuInfo.getSelectedMouldList();
        BeforeSkuProductionInfo beforeSkuInfo = selectSkuInfo.getLhBeforeSkuInfo();
        earliestConclusionLhGroup.updateBeforeSkuInfo(beforeSkuInfo);
        log.info(TbrMouldProductionLogRecorder.addContinueSkuStartSameInfoMouldLog(context, groupName, beforeSkuInfo.getMaterialDesc(), continueType, selectedMaterialDesc));
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = matchList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        //总排产量
        Integer sumProductionQty = ContinueSkuCalculator.getContinueSkuSummaryQty(productionStage, selectedProductionPlanList);
        MonthPlanProductionRequirePlanVo addSkuInfo = selectedProductionPlanList.get(BigDecimal.ZERO.intValue());
        //日硫化量
        Integer dayMaxProductionQty = addSkuInfo.getMaxDaySingleLhMachineQty();
        //实际排产量
        Integer realSumProductionQty = BigDecimal.ZERO.intValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, cxMachineCodeInfo, earliestConclusionLhGroup.transformCxLhGroup(), sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //逐日进行排产
        TbrMouldProductionLogRecorder.addSkuNeedProductionInfoLog(context, groupName, addSkuInfo.getMaterialDesc(), continueType.getDesc(), sumProductionQty);
        CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList, continueType);
        //递归：实际没有排产，则该Sku跳过
        Integer productionQty = lhProductionQtyHelper.getRealSumProductionQty();
        if (productionQty <= BigDecimal.ZERO.intValue()) {
            excludeSkuSet.add(selectedMaterialDesc);
        } else {
            //换模处理 20260522+ 同规格同花纹是否算换模次数，采用参数控制
            if (isAddChangeMoldCountByChangeMold(context, continueType)) {
                CxLhMouldProductionCalculator.addChangeMouldInfo(productionContext, addSkuInfo, startDay, beforeSkuInfo, selectedMouldList);
            }
            excludeDaySet = new HashSet<>();
        }
        //迭代下一个硫化组
        productionContinueByType(productionContext, productionStage, productionPlanInfo, continueType, endDay, continueSkuMap, excludeDaySet, excludeSkuSet);
    }

    /**
     * 续作-降膜排产信息设置
     * 1、周期结构-Sku排产量调整
     * 2、不同分组-强制降膜信息
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组计划
     * @param deductMould       降膜信息对象
     * @param cxContinueSkuInfo 续作Sku信息
     */
    private static void setDeductInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, DeductMouldVo deductMould, CxContinueSkuInfoHelper cxContinueSkuInfo) {
        //20260414+ 周期结构调整排产量
        Integer planDemandQty = CycleGroupCalculateHandler.getSingleSkuMaxQty(context, cxContinueSkuInfo, groupPlanInfo);
        if (null != planDemandQty) {
            //20260511+ 增加是否取高优先级量的处理
            Integer lhMachineCondition = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getContinueSkuProductionHeightRequire();
            planDemandQty = getContinueHeightQty(context, groupPlanInfo, lhMachineCondition, deductMould, cxContinueSkuInfo, planDemandQty);
            deductMould.setTotalQty(planDemandQty);
            deductMould.setRemainingQty(planDemandQty);
        }
        //20260421+ 不同分组-模具分配比例调整-强制降膜信息
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, Map<Integer, Integer>> forceDeductSkuMap = productionContext.getForceDeductSkuMap();
        if (CollectionUtils.isEmpty(forceDeductSkuMap)) {
            deductMould.setDayMaxMachinesLimitMap(null);
            return;
        }
        String materialDesc = cxContinueSkuInfo.getMaterialDesc();
        if (StringUtils.isBlank(materialDesc)) {
            deductMould.setDayMaxMachinesLimitMap(null);
            return;
        }
        Map<Integer, Integer> dayDeductMap = forceDeductSkuMap.get(materialDesc);
        if (CollectionUtils.isEmpty(dayDeductMap)) {
            deductMould.setDayMaxMachinesLimitMap(null);
            return;
        }
        Integer maxMouldNumber = cxContinueSkuInfo.getMouldNumber();
        Map<Integer, Integer> dayMaxMachinesLimitMap = new HashMap<>();
        dayDeductMap.forEach((deductDay, deductMoldNumber) -> {
            Integer dayMaxMoldNumber = maxMouldNumber - deductMoldNumber;
            Integer dayMaxLimitLhMachines = dayMaxMoldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            dayMaxMachinesLimitMap.put(deductDay, dayMaxLimitLhMachines);
        });
        if (CollectionUtils.isEmpty(dayMaxMachinesLimitMap)) {
            deductMould.setDayMaxMachinesLimitMap(null);
            return;
        }
        deductMould.setDayMaxMachinesLimitMap(dayMaxMachinesLimitMap);
    }

    /**
     * 获取下一个排产Sku
     *
     * @param productionContext 排产上下文
     * @param lhGroup           硫化组
     * @param productionStage   排产阶段
     * @param continueType      类型
     * @param matchList         可排产计划集合
     * @param excludeSkuSet     需要剔除的Sku集合
     * @return
     */
    private static ContinueSkuNextSkuInfo getNextSku(TbrProductionContext productionContext, EarliestConclusionLhGroupHelper lhGroup, ProductionPlanGroupInfo productionPlanInfo, ProductionStageEnum productionStage, ContinueTypeEnum continueType, List<MonthPlanProductionRequirePlanVo> matchList, Set<String> excludeSkuSet) {
        //改用Top3列表，不再单个优先级
        Integer startDay = lhGroup.getClosingDay();
        Integer endDay = lhGroup.getEndDay();
        String selectedMaterialDesc = SkuPrioritySelector.getHighestPrioritySku(productionContext, productionStage, null, productionPlanInfo, lhGroup, continueType, matchList, excludeSkuSet, null);
        String groupName = productionPlanInfo.getGroupName();
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            //todo 记录日志
            return null;
        }
        //选择模具
        List<ProductionMouldInfoVo> selectedMouldList = SkuMouldSelector.selectedDoubleMouldByRange(productionContext, selectedMaterialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(selectedMouldList)) {
            excludeSkuSet.add(selectedMaterialDesc);
            //记录日志
            productionContext.addSkuProductionLimitInfo(selectedMaterialDesc, MouldProductionLimitTypeEnum.FIND_MOULD_LIMIT);
            return getNextSku(productionContext, lhGroup, productionPlanInfo, productionStage, continueType, matchList, excludeSkuSet);
        }
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = matchList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        //20260327 修正根据materialDesc重新构建前Sku信息
        MonthPlanProductionRequirePlanVo addSkuInfo = selectedProductionPlanList.get(BigDecimal.ZERO.intValue());
        //SKU二次上机检查 sandy+ 20260129
        if (!SecondOnLineMachineHandler.checkSecondOnLine(productionPlanInfo, productionContext, addSkuInfo, startDay)) {
            TbrMouldProductionLogRecorder.addSkuProductionLimitLog(productionContext, productionPlanInfo.getGroupName(), "", addSkuInfo, startDay, MouldProductionLimitTypeEnum.SECOND_PRODUCTION_LIMIT);
            excludeSkuSet.add(selectedMaterialDesc);
            return getNextSku(productionContext, lhGroup, productionPlanInfo, productionStage, continueType, matchList, excludeSkuSet);
        }
        Set<String> cxMachineCodeInfo = Optional.ofNullable(productionPlanInfo.getAllocationCxMachineCodeSet()).orElse(Collections.emptySet());
        String onLineMachineInfo = String.join(StringConstant.COMMA, cxMachineCodeInfo);
        productionPlanInfo.correctProductionDateRange(productionContext, addSkuInfo, lhGroup, selectedMouldList, onLineMachineInfo);
        BeforeSkuProductionInfo beforeSku = lhGroup.getBeforeSkuInfo();
        Integer newStartDay = lhGroup.getClosingDay();
        endDay = lhGroup.getEndDay();
        TbrMouldProductionLogRecorder.addContinueGroupContinueMachineCorrectLhGroupRangeLog(productionContext, groupName, onLineMachineInfo, newStartDay, endDay, beforeSku);
        if (null == newStartDay || null == endDay || !startDay.equals(newStartDay)) {
            excludeSkuSet.add(selectedMaterialDesc);
            return getNextSku(productionContext, lhGroup, productionPlanInfo, productionStage, continueType, matchList, excludeSkuSet);
        }
        BeforeSkuProductionInfo lhBeforeSkuInfo = ConclusionLhMachineHandler.findChangeTypeBlockBeforeSkuByAddSku(productionContext, addSkuInfo, productionPlanInfo, startDay);
        TbrMouldProductionLogRecorder.addFindBeforeSkuInfo(productionContext, groupName, addSkuInfo.getMaterialDesc(), lhBeforeSkuInfo);
        if (null == lhBeforeSkuInfo) {
            excludeSkuSet.add(selectedMaterialDesc);
            return getNextSku(productionContext, lhGroup, productionPlanInfo, productionStage, continueType, matchList, excludeSkuSet);
        }
        return new ContinueSkuNextSkuInfo(selectedMaterialDesc, selectedMouldList, lhBeforeSkuInfo);
    }

    /**
     * 续作Sku接活字块是否加入换模次数
     * 1、不是同规格同花纹算换模次数
     * 2、同规格同花纹，看SYS0203016参数，Y则算
     *
     * @param context      排产上下文
     * @param continueType 排产类型
     * @return
     */
    private static boolean isAddChangeMoldCountByChangeMold(Context context, ContinueTypeEnum continueType) {
        if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
            return true;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        boolean isAddChangeMouldCount = productionContext.getBaseDataContainer().getParamConfiguration().isAddChangeMoldCountBySameSpecificationsPattern();
        //同规格同花纹排产下，且参数配置：算换模次数
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType && isAddChangeMouldCount) {
            return true;
        }
        return false;
    }

    /**
     * 增加续作是否先排产高优先级量
     *
     * @param context            排产上下文
     * @param groupPlanInfo      分组计划
     * @param lhMachineCondition 硫化机台条件
     * @param deductMould        续作排产信息
     * @param cxContinueSkuInfo  续作Sku信息
     * @param planDemandQty      排产量
     * @return
     */
    private static Integer getContinueHeightQty(Context context, ProductionPlanGroupInfo groupPlanInfo, Integer lhMachineCondition, DeductMouldVo deductMould, CxContinueSkuInfoHelper cxContinueSkuInfo, Integer planDemandQty) {
        if (null == lhMachineCondition) {
            lhMachineCondition = Integer.MAX_VALUE;
        }
        Integer lhMachineSize = deductMould.getMachinesAssigned();
        if (lhMachineSize <= lhMachineCondition) {
            return planDemandQty;
        }
        List<MonthPlanProductionRequirePlanVo> groupAllPlanList = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupAllPlanList)) {
            return planDemandQty;
        }
        String materialDesc = cxContinueSkuInfo.getMaterialDesc();
        List<MonthPlanProductionRequirePlanVo> allSkuPlanList = groupAllPlanList.stream().filter(singlePlan -> YesOrNoEnum.YES.getCode().equals(singlePlan.getIsProduction()) && materialDesc.equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allSkuPlanList)) {
            return planDemandQty;
        }
        MonthPlanProductionRequirePlanVo skuPlan = allSkuPlanList.get(BigDecimal.ZERO.intValue());
        boolean isAllSum = skuPlan.getIsAllSum();
        if (isAllSum) {
            return planDemandQty;
        }
        //需按净需求一起排产
        if (YesOrNoEnum.YES.getValue().equals(skuPlan.getIsProductionBySum())) {
            return planDemandQty;
        }
        //20260522+ 在没有高优级量，又能排产上的续作Sku名单中
        TbrProductionContext productionContext = (TbrProductionContext) context;
        if (productionContext.getSimulateResult().hasProductionFirstByContinueSku(materialDesc)) {
            return planDemandQty;
        }
        return allSkuPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
    }
}