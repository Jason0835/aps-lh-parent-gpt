package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.MouldProductionLimitTypeEnum;
import com.zlt.aps.mp.engine.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMouldDayProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.enums.MouldRelationTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
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
     * @param context         排产上下文
     * @param productionStage 排产阶段
     * @param materialDesc    物料描述
     * @param mouldNumber     模具数
     * @return
     */
    public static List<ProductionMouldInfoVo> getContinueSkuMouldNumberInit(Context context, ProductionStageEnum productionStage, String materialDesc, Integer mouldNumber) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        if (StringUtils.isBlank(materialDesc) || null == mouldNumber || mouldNumber <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyList();
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptyList();
        }
        MonthPlanProductionRequirePlanVo productionPlan = productionContext.getAllSkuProductionPlan().get(materialDesc).get(BigDecimal.ZERO.intValue());
        List<ProductionMouldInfoVo> effectiveList = getEffectiveContinueRelation(baseDataContainer, skuRelationList);
        String groupName = productionPlan.getStructureName();
        Integer max = effectiveList.size();
        if (max < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.FIND_MOULD_LIMIT);
            return Collections.emptyList();
        }
        //20260116 得到模壳标准：理论只有一个模壳标准
        Integer mouldShellLimitQty = getMouldShellQty(productionContext, productionStage, groupName, materialDesc, effectiveList.get(BigDecimal.ZERO.intValue()));
        max = Math.min(max, mouldShellLimitQty);
        //20260117 获取模具分配比例
        Integer mouldAllocationLimitQty = getMouldAllocationQty(productionContext, productionStage, groupName, materialDesc, productionPlan);
        max = Math.min(max, mouldAllocationLimitQty);
        //20260119 获取胶囊卡盘的数量
        Integer capsuleChuckLimitQty = getCapsuleChuckQty(productionContext, productionStage, groupName, materialDesc, productionPlan);
        max = Math.min(max, capsuleChuckLimitQty);
        if (max < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
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
        if (CollectionUtils.isEmpty(effectiveList) || effectiveList.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        //20260306 按起始日排产量分组，优先挑选有排产量的模具
        List<ProductionMouldInfoVo> enableSelectedList = getDoubleByStartDay(effectiveList, materialDesc, startDay);
        if (CollectionUtils.isEmpty(enableSelectedList) || enableSelectedList.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        enableSelectedList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue).thenComparing(ProductionMouldInfoVo::getMouldCode, Comparator.reverseOrder()));
        return enableSelectedList.subList(BigDecimal.ZERO.intValue(), ProductionConstant.DOUBLE_MOULD_PRODUCTION);
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
     * 取得排产量一样的模具类别
     *
     * @param effectiveList 有效模具列表
     * @param materialDesc  排产物料
     * @param startDay      起始排产日
     * @return
     */
    private static List<ProductionMouldInfoVo> getDoubleByStartDay(List<ProductionMouldInfoVo> effectiveList, String materialDesc, Integer startDay) {
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        //优先挑选已经排产模具
        List<ProductionMouldInfoVo> hasProductSkuList = new ArrayList<>();
        effectiveList.forEach(singleMould -> {
            if(CollectionUtils.isEmpty(singleMould.getDayProductionInfo())){
                return ;
            }
            List<CxMouldDayProductionHelper> dayProductionList = singleMould.getDayProductionInfo().get(startDay);
            if (CollectionUtils.isEmpty(dayProductionList)) {
                return;
            }
            CxMouldDayProductionHelper lastProductionSku = dayProductionList.get(dayProductionList.size() - BigDecimal.ONE.intValue());
            if (!materialDesc.equals(lastProductionSku.getMaterialDesc())) {
                return;
            }
            hasProductSkuList.add(singleMould);
        });
        List<ProductionMouldInfoVo> selectList = null;
        if (!CollectionUtils.isEmpty(hasProductSkuList)) {
            selectList = getSameProductionQtyMould(hasProductSkuList, startDay);
        }
        if (!CollectionUtils.isEmpty(selectList)) {
            return selectList;
        }
        return getSameProductionQtyMould(effectiveList, startDay);
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

    /**
     * 取得模壳数量
     *
     * @param productionContext 排产上下文
     * @param productionStage   排产阶段
     * @param groupName         分组名(TBR结构)
     * @param materialDesc      物料描述
     * @param mouldInfo         模具信息
     * @return
     */
    private static Integer getMouldShellQty(TbrProductionContext productionContext,
                                            ProductionStageEnum productionStage,
                                            String groupName,
                                            String materialDesc,
                                            ProductionMouldInfoVo mouldInfo) {
        MouldShellBaseInfoVo mouldShellInfo = productionContext.getMouldShellInfo(mouldInfo);
        Integer mouldShellLimitQty;
        if (null == mouldShellInfo) {
            mouldShellLimitQty = BigDecimal.ZERO.intValue();
        } else {
            mouldShellLimitQty = mouldShellInfo.getLeftOverUsedQtyByContinueSku();
        }
        if (mouldShellLimitQty < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            log.info(TbrMouldProductionLogRecorder.addContinueSkuNoFindMouldLog(productionContext, productionStage, groupName, materialDesc, MouldProductionLimitTypeEnum.MOULD_SHELL_LIMIT));
        }
        return mouldShellLimitQty;
    }

    /**
     * 取得模具分配比例数量
     *
     * @param productionContext 排产上下文
     * @param productionStage   排产阶段
     * @param groupName         分组名(TBR结构)
     * @param materialDesc      物料描述
     * @param productionPlan    排产计划信息
     * @return
     */
    private static Integer getMouldAllocationQty(TbrProductionContext productionContext,
                                                 ProductionStageEnum productionStage,
                                                 String groupName,
                                                 String materialDesc,
                                                 MonthPlanProductionRequirePlanVo productionPlan) {
        Integer mouldAllocationLimitQty = productionContext.getMouldAllocationLimitQty(productionPlan);
        if (mouldAllocationLimitQty < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            log.info(TbrMouldProductionLogRecorder.addContinueSkuNoFindMouldLog(productionContext, productionStage, groupName, materialDesc, MouldProductionLimitTypeEnum.MOULD_ALLOCATION_LIMIT));
        }
        return mouldAllocationLimitQty;
    }

    /**
     * 取得胶囊卡盘数量
     *
     * @param productionContext 排产上下文
     * @param productionStage   排产阶段
     * @param groupName         分组名(TBR结构)
     * @param materialDesc      物料描述
     * @param productionPlan    排产计划信息
     * @return
     */
    private static Integer getCapsuleChuckQty(TbrProductionContext productionContext,
                                              ProductionStageEnum productionStage,
                                              String groupName,
                                              String materialDesc,
                                              MonthPlanProductionRequirePlanVo productionPlan) {
        Integer capsuleChuckLimitQty = productionContext.getCapsuleChuckLimitQty(productionPlan);
        if (capsuleChuckLimitQty < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            log.info(TbrMouldProductionLogRecorder.addContinueSkuNoFindMouldLog(productionContext, productionStage, groupName, materialDesc, MouldProductionLimitTypeEnum.CAPSULE_CHUCK_LIMIT));
        }
        return capsuleChuckLimitQty;
    }

    /**
     * 获取在startDay有相同排产量的模具列表
     *
     * @param mouldList 模具列表
     * @param startDay  startDay
     * @return
     */
    private static List<ProductionMouldInfoVo> getSameProductionQtyMould(List<ProductionMouldInfoVo> mouldList, Integer startDay) {
        if (CollectionUtils.isEmpty(mouldList) || null == startDay) {
            return Collections.emptyList();
        }
        Map<Integer, List<ProductionMouldInfoVo>> startDayGroup = new HashMap<>();
        mouldList.forEach(singleMould -> {
            if(CollectionUtils.isEmpty(singleMould.getDayProductionInfo())){
                return ;
            }
            List<CxMouldDayProductionHelper> dayProductionList = singleMould.getDayProductionInfo().get(startDay);
            if (CollectionUtils.isEmpty(dayProductionList)) {
                addGroup(startDayGroup, Integer.MAX_VALUE, singleMould);
                return;
            }
            Integer sumProductionQty = dayProductionList.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
            addGroup(startDayGroup, sumProductionQty, singleMould);
        });
        if(CollectionUtils.isEmpty(startDayGroup)){
            return Collections.emptyList();
        }
        Map<Integer, List<ProductionMouldInfoVo>> doubleMouldMap = new HashMap<>();
        startDayGroup.forEach((productionQty, enableMouldList) -> {
            if (CollectionUtils.isEmpty(enableMouldList)) {
                return;
            }
            if (enableMouldList.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                return;
            }
            doubleMouldMap.put(productionQty, enableMouldList);
        });
        if (CollectionUtils.isEmpty(doubleMouldMap)) {
            return Collections.emptyList();
        }
        List<Integer> keyList = new ArrayList<>(doubleMouldMap.keySet());
        keyList.sort(Comparator.comparing(Integer::intValue));
        return doubleMouldMap.get(keyList.get(BigDecimal.ZERO.intValue()));
    }

    /**
     * 根据key，从startDayGroup增加排产模具
     *
     * @param startDayGroup key的分组信息
     * @param key           分组key
     * @param singleMould   单模具信息
     */
    private static void addGroup(Map<Integer, List<ProductionMouldInfoVo>> startDayGroup, Integer key, ProductionMouldInfoVo singleMould) {
        if (null == key || null == singleMould) {
            return;
        }
        List<ProductionMouldInfoVo> enableMouldList = startDayGroup.get(key);
        if (null == enableMouldList) {
            enableMouldList = new ArrayList<>();
            startDayGroup.put(key, enableMouldList);
        }
        enableMouldList.add(singleMould);
    }
}
