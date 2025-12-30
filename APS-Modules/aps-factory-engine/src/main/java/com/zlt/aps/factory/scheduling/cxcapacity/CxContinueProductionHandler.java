package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.LhProductionQtyHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.ContinueTypeEnum;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.handler.SkuMouldSelector;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
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
     * 排产续作排产
     * 1、同规格同花纹
     * 2、共生胎、同模具
     *
     * @param context            排产上下文
     * @param productionPlanInfo 分组排产计划
     * @param continueType       续作类型 同规格同花纹 共生胎同模具
     * @param endDay             分组计划收尾日(理论)
     * @param continueSkuMap     分组计划中续作Sku信息集合
     * @param mouldShellMap      模壳信息
     */
    public static void productionContinueByType(Context context, ProductionPlanGroupInfo productionPlanInfo, ContinueTypeEnum continueType, Integer endDay, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //取得最早收尾的硫化组
        CxLhProductionHelper earliestConclusionLhGroup = productionPlanInfo.getEarliestConclusionLhGroup();
        if (null == earliestConclusionLhGroup) {
            //todo 记录日志
            return;
        }
        Integer startDay = earliestConclusionLhGroup.getProductionDay();
        if (startDay >= endDay) {
            //todo 记录日志
            return;
        }
        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productionPlanList)) {
            //todo 记录日志
            return;
        }
        //获取 续作收尾的sku 规格、花纹等信息
        String materialDesc = earliestConclusionLhGroup.getMaterialDesc();
        CxContinueSkuInfoHelper continueProductInfoHelper = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc, productionPlanList, continueSkuMap);
        //共用模具的sku
        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = productionContext.getBaseDataContainer().getSkuMouldRelationMap();
        Set<String> shareMouldMaterialDescSet = getShareMouldSkuByLhGroup(mouldInfoMap, earliestConclusionLhGroup);
        //获取同规格同花纹或是同生胎同模具的其它sku排产计划
        List<MonthPlanProductionRequirePlanVo> matchList = productionPlanInfo.getContinueListByType(continueType, materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        if (CollectionUtils.isEmpty(matchList)) {
            //todo
            return;
        }
        //挑选下一个同规格同花纹的sku进行排产
        String selectedMaterialDesc = getSelectedSuitableSku(matchList);
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            //todo 记录日志
            return;
        }
        //选中的续作模具
        List<ProductionMouldInfoVo> selectedMouldList = SkuMouldSelector.getSelectedMouldList(context, selectedMaterialDesc, earliestConclusionLhGroup, startDay, endDay);
        if (CollectionUtils.isEmpty(selectedMouldList)) {
            //todo 记录日志
            return;
        }
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = matchList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        Long sumProductionQty;
        Integer isProductionBySum = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getIsProductionBySum();
        if (YesOrNoEnum.YES.getValue().equals(isProductionBySum)) {
            //总净需求量
            sumProductionQty = selectedProductionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
        } else {
            //高优先级排产量
            sumProductionQty = selectedProductionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
        }
        //日硫化量
        Long dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaxDaySingleLhMachineQty();
        //实际排产量
        Long realSumProductionQty = BigDecimal.ZERO.longValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, null, earliestConclusionLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //逐日进行排产
        CxLhMouldProductionCalculator.lhProductionByLhGroupHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList);
        //迭代下一个硫化组
        productionContinueByType(productionContext, productionPlanInfo, continueType, endDay, continueSkuMap, mouldShellMap);
    }

//    /**
//     * 排产续作排产-同规格同花纹
//     *
//     * @param context            排产上下文
//     * @param productionPlanInfo 分组排产计划
//     * @param endDay             分组计划收尾日(理论)
//     * @param continueSkuMap     分组计划中续作Sku信息集合
//     * @param mouldShellMap      模壳信息
//     */
//    public static void productionSameSpecificationsAndPattern(Context context, ProductionPlanGroupInfo productionPlanInfo, Integer endDay, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
//        TbrProductionContext productionContext = (TbrProductionContext) context;
//        //取得最早收尾的硫化组
//        CxLhProductionHelper earliestConclusionLhGroup = productionPlanInfo.getEarliestConclusionLhGroup();
//        if (null == earliestConclusionLhGroup) {
//            //todo 记录日志
//            return;
//        }
//        Integer startDay = earliestConclusionLhGroup.getProductionDay();
//        if (startDay >= endDay) {
//            //todo 记录日志
//            return;
//        }
//        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(productionPlanList)) {
//            //todo 记录日志
//            return;
//        }
//        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = productionContext.getBaseDataContainer().getSkuMouldRelationMap();
//        //共用模具的sku
//        Set<String> shareMouldMaterialDescSet = getShareMouldSkuByLhGroup(mouldInfoMap, earliestConclusionLhGroup);
//        //续作收尾的sku
//        String materialDesc = earliestConclusionLhGroup.getMaterialDesc();
//        //获取规格、花纹等信息
//        CxContinueSkuInfoHelper continueProductInfoHelper = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc, productionPlanList, continueSkuMap);
//        //获取同规格同花纹的其它sku排产计划
//        List<MonthPlanProductionRequirePlanVo> sameSpecificationsAndPatternList = productionPlanInfo.getSameSpecificationsAndPatternPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
//        //挑选下一个同规格同花纹的sku进行排产
//        String selectedMaterialDesc = getSelectedSuitableSku(sameSpecificationsAndPatternList);
//        if (StringUtils.isBlank(selectedMaterialDesc)) {
//            //todo 记录日志
//            return;
//        }
//        //选中的续作模具
//        List<ProductionMouldInfoVo> selectedMouldList = getSelectedMouldList(context, selectedMaterialDesc, earliestConclusionLhGroup, mouldInfoMap, startDay, endDay);
//        if (CollectionUtils.isEmpty(selectedMouldList)) {
//            //todo
//            return;
//        }
//        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = sameSpecificationsAndPatternList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
//        //高优先级排产量
//        Long sumProductionQty = selectedProductionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
//        //日硫化量
//        Long dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaxDaySingleLhMachineQty();
//        //实际排产量
//        Long realSumProductionQty = BigDecimal.ZERO.longValue();
//        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, null, earliestConclusionLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
//        //逐日进行排产
//        CxLhMouldProductionCalculator.lhProductionByLhGroupHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList);
//        //迭代下一个硫化组
//        productionSameSpecificationsAndPattern(productionContext, productionPlanInfo, endDay, continueSkuMap, mouldShellMap);
//    }
//
//    /**
//     * 排产续作排产-同生胎共用模具
//     *
//     * @param context            排产上下文
//     * @param productionPlanInfo 分组排产计划
//     * @param endDay             分组计划收尾日(理论)
//     * @param continueSkuMap     分组计划中续作Sku信息集合
//     * @param mouldShellMap      模壳信息
//     */
//    public static void productionSameEmbryoCodeAndMould(Context context, ProductionPlanGroupInfo productionPlanInfo, Integer endDay, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
//        TbrProductionContext productionContext = (TbrProductionContext) context;
//        //取得最早收尾的硫化组
//        CxLhProductionHelper earliestConclusionLhGroup = productionPlanInfo.getEarliestConclusionLhGroup();
//        if (null == earliestConclusionLhGroup) {
//            //todo 记录日志
//            return;
//        }
//        Integer startDay = earliestConclusionLhGroup.getProductionDay();
//        if (startDay >= endDay) {
//            //todo 记录日志
//            return;
//        }
//        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(productionPlanList)) {
//            //todo 记录日志
//            return;
//        }
//        Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap = productionContext.getBaseDataContainer().getSkuMouldRelationMap();
//        //共用模具的sku
//        Set<String> shareMouldMaterialDescSet = getShareMouldSkuByLhGroup(mouldInfoMap, earliestConclusionLhGroup);
//        //续作收尾的sku
//        String materialDesc = earliestConclusionLhGroup.getMaterialDesc();
//        //获取规格、花纹等信息
//        CxContinueSkuInfoHelper continueProductInfoHelper = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc, productionPlanList, continueSkuMap);
//        //获取同生胎共用模具的其它sku排产计划
//        List<MonthPlanProductionRequirePlanVo> sameEmbryoCodeAndMouldList = productionPlanInfo.getSameEmbryoCodeAndMouldPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
//        //挑选下一个同生胎的sku进行排产
//        String selectedMaterialDesc = getSelectedSuitableSku(sameEmbryoCodeAndMouldList);
//        if (StringUtils.isBlank(selectedMaterialDesc)) {
//            //todo 记录日志
//            return;
//        }
//        //选中的续作模具
//        List<ProductionMouldInfoVo> selectedMouldList = getSelectedMouldList(context, selectedMaterialDesc, earliestConclusionLhGroup, mouldInfoMap, startDay, endDay);
//        if (CollectionUtils.isEmpty(selectedMouldList)) {
//            //todo
//            return;
//        }
//        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = sameEmbryoCodeAndMouldList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
//        //高优先级排产量
//        Long sumProductionQty = selectedProductionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
//        //日硫化量
//        Long dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getMaxDaySingleLhMachineQty();
//        //实际排产量
//        Long realSumProductionQty = BigDecimal.ZERO.longValue();
//        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, null, earliestConclusionLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
//        //逐日进行排产
//        CxLhMouldProductionCalculator.lhProductionByLhGroupHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList);
//        //迭代下一个硫化组
//        productionSameEmbryoCodeAndMould(productionContext, productionPlanInfo, endDay, continueSkuMap, mouldShellMap);
//    }

    /**
     * 从模具关系中和硫化组排产模具，挑选共用模具的物料集合
     *
     * @param mouldInfoMap sku与模具关系
     * @param cxLhGroup    硫化组信息-排产模具
     */
    private static Set<String> getShareMouldSkuByLhGroup(Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, CxLhProductionHelper cxLhGroup) {
        Set<String> shareMouldMaterialDescSet = new HashSet<>();
        mouldInfoMap.forEach((shareMouldMaterialDesc, mouldRelationList) -> {
            Set<String> mouldCodeSet = mouldRelationList.stream().map(MonthPlanProductMouldInfoVo::getMouldCode).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(mouldCodeSet)) {
                return;
            }
            //模具关系中全包含
            if (mouldCodeSet.containsAll(cxLhGroup.getProductionMouldSet())) {
                shareMouldMaterialDescSet.add(shareMouldMaterialDesc);
            }
        });
        return shareMouldMaterialDescSet;
    }

    /**
     * 获取续作sku合适的同规格同花纹/共生胎同模具的下多个sku
     * 优先选择高优级数量多的，其次是净需求量多的
     *
     * @param sameMultipleSkuList 同规格同花纹/共生胎同模具的下多个sku
     * @return
     */
    private static String getSelectedSuitableSku(List<MonthPlanProductionRequirePlanVo> sameMultipleSkuList) {
        //挑选可排产计划
        if (CollectionUtils.isEmpty(sameMultipleSkuList)) {
            //todo 记录日志
            return "";
        }
        //先取得高优先级量最大的
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = sameMultipleSkuList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Map<String, Long> productionSkuMap = new HashMap<>();
        skuGroupMap.forEach((skuMaterialDesc, groupPlanList) -> {
            Long sumHeightProductionQty = groupPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
            if (sumHeightProductionQty > BigDecimal.ZERO.longValue()) {
                productionSkuMap.put(skuMaterialDesc, sumHeightProductionQty);
            }
        });
        if (CollectionUtils.isEmpty(productionSkuMap)) {
            //todo 记录日志
            return "";
        }
        Optional<Map.Entry<String, Long>> maxEntry = productionSkuMap.entrySet().stream().max(Map.Entry.comparingByValue());
        return maxEntry.get().getKey();
    }
}
