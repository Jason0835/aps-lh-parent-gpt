package com.zlt.aps.factory.handler;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.MouldRelationTypeEnum;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 规格-模具选择器
 *
 * @author ZLT
 * @date 20251221
 */
@Slf4j
public class SkuMouldSelector {

    /**
     * 获取选中模具信息
     *
     * @param context                   排产上下文
     * @param selectedMaterialDesc      选中的sku
     * @param earliestConclusionLhGroup 收尾硫化组
     * @param startDay                  排产开始日
     * @param endDay                    排产结束日
     * @return
     */
    public static List<ProductionMouldInfoVo> getSelectedMouldList(Context context, String selectedMaterialDesc, EarliestConclusionLhGroupHelper earliestConclusionLhGroup, Integer startDay, Integer endDay) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, List<MonthPlanProductMouldInfoVo>> allMouldInfo = productionContext.getBaseDataContainer().getSkuMouldRelationMap();
        List<MonthPlanProductMouldInfoVo> allMouldList = allMouldInfo.get(selectedMaterialDesc);
        Set<String> productionMouldSet = earliestConclusionLhGroup.getUsedMouldSet();
        List<MonthPlanProductMouldInfoVo> selectedMouldRelationList = new ArrayList<>();
        allMouldList.forEach(mouldRelationInfo -> {
            if (productionMouldSet.contains(mouldRelationInfo.getMouldCode())) {
                selectedMouldRelationList.add(mouldRelationInfo);
            }
        });
        //选中的模具
        return selectedEnableMouldByNumber(context, ProductionConstant.DOUBLE_MOULD_PRODUCTION, selectedMouldRelationList, startDay, endDay);
    }

    /**
     * 获取续作sku对应的模具信息，
     * 并按共用性差的在前，模具编号大的在前排序
     *
     * @param context      排产上下文
     * @param materialDesc 物料描述
     * @param mouldNumber  模具数
     * @return
     */
    public static List<ProductionMouldInfoVo> getContinueSkuMouldNumberInit(Context context, String materialDesc, Integer mouldNumber) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        if (StringUtils.isBlank(materialDesc) || null == mouldNumber || mouldNumber <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyList();
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptyList();
        }
        List<ProductionMouldInfoVo> effectiveList = getEffectiveContinueRelation(baseDataContainer, skuRelationList);
        Integer max = effectiveList.size();
        if (max < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        //20260116 得到模壳标准：理论只有一个模壳标准
        MouldShellBaseInfoVo mouldShellInfo = productionContext.getMouldShellInfo(effectiveList.get(BigDecimal.ZERO.intValue()));
        Integer leftOverUsedQty = mouldShellInfo.getLeftOverUsedQtyByContinueSku();
        max = Math.min(max, leftOverUsedQty);
        //20260117 获取模具分配比例：理论最大
        MonthPlanProductionRequirePlanVo productionPlan = productionContext.getAllSkuProductionPlan().get(materialDesc).get(BigDecimal.ZERO.intValue());
        Integer limitQty = productionContext.getMouldAllocationLimitQty(productionPlan);
        max = Math.min(max, limitQty);
        effectiveList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue)
                .thenComparing(ProductionMouldInfoVo::getLeftOverCapacity)
                .thenComparing(ProductionMouldInfoVo::getMouldCode, Comparator.reverseOrder()));
        if (max >= mouldNumber) {
            return effectiveList.subList(BigDecimal.ZERO.intValue(), mouldNumber);
        }
        return effectiveList;
    }

    /**
     * 获取materialDesc在startDay~endDay范围内可排产的两副模具
     * 在多幅的情形下，共用性差的优先，否则编号大的优先
     *
     * @param materialDesc 物料描述
     * @param startDay     排产开始日
     * @param endDay       排产结束日
     * @return
     */
    public static List<ProductionMouldInfoVo> selectedDoubleMouldByRange(Context context, String materialDesc, Integer startDay, Integer endDay) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        if (StringUtils.isBlank(materialDesc) || null == startDay || null == endDay || startDay > endDay) {
            return Collections.emptyList();
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptyList();
        }
        List<ProductionMouldInfoVo> effectiveList = getEffectiveByRange(baseDataContainer, skuRelationList, startDay, endDay);
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        if (effectiveList.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        effectiveList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue).thenComparing(ProductionMouldInfoVo::getMouldCode, Comparator.reverseOrder()));
        return effectiveList.subList(BigDecimal.ZERO.intValue(), ProductionConstant.DOUBLE_MOULD_PRODUCTION);
    }

    /**
     * 根据模具关系，获取在startDay~endDay有效排产的模具信息
     *
     * @param baseDataContainer 基础数据配置容器
     * @param skuRelationList   配置的模具关系
     * @param startDay          排产开始日
     * @param endDay            排产结束日
     * @return
     */
    private static List<ProductionMouldInfoVo> getEffectiveByRange(BaseDataContainer baseDataContainer, List<MonthPlanProductMouldInfoVo> skuRelationList, Integer startDay, Integer endDay) {
        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = baseDataContainer.getMouldInfoMap().get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            if (!mouldInfo.isProduction(startDay, endDay)) {
                return;
            }
            effectiveList.add(mouldInfo);
        });
        return effectiveList;
    }

    /**
     * 根据模具关系获取续作模具关系信息
     * 排除新模具到货计划的模具关系
     *
     * @param baseDataContainer 基础数据配置容器
     * @param skuRelationList   配置的模具关系
     * @return
     */
    private static List<ProductionMouldInfoVo> getEffectiveContinueRelation(BaseDataContainer baseDataContainer, List<MonthPlanProductMouldInfoVo> skuRelationList) {
        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = baseDataContainer.getMouldInfoMap().get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            //排除不是模具关系的数据
            if (MouldRelationTypeEnum.SKU_RELATION_CONFIGURATION != mouldInfo.getRelationType()) {
                return;
            }
            effectiveList.add(mouldInfo);
        });
        return effectiveList;
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
}
