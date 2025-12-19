package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxContinueProductInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型模具排产业务处理
 *
 * @author ZLT
 * @date 20251217
 */
@Slf4j
public class CxMouldProductionHandler {

    /**
     * 在机结构，进行模具排产
     * 1、先排续作规格
     * 1.1、续作SKU
     * 1.2、同规格同花纹
     * 1.3、换活字块-共生胎同模具
     * 2、收尾新增规格
     *
     * @param context        排产上下文
     * @param cxMachineCode  成型机台
     * @param productionPlan 排产计划信息
     * @param mouldInfoMap   模具关系信息
     * @param mouldShellMap  模壳信息
     */
    public static void continueGroupPlanMouldProduction(Context context, String cxMachineCode, CxMachineAllocationPlanHelper productionPlan, CxContinueInfoHelper cxContinueInfo, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlan.getProductionPlanInfo().getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            //todo 记录日志
            return;
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionPlanList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionPlanList)) {
            //todo 记录日志
            return;
        }
        //先续作排产： 续作SKU ->同规格同花纹 -> 换活字块共生胎同模具
        Map<String, CxContinueProductInfoHelper> continueSkuMap = cxContinueInfo.getCxMachineGroup().get(cxMachineCode);
        if (!CollectionUtils.isEmpty(continueSkuMap)) {
            productionContinueSku(context, cxMachineCode, hasProductionPlanList, continueSkuMap, productionPlan, mouldInfoMap, mouldShellMap);
        }


        //排产收尾新增规格


    }

    /**
     * 续作排产
     * 1、续作SKU排产
     * 2、同规格同花纹排产
     * 3、换活字块排产(共生胎同模具)
     *
     * @param context            排产上下文
     * @param cxMachineCode      成型机台
     * @param productionPlanList 排产计划
     * @param continueSkuMap     续作SKU信息
     * @param productionPlan     排产信息，包含起始及收尾日期
     * @param mouldInfoMap       模具关系信息
     * @param mouldShellMap      模壳信息
     */
    private static void productionContinueSku(Context context, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, Map<String, CxContinueProductInfoHelper> continueSkuMap, CxMachineAllocationPlanHelper productionPlan, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        Integer startDay = productionPlan.getStartDay();
        Integer endDay = productionPlan.getEndDay();
        //优先排产续作SKU
        continueSkuMap.forEach((materialDesc, continueSkuInfo) -> {
            Integer mouldNumber = continueSkuInfo.getMouldNumber();
            List<MonthPlanProductMouldInfoVo> mouldList = mouldInfoMap.get(materialDesc);
            if (CollectionUtils.isEmpty(mouldList)) {
                //todo 记录日志-没有模具
                return;
            }
            List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = productionPlanList.stream().filter(groupPlan -> materialDesc.equals(groupPlan.getMaterialDesc())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(continueSkuPlanList)) {
                //todo 没有续作计划
                return;
            }
            //选中的续作模具
            List<ProductionMouldInfoVo> selectedMouldList = selectedEnableMouldByNumber(context, mouldNumber, mouldList, startDay, endDay);
            if (CollectionUtils.isEmpty(selectedMouldList)) {
                //todo 没有可用模具
                return;
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
            Long realSumProductionQty = BigDecimal.ZERO.longValue();
            for (int mouldQty = ProductionConstant.DOUBLE_MOULD_QTY; mouldQty <= mouldNumber; ) {
                if (mouldQty > selectedMouldList.size()) {
                    //todo 没有模具
                    break;
                }
                List<ProductionMouldInfoVo> selectedDoubleList = selectedMouldList.subList(mouldQty - ProductionConstant.DOUBLE_MOULD_PRODUCTION, mouldQty);
                for (int day = startDay; day <= endDay; day++) {
                    if (context.getStopDays().contains(day)) {
                        continue;
                    }
                    Long realDayProductionQty = Math.min(sumProductionQty, dayMaxProductionQty);
                    realSumProductionQty = realSumProductionQty + realDayProductionQty;
                    sumProductionQty = sumProductionQty - realDayProductionQty;
                    //todo 判断模具是否排产完毕
                    boolean isDayFinish = true;
                    Integer productionDay = day;
                    selectedDoubleList.forEach(productionMould -> productionMould.addProductionInfo(productionDay, isDayFinish, realDayProductionQty, dayMaxProductionQty, cxMachineCode, continueSkuPlanList));
                    if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                        break;
                    }
                }
                if (sumProductionQty <= BigDecimal.ZERO.longValue()) {
                    break;
                }
                mouldQty = mouldQty + ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            }
        });
        //在排产同规格同花纹


        //在排产共生胎同模具
    }

    /**
     * 根据续作模具数，得到共用模具的其他物料的高优先级产能量
     *
     * @param materialDesc        物料描述
     * @param continueProductInfo 续作Sku信息
     * @param shareMaterialDesc   共用模具的物料信息
     * @param productionPlanList  结构下的所有SKU计划
     * @return
     */
    private static Long getShareMouldOtherHeightQty(String materialDesc, CxContinueProductInfoHelper continueProductInfo, Set<String> shareMaterialDesc, List<MonthPlanProductionRequirePlanVo> productionPlanList) {
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
        if (maxCapacity <= continueSkuHeightProductionQty) {
            return continueSkuSumProductionQty;
        }
        //获得所有高优先级待排产量
        Long allHeightProductionQty = continueSkuHeightProductionQty + otherShareMouldHeightQty;
        //最大高优先级可排产量
        Long maxHeightProductionQty = Math.min(allHeightProductionQty, maxCapacity);
        //续作sku非高优先级最大可排产量
        Long noHeightMaxProductionQty = maxCapacity - maxHeightProductionQty;
        //续作Sku非高优先级排产量
        Long noHeightProductionQty = continueSkuSumProductionQty - continueSkuHeightProductionQty;
        Long realNoHeightProductionQty = Math.min(noHeightMaxProductionQty, noHeightProductionQty);
        return continueSkuHeightProductionQty + realNoHeightProductionQty;
    }

    /**
     * 从mouldList的模具关系中，挑选两副符合startDay~endDay可进行排产的模具
     *
     * @param context   排产上下文
     * @param mouldList SKU配置的所有模具关系
     * @param startDay  开始排产日--一般为前一个SKU的收尾日
     * @param endDay    结束排产日
     * @return
     */
    private static List<ProductionMouldInfoVo> selectedDoubleProductionMould(Context context, List<MonthPlanProductMouldInfoVo> mouldList, Integer startDay, Integer endDay) {
        List<ProductionMouldInfoVo> enableSelectedList = selectedEnableProductionMould(context, mouldList, startDay, endDay);
        if (CollectionUtils.isEmpty(enableSelectedList)) {
            return Collections.emptyList();
        }
        //双模排产
        if (enableSelectedList.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        //排序：关联数越小的优先
        enableSelectedList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue));
        //取得前两副
        return enableSelectedList.subList(BigDecimal.ZERO.intValue(), ProductionConstant.DOUBLE_MOULD_PRODUCTION);
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
        Map<String, ProductionMouldInfoVo> mouldInfoMap = productionContext.getMouldInfoMap();
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

}
