package com.zlt.aps.factory.handler;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 规格-模具选择器
 *
 * @author ZLT
 * @date 20251221
 */
@Slf4j
public class SkuMouldSelector {

    /**
     * 获取续作sku对应的模具信息，并按共用性差的在前，模具编号大的在前
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
        List<ProductionMouldInfoVo> effectiveList = getEffectiveInit(baseDataContainer, skuRelationList);
        Integer max = effectiveList.size();
        if (max < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        effectiveList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue).thenComparing(ProductionMouldInfoVo::getMouldCode, Comparator.reverseOrder()));
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
     * 根据模具关系，获取在startDay~endDay有效排产的模具信息
     *
     * @param baseDataContainer 基础数据配置容器
     * @param skuRelationList   配置的模具关系
     * @return
     */
    private static List<ProductionMouldInfoVo> getEffectiveInit(BaseDataContainer baseDataContainer, List<MonthPlanProductMouldInfoVo> skuRelationList) {
        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = baseDataContainer.getMouldInfoMap().get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            effectiveList.add(mouldInfo);
        });
        return effectiveList;
    }
}
