package com.zlt.aps.mp.engine.handler.appoint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 在机结构强制下机，Sku日减硫化机台优先级选择器
 *
 * @author ZLT
 * @date 20260810
 */
@Slf4j
@Component
public class DayReduceSkuLhMachinePrioritySelector {

    /**
     * 构建胎胚强制下机优先级对象
     *
     * @param embryoCodeGroup
     * @return
     */
    public List<DayReduceEmbryoPriorityInfo> buildPriorityByEmbryo(Map<String, List<ContinueSkuDayUsedInfo>> embryoCodeGroup) {
        if (CollectionUtils.isEmpty(embryoCodeGroup)) {
            return Collections.emptyList();
        }
        Map<String, DayReduceEmbryoPriorityInfo> embryoCodePriorityMap = Maps.newHashMap();
        embryoCodeGroup.forEach((embryoCode, details) -> {
            if (CollectionUtils.isEmpty(details)) {
                return;
            }
            Integer usedLhMachines = details.stream().mapToInt(ContinueSkuDayUsedInfo::getLeftOverUsedLhMachine).sum();
            if (usedLhMachines > BigDecimal.ZERO.intValue()) {
                return;
            }
            DayReduceEmbryoPriorityInfo priorityInfo = new DayReduceEmbryoPriorityInfo(embryoCode, usedLhMachines, details);
            embryoCodePriorityMap.put(embryoCode, priorityInfo);
        });
        if (CollectionUtils.isEmpty(embryoCodePriorityMap)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(embryoCodePriorityMap.values());
    }

    /**
     * 构建Sku需要降膜下机优先级对象
     *
     * @param context
     * @param groupInfo
     * @param originHighQtyMap
     * @param skuInfoList
     * @return
     */
    public List<DayReduceLhMachinePriorityInfo> getPriorityBySku(Context context, ProductionPlanGroupInfo groupInfo, Map<String, Integer> originHighQtyMap, List<ContinueSkuDayUsedInfo> skuInfoList) {
        if (CollectionUtils.isEmpty(skuInfoList)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Map<String, DayReduceLhMachinePriorityInfo> priorityResultMap = Maps.newHashMap();
        Set<String> oemInfo = Optional.ofNullable(groupInfo.getOemBrandSet()).orElse(Collections.emptySet());
        skuInfoList.forEach(singleSku -> {
            String materialDesc = singleSku.getMaterialDesc();
            List<MonthPlanProductionRequirePlanVo> planList = productionContext.getInitProductionInfoByPlan(materialDesc);
            if (CollectionUtils.isEmpty(planList)) {
                return;
            }
            //构建优先级对象
            Integer sumQty = planList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginProductionQty).sum();
            MonthPlanProductionRequirePlanVo baseSkuInfo = planList.get(BigDecimal.ZERO.intValue());
            Integer productionDay = singleSku.getProductionDay();
            Integer endDay = productionDay - BigDecimal.ONE.intValue();
            //是否长荣品牌
            String brand = baseSkuInfo.getBrand();
            boolean isOemSku = oemInfo.contains(brand);
            Integer sumProductionQty = baseDataContainer.getSumProductionQty(materialDesc, endDay);
            //库销比
            double inventorySalesRatio = baseSkuInfo.getInventorySalesRatioByAddProductionQty(sumProductionQty);
            Integer originHighQty = originHighQtyMap.get(materialDesc);
            if (null == originHighQty) {
                originHighQty = BigDecimal.ZERO.intValue();
            }
            //高优先级
            boolean hasHighPriority = originHighQty > sumProductionQty;
            //还需排产量
            Integer needProductionQty;
            if (YesOrNoEnum.YES.getValue().equals(baseSkuInfo.getIsPriorityHeight())) {
                //高优先排产
                needProductionQty = originHighQty - sumProductionQty;
            } else {

                needProductionQty = sumQty - sumProductionQty;
            }
            if (needProductionQty < BigDecimal.ZERO.intValue()) {
                needProductionQty = BigDecimal.ZERO.intValue();
            }
            boolean hasMoldCapacityLimit = baseDataContainer.hasMoldCapacityLimit(materialDesc);
            String embryoCode = singleSku.getEmbryoCode();
            String materialCode = singleSku.getMaterialCode();
            DayReduceLhMachinePriorityInfo skuPriority = new DayReduceLhMachinePriorityInfo(materialDesc, materialCode, embryoCode, isOemSku, hasMoldCapacityLimit, hasHighPriority, needProductionQty, inventorySalesRatio);
            priorityResultMap.put(materialDesc, skuPriority);
        });
        if (CollectionUtils.isEmpty(priorityResultMap)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(priorityResultMap.values());
    }
}
