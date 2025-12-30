package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在机结构，续作模具排产处理
 *
 * @author ZLT
 * @date 20251217
 */
@Slf4j
public class CxContinueSkuProductionHandler {

    /**
     * 续作排产
     * 1、续作SKU排产
     * 2、同规格同花纹排产
     * 3、换活字块排产(共生胎同模具)
     *
     * @param context            排产上下文
     * @param cxMachineCode      成型机台
     * @param productionPlanList 分组排产计划
     * @param productionPlan     排产信息，包含起始及收尾日期
     * @param mouldInfoMap       模具关系信息
     * @param mouldShellMap      模壳信息
     */
    public static void productionContinue(Context context, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        //构建成型机台对应的空硫化分组
        createCxLhRatioMapByContinue(context, cxMachineCode, productionPlan.getMaxRatio(), productionPlan.getProductionPlanInfo().getGroupName());
        //成型分配的排产日
        Integer startDay = productionPlan.getStartDay();
        Integer endDay = productionPlan.getEndDay();
        Integer cxLhGroupNo = BigDecimal.ONE.intValue();
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = productionPlan.getContinueSkuMap();
        //1、优先排产续作SKU
        for (Map.Entry<String, CxContinueSkuInfoHelper> entry : continueSkuMap.entrySet()) {
            CxContinueSkuInfoHelper continueSkuInfo = entry.getValue();
            String materialDesc = entry.getKey();
            ProductionSkuParamHelper paramHelper = new ProductionSkuParamHelper(startDay, endDay, cxMachineCode, cxLhGroupNo, materialDesc);
            cxLhGroupNo = productionSingleContinueSku(context, productionPlan, paramHelper, productionPlanList, continueSkuInfo, mouldInfoMap);
        }
        //2、再排产同规格同花纹，获取先收尾的硫化组信息
        productionSameSpecificationsAndPattern(context, productionPlan, cxMachineCode, endDay, productionPlanList, mouldInfoMap, mouldShellMap);
        //3、最后排产共生胎同模具
        productionSameEmbryoCodeAndMould(context, productionPlan, cxMachineCode, endDay, productionPlanList, mouldInfoMap, mouldShellMap);
    }

    /**
     * 根据成型对应的配比及续作信息，构建初始的硫化配比信息
     *
     * @param context       排产上下文
     * @param cxMachineCode 成型机台
     * @param ratio         配比信息
     * @param groupName     在机分组信息-TBR为结构
     */
    private static void createCxLhRatioMapByContinue(Context context, String cxMachineCode, Integer ratio, String groupName) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            return;
        }
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = cxMachineInfo.getCxLhRatioMap();
        if (!CollectionUtils.isEmpty(cxLhRatioMap)) {
            return;
        }
        Set<String> lhCxMachineInfo = new HashSet<>();
        lhCxMachineInfo.add(cxMachineCode);
        cxLhRatioMap = new HashMap<>(ratio);
        //初始化成型下配比的硫化分组
        for (int cxLhGroupNo = BigDecimal.ONE.intValue(); cxLhGroupNo <= ratio; cxLhGroupNo++) {
            CxLhProductionHelper cxLhHelper = CxLhProductionHelper.createEmptyLhGroup(groupName, cxLhGroupNo, lhCxMachineInfo);
            cxLhRatioMap.put(cxLhGroupNo, cxLhHelper);
        }
        cxMachineInfo.setCxLhRatioMap(cxLhRatioMap);
    }

    /**
     * 对单个续作Sku进行排产，并返回最新的硫化分组编号
     * 需要考虑与其同规格同花纹、换活字块(共生胎同模具)的高优先级需求量
     * 不能Sku排完非高优先级量导致其同规格同花纹、换活字块的高优先级需求量排不上
     *
     * @param context            排产上下文
     * @param paramHelper        排产信息(开始日~收尾日,成型机台,硫化分组,物料描述)
     * @param productionPlanList 分组排产计划
     * @param continueSkuInfo    续作Sku信息
     * @param mouldInfoMap       模具信息
     * @return
     */
    private static Integer productionSingleContinueSku(Context context, CxMachineAllocationPlanHelper productionPlan, ProductionSkuParamHelper paramHelper, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxContinueSkuInfoHelper continueSkuInfo, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap) {
        Integer startDay = paramHelper.getStartDay();
        Integer endDay = paramHelper.getEndDay();
        String cxMachineCode = paramHelper.getCxMachineCode();
        Integer cxLhGroupNo = paramHelper.getCxLhGroupNo();
        String materialDesc = paramHelper.getMaterialDesc();
        //成型硫化配比信息
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = cxMachineInfo.getCxLhRatioMap();
        //续作Sku使用的模具数-转化为对应的硫化分组(双模)
        Integer mouldNumber = continueSkuInfo.getMouldNumber();
        Integer lhMachineCount = mouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        List<MonthPlanProductMouldInfoVo> mouldList = mouldInfoMap.get(materialDesc);
        //选中的续作模具
        List<ProductionMouldInfoVo> selectedMouldList = selectedEnableMouldByNumber(context, mouldNumber, mouldList, startDay, endDay);
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = productionPlanList.stream().filter(groupPlan -> materialDesc.equals(groupPlan.getMaterialDesc())).collect(Collectors.toList());
        //日硫化量
        Long dayMaxProductionQty = continueSkuPlanList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //得到续作sku可排产量
        Long sumProductionQty = getContinueSkuTheoryProductionQty(context, paramHelper, productionPlanList, selectedMouldList, continueSkuInfo);
        Long realSumProductionQty = BigDecimal.ZERO.longValue();
        for (int lhCount = BigDecimal.ONE.intValue(); lhCount <= lhMachineCount; lhCount++) {
            //设置硫化组排产信息并得到对应模具--续作Sku
            CxLhProductionHelper cxLhProductionHelper = cxLhRatioMap.get(cxLhGroupNo);
            List<ProductionMouldInfoVo> selectedDouble = setCxLhGroupInfo(cxLhProductionHelper, startDay, lhCount, selectedMouldList, continueSkuInfo);
            //硫化组编号+1
            cxLhGroupNo = cxLhGroupNo + BigDecimal.ONE.intValue();
            //没有需求量或是没有模具
            if (sumProductionQty <= BigDecimal.ZERO.longValue() || CollectionUtils.isEmpty(selectedDouble)) {
                continue;
            }
            //逐日进行排产
            LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlan.getProductionPlanInfo(), cxMachineInfo, cxLhProductionHelper, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
            CxLhMouldProductionCalculator.lhProductionHandler(context, lhProductionQtyHelper, startDay, endDay, selectedDouble, continueSkuPlanList);
            sumProductionQty = lhProductionQtyHelper.getSumProductionQty();
            realSumProductionQty = lhProductionQtyHelper.getRealSumProductionQty();
        }
        return cxLhGroupNo;
    }

    /**
     * 排产续作排产-同规格同花纹
     *
     * @param context            排产上下文
     * @param productionPlan     排产分组计划信息对象
     * @param cxMachineCode      成型机台
     * @param endDay             分组计划收尾日
     * @param productionPlanList 排产计划
     * @param mouldInfoMap       模具信息
     * @param mouldShellMap      模壳信息
     */
    private static void productionSameSpecificationsAndPattern(Context context, CxMachineAllocationPlanHelper productionPlan, String cxMachineCode, Integer endDay, List<MonthPlanProductionRequirePlanVo> productionPlanList, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = productionPlan.getContinueSkuMap();
        //取得最早收尾的硫化组
        CxLhProductionHelper earliestConclusionLhGroup = getEarliestConclusionLhGroup(context, cxMachineCode);
        if (null == earliestConclusionLhGroup) {
            //todo 记录日志
            return;
        }
        Integer startDay = earliestConclusionLhGroup.getProductionDay();
        if (startDay >= endDay) {
            //todo 记录日志
            return;
        }
        //共用模具的sku
        Set<String> shareMouldMaterialDescSet = getShareMouldSkuByLhGroup(mouldInfoMap, earliestConclusionLhGroup);
        //续作收尾的sku
        String materialDesc = earliestConclusionLhGroup.getMaterialDesc();
        //获取规格、花纹等信息
        CxContinueSkuInfoHelper continueProductInfoHelper = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc, productionPlanList, continueSkuMap);
        //获取同规格同花纹的其它sku排产计划
        List<MonthPlanProductionRequirePlanVo> sameSpecificationsAndPatternList = productionPlan.getProductionPlanInfo().getSameSpecificationsAndPatternPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        //挑选下一个同规格同花纹的sku进行排产
        String selectedMaterialDesc = getSelectedSuitableSku(sameSpecificationsAndPatternList);
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            //todo 记录日志
            return;
        }
        //选中的续作模具
        List<ProductionMouldInfoVo> selectedMouldList = getSelectedMouldList(context, selectedMaterialDesc, earliestConclusionLhGroup, mouldInfoMap, startDay, endDay);
        if (CollectionUtils.isEmpty(selectedMouldList)) {
            //todo
            return;
        }
        ProductionSkuParamHelper paramHelper = new ProductionSkuParamHelper(startDay, endDay, cxMachineCode, earliestConclusionLhGroup.getLhGroupNo(), selectedMaterialDesc);
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = sameSpecificationsAndPatternList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        Long sumProductionQty = getSelectedSkuNeedProductionQty(context, paramHelper, selectedMouldList, selectedProductionPlanList, productionPlanList, continueProductInfoHelper);
        //日硫化量
        Long dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //实际排产量
        Long realSumProductionQty = BigDecimal.ZERO.longValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlan.getProductionPlanInfo(), cxMachineInfo, earliestConclusionLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //逐日进行排产
        CxLhMouldProductionCalculator.lhProductionHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList);
        //迭代下一个硫化组
        productionSameSpecificationsAndPattern(productionContext, productionPlan, cxMachineCode, endDay, productionPlanList, mouldInfoMap, mouldShellMap);
    }

    /**
     * 排产续作排产-同生胎共用模具
     *
     * @param context            排产上下文
     * @param productionPlan     分组排产计划信息对象
     * @param cxMachineCode      成型机台
     * @param endDay             分组计划收尾日
     * @param productionPlanList 排产计划
     * @param mouldInfoMap       模具信息
     * @param mouldShellMap      模壳信息
     */
    private static void productionSameEmbryoCodeAndMould(Context context, CxMachineAllocationPlanHelper productionPlan, String cxMachineCode, Integer endDay, List<MonthPlanProductionRequirePlanVo> productionPlanList, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        //取得最早收尾的硫化组
        CxLhProductionHelper earliestConclusionLhGroup = getEarliestConclusionLhGroup(context, cxMachineCode);
        if (null == earliestConclusionLhGroup) {
            //todo 记录日志
            return;
        }
        Integer startDay = earliestConclusionLhGroup.getProductionDay();
        if (startDay >= endDay) {
            //todo 记录日志
            return;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = productionPlan.getContinueSkuMap();
        //共用模具的sku
        Set<String> shareMouldMaterialDescSet = getShareMouldSkuByLhGroup(mouldInfoMap, earliestConclusionLhGroup);
        //续作收尾的sku
        String materialDesc = earliestConclusionLhGroup.getMaterialDesc();
        //获取规格、花纹等信息
        CxContinueSkuInfoHelper continueProductInfoHelper = CxContinueSkuInfoHelper.buildContinueProductInfo(materialDesc, productionPlanList, continueSkuMap);
        //获取同生胎共用模具的其它sku排产计划
        List<MonthPlanProductionRequirePlanVo> sameEmbryoCodeAndMouldList = productionPlan.getProductionPlanInfo().getSameEmbryoCodeAndMouldPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        //挑选下一个同生胎的sku进行排产
        String selectedMaterialDesc = getSelectedSuitableSku(sameEmbryoCodeAndMouldList);
        if (StringUtils.isBlank(selectedMaterialDesc)) {
            //todo 记录日志
            return;
        }
        //选中的续作模具
        List<ProductionMouldInfoVo> selectedMouldList = getSelectedMouldList(context, selectedMaterialDesc, earliestConclusionLhGroup, mouldInfoMap, startDay, endDay);
        if (CollectionUtils.isEmpty(selectedMouldList)) {
            //todo
            return;
        }
        ProductionSkuParamHelper paramHelper = new ProductionSkuParamHelper(startDay, endDay, cxMachineCode, earliestConclusionLhGroup.getLhGroupNo(), selectedMaterialDesc);
        List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList = sameEmbryoCodeAndMouldList.stream().filter(selectedPlan -> selectedPlan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        Long sumProductionQty = getSelectedSkuNeedProductionQty(context, paramHelper, selectedMouldList, selectedProductionPlanList, productionPlanList, continueProductInfoHelper);
        //日硫化量
        Long dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //实际排产量
        Long realSumProductionQty = BigDecimal.ZERO.longValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlan.getProductionPlanInfo(), cxMachineInfo, earliestConclusionLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //逐日进行排产
        CxLhMouldProductionCalculator.lhProductionHandler(context, lhProductionQtyHelper, startDay, endDay, selectedMouldList, selectedProductionPlanList);
        //迭代下一个硫化组
        productionSameEmbryoCodeAndMould(productionContext, productionPlan, cxMachineCode, endDay, productionPlanList, mouldInfoMap, mouldShellMap);
    }


    /**
     * 从mouldList关系中获取能在startDay~endDay范围内可排产模具集合
     * 并符合mouldNumber数量
     *
     * @param context     排产上下文
     * @param mouldNumber 模具数量
     * @param mouldList   sku配置的模具
     * @param startDay    开始排产日
     * @param endDay      结束排产日
     * @return
     */
    private static List<ProductionMouldInfoVo> selectedEnableMouldByNumber(Context context, Integer mouldNumber, List<MonthPlanProductMouldInfoVo> mouldList, Integer startDay, Integer endDay) {
        //没有模具关系，续作模具数，结构排产计划则直接返回
        if (CollectionUtils.isEmpty(mouldList) || mouldNumber <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyList();
        }
        List<ProductionMouldInfoVo> enableSelectedList = selectedEnableProductionMould(context, mouldList, startDay, endDay);
        if (CollectionUtils.isEmpty(enableSelectedList)) {
            return Collections.emptyList();
        }
        enableSelectedList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue));
        List<ProductionMouldInfoVo> maxSelectedMouldList;
        if (enableSelectedList.size() > mouldNumber) {
            maxSelectedMouldList = enableSelectedList.subList(BigDecimal.ZERO.intValue(), mouldNumber);
        } else {
            maxSelectedMouldList = enableSelectedList;
        }
        return maxSelectedMouldList;
    }

    /**
     * 续作Sku量，不能直接是直接的量，需要根据模具在开始日~收尾日的产能估算，
     * 其它同规格同花纹、共生胎同模具的高优先级排产量
     * 在包容其它同规格同花纹、共生胎同模具的高优级的情形下，续作sku非高优先级可能需要提前下机
     *
     * @param context            排产上下文
     * @param paramHelper        排产信息(开始日~收尾日,成型机台,硫化分组,物料描述)
     * @param productionPlanList 所有分组下需要排产的计划
     * @param selectedMouldList  选中的模具信息
     * @param continueSkuInfo    续作Sku信息-规格、花纹、生胎等信息
     * @return
     */
    private static Long getContinueSkuTheoryProductionQty(Context context, ProductionSkuParamHelper paramHelper, List<MonthPlanProductionRequirePlanVo> productionPlanList, List<ProductionMouldInfoVo> selectedMouldList, CxContinueSkuInfoHelper continueSkuInfo) {
        Integer startDay = paramHelper.getStartDay();
        Integer endDay = paramHelper.getEndDay();
        String materialDesc = paramHelper.getMaterialDesc();
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = productionPlanList.stream().filter(groupPlan -> materialDesc.equals(groupPlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(continueSkuPlanList)) {
            return BigDecimal.ZERO.longValue();
        }
        //共用模具sku信息
        Set<String> shareMaterialDesc = new HashSet<>();
        selectedMouldList.forEach(productionMouldInfo -> shareMaterialDesc.addAll(productionMouldInfo.getAssociationMaterialSet()));
        //续作SKU高优先级待排产量
        Long heightProductionQty = continueSkuPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
        //续作SKU所有待排产量
        Long sumProductionQty = continueSkuPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
        //日硫化量
        Long dayMaxProductionQty = continueSkuPlanList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //其它共用模具的高优先级待排产量
        Long otherShareMouldHeightQty = getShareMouldOtherHeightQty(materialDesc, continueSkuInfo, shareMaterialDesc, productionPlanList);
        Long maxCapacity = getMaxCapacityQty(context, selectedMouldList, dayMaxProductionQty, startDay, endDay);
        //得到续作sku可排产量
        sumProductionQty = getContinueSkuSumProductionQty(sumProductionQty, heightProductionQty, maxCapacity, otherShareMouldHeightQty);
        return sumProductionQty;
    }

    /**
     * 设置成型硫化组信息-续作排产信息
     *
     * @param cxLhProductionHelper 对应硫化组信息
     * @param startDay             排产开始日-初始的
     * @param lhCount              续作硫化组数
     * @param selectedMouldList    续作模具数
     * @param continueSkuInfo      续作Sku信息
     * @return
     */
    private static List<ProductionMouldInfoVo> setCxLhGroupInfo(CxLhProductionHelper cxLhProductionHelper, Integer startDay, Integer lhCount, List<ProductionMouldInfoVo> selectedMouldList, CxContinueSkuInfoHelper continueSkuInfo) {
        List<ProductionMouldInfoVo> selectedDouble;
        int needSize = lhCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        if (needSize <= selectedMouldList.size()) {
            Integer startIndex = (lhCount - BigDecimal.ONE.intValue()) * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            Integer endIndex = lhCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            selectedDouble = selectedMouldList.subList(startIndex, endIndex);
        } else {
            selectedDouble = new ArrayList<>();
        }
        addContinueSkuInfo(startDay, cxLhProductionHelper, continueSkuInfo, selectedDouble);
        return selectedDouble;
    }

    /**
     * 获取成型机台下，最早收尾的硫化组信息
     *
     * @param context       排产上下文
     * @param cxMachineCode 成型机台
     */
    private static CxLhProductionHelper getEarliestConclusionLhGroup(Context context, String cxMachineCode) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            //todo 记录日志
            return null;
        }
        return cxMachineInfo.getEarliestConclusionLhGroup();
    }

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
        //挑选同规格同花纹的可排产计划
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
            //再取得其它优先级量最大的
            skuGroupMap.forEach((skuMaterialDesc, groupPlanList) -> {
                Long sumNoHeightProductionQty = groupPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
                if (sumNoHeightProductionQty > BigDecimal.ZERO.longValue()) {
                    productionSkuMap.put(skuMaterialDesc, sumNoHeightProductionQty);
                }
            });
        }
        if (CollectionUtils.isEmpty(productionSkuMap)) {
            //todo 记录日志
            return "";
        }
        Optional<Map.Entry<String, Long>> maxEntry = productionSkuMap.entrySet().stream().max(Map.Entry.comparingByValue());
        return maxEntry.get().getKey();
    }

    /**
     * 获取选中模具信息
     *
     * @param context                   排产上下文
     * @param selectedMaterialDesc      选中的sku
     * @param earliestConclusionLhGroup 收尾硫化组
     * @param mouldInfoMap              sku模具关系
     * @param startDay                  排产开始日
     * @param endDay                    排产结束日
     * @return
     */
    private static List<ProductionMouldInfoVo> getSelectedMouldList(Context context, String selectedMaterialDesc, CxLhProductionHelper earliestConclusionLhGroup, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Integer startDay, Integer endDay) {
        List<MonthPlanProductMouldInfoVo> allMouldList = mouldInfoMap.get(selectedMaterialDesc);
        Set<String> productionMouldSet = earliestConclusionLhGroup.getProductionMouldSet();
        List<MonthPlanProductMouldInfoVo> selectedMouldRelationList = new ArrayList<>();
        allMouldList.forEach(mouldRelationInfo -> {
            if (productionMouldSet.contains(mouldRelationInfo.getMouldCode())) {
                selectedMouldRelationList.add(mouldRelationInfo);
            }
        });
        //选中的续作模具
        return selectedEnableMouldByNumber(context, ProductionConstant.DOUBLE_MOULD_PRODUCTION, selectedMouldRelationList, startDay, endDay);
    }

    /**
     * 得到选中的sku的可排产量，需要考虑其它规格的高优先级量
     *
     * @param context                    排产上下文
     * @param paramHelper                排产参数辅助信息
     * @param selectedMouldList          选中的模具
     * @param selectedProductionPlanList 选中sku的计划
     * @param productionPlanList         分组下所有计划(包含同规格同花纹、共生胎同模具的计划)
     * @return
     */
    private static Long getSelectedSkuNeedProductionQty(Context context, ProductionSkuParamHelper paramHelper, List<ProductionMouldInfoVo> selectedMouldList, List<MonthPlanProductionRequirePlanVo> selectedProductionPlanList, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxContinueSkuInfoHelper continueProductInfoHelper) {
        Integer startDay = paramHelper.getStartDay();
        Integer endDay = paramHelper.getEndDay();
        String materialDesc = paramHelper.getMaterialDesc();
        //日硫化量
        Long dayMaxProductionQty = selectedProductionPlanList.get(BigDecimal.ZERO.intValue()).getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        //模具最大产能
        Long maxCapacity = getMaxCapacityQty(context, selectedMouldList, dayMaxProductionQty, startDay, endDay);
        //高优先级排产量
        Long sumHeightProductionQty = selectedProductionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
        //总的需排产量
        Long sumNeedProductionQty = selectedProductionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
        //共用模具sku信息
        Set<String> shareMaterialDesc = new HashSet<>();
        selectedMouldList.forEach(productionMouldInfo -> shareMaterialDesc.addAll(productionMouldInfo.getAssociationMaterialSet()));
        //其它共用模具的高优先级待排产量
        Long otherShareMouldHeightQty = getShareMouldOtherHeightQty(materialDesc, continueProductInfoHelper, shareMaterialDesc, productionPlanList);
        return getContinueSkuSumProductionQty(sumNeedProductionQty, sumHeightProductionQty, maxCapacity, otherShareMouldHeightQty);
    }

    /**
     * 从mouldList的模具关系中，挑选符合startDay~endDay可进行排产的模具
     *
     * @param context   排产上下文
     * @param mouldList SKU配置的所有模具关系
     * @param startDay  开始排产日--一般为前一个SKU的收尾日
     * @param endDay    结束排产日
     * @return
     */
    private static List<ProductionMouldInfoVo> selectedEnableProductionMould(Context context, List<MonthPlanProductMouldInfoVo> mouldList, Integer startDay, Integer endDay) {
        if (CollectionUtils.isEmpty(mouldList)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionMouldInfoVo> mouldInfoMap = productionContext.getBaseDataContainer().getMouldInfoMap();
        if (CollectionUtils.isEmpty(mouldInfoMap)) {
            return Collections.emptyList();
        }
        List<ProductionMouldInfoVo> enableSelectedList = new ArrayList<>();
        Set<String> mouldSet = mouldList.stream().map(MonthPlanProductMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        mouldSet.forEach(mouldCode -> {
            ProductionMouldInfoVo mouldInfo = mouldInfoMap.get(mouldCode);
            if (null == mouldInfo) {
                return;
            }
            if (!mouldInfo.isProduction(startDay, endDay)) {
                return;
            }
            enableSelectedList.add(mouldInfo);
        });
        return enableSelectedList;
    }

    /**
     * 根据续作模具数，得到共用模具的其他物料的高优先级待排产量
     *
     * @param materialDesc        物料描述
     * @param continueProductInfo 续作Sku信息
     * @param shareMaterialDesc   共用模具的物料信息
     * @param productionPlanList  结构下的所有SKU计划
     * @return
     */
    private static Long getShareMouldOtherHeightQty(String materialDesc, CxContinueSkuInfoHelper continueProductInfo, Set<String> shareMaterialDesc, List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        //没有模具关系，续作模具数，结构排产计划则直接返回
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return BigDecimal.ZERO.longValue();
        }
        if (CollectionUtils.isEmpty(shareMaterialDesc)) {
            return BigDecimal.ZERO.longValue();
        }
        List<MonthPlanProductionRequirePlanVo> shareMouldPlanList = new ArrayList<>();
        productionPlanList.forEach(productionPlan -> {
            if (!productionPlan.hasProduction()) {
                return;
            }
            if (materialDesc.equals(productionPlan.getMaterialDesc())) {
                return;
            }
            //判断是否共用
            if (!shareMaterialDesc.contains(productionPlan.getMaterialDesc())) {
                return;
            }
            //共用模具下，同规格同花纹，共生胎
            if (!productionPlan.hasContinueProduction(continueProductInfo)) {
                return;
            }
            shareMouldPlanList.add(productionPlan);
        });
        if (CollectionUtils.isEmpty(shareMouldPlanList)) {
            return BigDecimal.ZERO.longValue();
        }
        //提取高优先级数量
        return shareMouldPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
    }

    /**
     * 计算模具在startDay~endDay范围内最大模具产能
     *
     * @param context             排产上下文
     * @param selectedMouldList   模具信息
     * @param dayMaxProductionQty 日硫化量
     * @param startDay            排产起始天数
     * @param endDay              排产结束天数
     * @return
     */
    private static Long getMaxCapacityQty(Context context, List<ProductionMouldInfoVo> selectedMouldList, Long dayMaxProductionQty, Integer startDay, Integer endDay) {
        Integer lhMachineCount = selectedMouldList.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        Long dayMaxCapacity = dayMaxProductionQty * lhMachineCount;
        int realDays = BigDecimal.ZERO.intValue();
        Set<Integer> stopDays = context.getStopDays();
        for (int day = startDay; day <= endDay; day++) {
            if (null != stopDays && stopDays.contains(day)) {
                continue;
            }
            realDays = realDays + BigDecimal.ONE.intValue();
        }
        return dayMaxCapacity * realDays;
    }

    /**
     * 获取续作Sku总的待排产量
     * 在模具产能优先满足高优先级量(续作Sku和非续作Sku)
     *
     * @param continueSkuSumProductionQty    续作Sku总的待排产量
     * @param continueSkuHeightProductionQty 续作Sku高优先级待排产量
     * @param maxCapacity                    续作模具最大产能
     * @param otherShareMouldHeightQty       共用模具其它高优先级待排产量
     * @return
     */
    private static Long getContinueSkuSumProductionQty(Long continueSkuSumProductionQty, Long continueSkuHeightProductionQty, Long maxCapacity, Long otherShareMouldHeightQty) {
        //模具最大产能不足以满足续作Sku的高优先级排产量
        if (continueSkuHeightProductionQty >= maxCapacity) {
            return continueSkuHeightProductionQty;
        }
        //获得所有高优先级待排产量
        Long allHeightProductionQty = continueSkuHeightProductionQty + otherShareMouldHeightQty;
        if (allHeightProductionQty >= maxCapacity) {
            return continueSkuHeightProductionQty;
        }
        //最大高优先级可排产量
        Long maxHeightProductionQty = Math.min(allHeightProductionQty, maxCapacity);
        //在高优先级量的基础上，可增加的量
        Long diffQty = maxCapacity - maxHeightProductionQty;
        //理论最大可排产量
        Long theoryMaxQty = continueSkuHeightProductionQty + diffQty;
        return Math.min(theoryMaxQty, continueSkuSumProductionQty);
    }

    /**
     * 增加续作排产Sku信息
     *
     * @param startDay             起始天
     * @param cxLhProductionHelper 成型硫化分组对象
     * @param continueSkuInfo      续作Sku信息
     * @param selectedDoubleList   选中的模具
     */
    private static void addContinueSkuInfo(Integer startDay, CxLhProductionHelper cxLhProductionHelper, CxContinueSkuInfoHelper continueSkuInfo, List<ProductionMouldInfoVo> selectedDoubleList) {
        cxLhProductionHelper.setProductionDay(startDay);
        cxLhProductionHelper.setProductionQty(BigDecimal.ZERO.longValue());
        cxLhProductionHelper.setMaterialCode(continueSkuInfo.getMaterialCode());
        cxLhProductionHelper.setMaterialDesc(continueSkuInfo.getMaterialDesc());
        if (CollectionUtils.isEmpty(selectedDoubleList)) {
            cxLhProductionHelper.setProductionMouldSet(new HashSet<>());
        } else {
            cxLhProductionHelper.setProductionMouldSet(selectedDoubleList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet()));
        }
    }
}
