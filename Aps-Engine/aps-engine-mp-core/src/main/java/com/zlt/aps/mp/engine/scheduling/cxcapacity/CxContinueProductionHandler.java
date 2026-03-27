package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.constant.StringConstant;
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
        ProductionCapacityParamConfiguration paramConfiguration = context.getBaseDataContainer().getParamConfiguration();
        //续作Sku轮询排产
        String groupName = groupPlanInfo.getGroupName();
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            //log.info();
            TbrMouldProductionLogRecorder.addContinueSkuStartMouldLog(context, groupName, materialDesc);
            if (!cxContinueSkuInfo.hasProduction()) {
                log.info(TbrMouldProductionLogRecorder.addContinueSkuNoProductionQtyLog(context, groupName, materialDesc));
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
            List<DailyScheduleVo> resultList = DeductMouldScheduler.scheduleProduction(deductMould);
            //分配结果
            if (CollectionUtils.isEmpty(resultList)) {
                //记录日志
                log.info(TbrMouldProductionLogRecorder.addContinueSkuNoProductionResultLog(context, groupName, materialDesc));
                return;
            }
            String mouldInfo = selectMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.joining(StringConstant.COMMA));
            //log.info();
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
     */
    public static void productionContinueByType(Context context, ProductionStageEnum productionStage, ProductionPlanGroupInfo productionPlanInfo, ContinueTypeEnum continueType, Integer endDay, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Set<Integer> excludeDaySet) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = productionPlanInfo.getGroupName();
        Set<String> cxMachineCodeInfo = continueSkuMap.values().stream().collect(Collectors.toList()).get(BigDecimal.ZERO.intValue()).getOnLineCxMachineSet();
        String onLineMachineInfo = String.join(StringConstant.COMMA, cxMachineCodeInfo);
        //取得最早收尾的续作硫化组
        EarliestConclusionLhGroupHelper earliestConclusionLhGroup = productionPlanInfo.getEarliestConclusionLhInfoByContinueSku(context, continueSkuMap, excludeDaySet);
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
        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            //todo 记录日志
            return;
        }
        //获取同规格同花纹或是同生胎同模具的其它sku排产计划
        List<MonthPlanProductionRequirePlanVo> matchList = ContinueSkuPrioritySelector.getContinueSkuPlanByType(context, productionStage, productionPlanInfo, continueType, continueSkuMap);
        if (CollectionUtils.isEmpty(matchList)) {
            //todo
            return;
        }
        Set<String> excludeSkuSet = new HashSet<>();
        //挑选下一个同规格同花纹的sku进行排产
        ContinueSkuNextSkuInfo selectSkuInfo = getNextSku(productionContext, productionPlanInfo, productionStage, matchList, excludeSkuSet, startDay, endDay);
        if (null == selectSkuInfo) {
            excludeDaySet.add(startDay);
            //递归迭代下一个硫化组
            productionContinueByType(productionContext, productionStage, productionPlanInfo, continueType, endDay, continueSkuMap, excludeDaySet);
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
        //日硫化量
        Integer dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaxDaySingleLhMachineQty();
        //实际排产量
        Integer realSumProductionQty = BigDecimal.ZERO.intValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, cxMachineCodeInfo, earliestConclusionLhGroup.transformCxLhGroup(), sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //逐日进行排产
        CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList, continueType);
        //迭代下一个硫化组
        productionContinueByType(productionContext, productionStage, productionPlanInfo, continueType, endDay, continueSkuMap, excludeDaySet);
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
     * @param continueSkuMap     分组计划中续作Sku信息集合
     */
    @Deprecated
    public static void oldProductionContinueByType(Context context, ProductionStageEnum productionStage, ProductionPlanGroupInfo productionPlanInfo, ContinueTypeEnum continueType, Integer endDay, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Set<Integer> excludeDaySet) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = productionPlanInfo.getGroupName();
        Set<String> cxMachineCodeInfo = continueSkuMap.values().stream().collect(Collectors.toList()).get(BigDecimal.ZERO.intValue()).getOnLineCxMachineSet();
        String onLineMachineInfo = String.join(StringConstant.COMMA, cxMachineCodeInfo);
        //取得最早收尾的续作硫化组
        EarliestConclusionLhGroupHelper earliestConclusionLhGroup = productionPlanInfo.getEarliestConclusionLhInfoByContinueSku(context, continueSkuMap, excludeDaySet);
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
        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            //todo 记录日志
            return;
        }
        //获取 续作收尾的sku 规格、花纹等信息
        String materialDesc = earliestConclusionLhGroup.getBeforeMaterialDesc();
        CxContinueSkuInfoHelper continueProductInfoHelper = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc, productionPlanList, continueSkuMap);
        //共用模具的sku
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = productionContext.getBaseDataContainer().getSkuMouldRelationMap();
        Set<String> shareMouldMaterialDescSet = getShareMouldSkuByLhGroup(mouldInfoMap, earliestConclusionLhGroup);
        //获取同规格同花纹或是同生胎同模具的其它sku排产计划
        List<MonthPlanProductionRequirePlanVo> matchList = productionPlanInfo.getContinueListByType(productionStage, continueType, materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        if (CollectionUtils.isEmpty(matchList)) {
            //todo
            return;
        }
        //挑选下一个同规格同花纹的sku进行排产
        String selectedMaterialDesc = getSelectedSuitableSku(productionStage, matchList);
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            //todo 记录日志
            return;
        }
        //选中的续作模具
        List<ProductionMouldInfoVo> selectedMouldList = SkuMouldSelector.getSelectedMouldList(context, selectedMaterialDesc, earliestConclusionLhGroup, startDay, realEndDay);
        if (CollectionUtils.isEmpty(selectedMouldList)) {
            //todo 记录日志
            productionContext.addSkuProductionLimitInfo(selectedMaterialDesc, MouldProductionLimitTypeEnum.FIND_MOULD_LIMIT);
            return;
        }
        log.info(TbrMouldProductionLogRecorder.addContinueSkuStartSameInfoMouldLog(context, groupName, materialDesc, continueType, selectedMaterialDesc));
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = matchList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        //总排产量
        Integer sumProductionQty = ContinueSkuCalculator.getContinueSkuSummaryQty(productionStage, selectedProductionPlanList);
        //日硫化量
        Integer dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaxDaySingleLhMachineQty();
        //实际排产量
        Integer realSumProductionQty = BigDecimal.ZERO.intValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, null, earliestConclusionLhGroup.transformCxLhGroup(), sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //逐日进行排产
        CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList, continueType);
        //迭代下一个硫化组
        oldProductionContinueByType(productionContext, productionStage, productionPlanInfo, continueType, endDay, continueSkuMap, excludeDaySet);
    }

    /**
     * 获取下一个排产Sku
     *
     * @param productionContext 排产上下文
     * @param productionStage   排产阶段
     * @param matchList         可排产计划集合
     * @param excludeSkuSet     需要剔除的Sku集合
     * @param startDay          起始日
     * @param endDay            结束日
     * @return
     */
    private static ContinueSkuNextSkuInfo getNextSku(TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo, ProductionStageEnum productionStage, List<MonthPlanProductionRequirePlanVo> matchList, Set<String> excludeSkuSet, Integer startDay, Integer endDay) {
        String selectedMaterialDesc = ContinueSkuPrioritySelector.getHeightPrioritySku(productionStage, matchList, excludeSkuSet);
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
            return getNextSku(productionContext, productionPlanInfo, productionStage, matchList, excludeSkuSet, startDay, endDay);
        }
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = matchList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        //20260327 修正根据materialDesc重新构建前Sku信息
        MonthPlanProductionRequirePlanVo addSkuInfo = selectedProductionPlanList.get(BigDecimal.ZERO.intValue());
        BeforeSkuProductionInfo lhBeforeSkuInfo = ConclusionLhMachineHandler.findChangeTypeBlockBeforeSkuByAddSku(productionContext, addSkuInfo, productionPlanInfo, startDay);
        TbrMouldProductionLogRecorder.addFindBeforeSkuInfo(productionContext, groupName, addSkuInfo.getMaterialDesc(), lhBeforeSkuInfo);
        if (null == lhBeforeSkuInfo) {
            excludeSkuSet.add(selectedMaterialDesc);
            return getNextSku(productionContext, productionPlanInfo, productionStage, matchList, excludeSkuSet, startDay, endDay);
        }
        return new ContinueSkuNextSkuInfo(selectedMaterialDesc, selectedMouldList, lhBeforeSkuInfo);
    }

    /**
     * 从模具关系中和硫化组排产模具，挑选共用模具的物料集合
     *
     * @param mouldInfoMap              sku与模具关系
     * @param earliestConclusionLhGroup 收尾信息
     */
    private static Set<String> getShareMouldSkuByLhGroup(Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, EarliestConclusionLhGroupHelper earliestConclusionLhGroup) {
        Set<String> shareMouldMaterialDescSet = new HashSet<>();
        mouldInfoMap.forEach((shareMouldMaterialDesc, mouldRelationList) -> {
            Set<String> mouldCodeSet = mouldRelationList.stream().map(MonthPlanProductMouldInfoVo::getMouldCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(mouldCodeSet)) {
                return;
            }
            //模具关系中全包含
            if (mouldCodeSet.containsAll(earliestConclusionLhGroup.getUsedMouldSet())) {
                shareMouldMaterialDescSet.add(shareMouldMaterialDesc);
            }
        });
        return shareMouldMaterialDescSet;
    }

    /**
     * 获取续作sku合适的同规格同花纹/共生胎同模具的下多个sku
     * 优先选择高优级数量多的，其次是净需求量多的
     *
     * @param productionStage     排产阶段
     * @param sameMultipleSkuList 同规格同花纹/共生胎同模具的下多个sku
     * @return
     */
    private static String getSelectedSuitableSku(ProductionStageEnum productionStage, List<MonthPlanProductionRequirePlanVo> sameMultipleSkuList) {
        //挑选可排产计划
        if (CollectionUtils.isEmpty(sameMultipleSkuList)) {
            //todo 记录日志
            return "";
        }
        //先取得高优先级量最大的
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = sameMultipleSkuList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Map<String, Integer> productionSkuMap = new HashMap<>();
        skuGroupMap.forEach((skuMaterialDesc, groupPlanList) -> {
            Integer sumProductionQty = ContinueSkuCalculator.getContinueSkuSummaryQty(productionStage, groupPlanList);
            if (sumProductionQty > BigDecimal.ZERO.intValue()) {
                productionSkuMap.put(skuMaterialDesc, sumProductionQty);
            }
        });
        if (CollectionUtils.isEmpty(productionSkuMap)) {
            //todo 记录日志
            return "";
        }
        Optional<Map.Entry<String, Integer>> maxEntry = productionSkuMap.entrySet().stream().max(Map.Entry.comparingByValue());
        return maxEntry.get().getKey();
    }
}