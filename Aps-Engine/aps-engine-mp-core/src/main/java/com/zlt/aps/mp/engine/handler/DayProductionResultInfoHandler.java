package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 日排产结果信息处理器
 *
 * @author ZLT
 * @date 20260420
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayProductionResultInfoHandler {

    /**
     * 获取指定分组名+主花纹指定排产日
     * 的不同结构模具分配比例下实际排产模具数信息
     * 根据不同分组的花纹分配比例信息，只获取该部分的实际
     * 续作排产使用模具数，并按分组名+主花纹为key，
     * 得到分组名+主花纹下各续作Sku使用的排产模具数
     *
     * @param context       排产上下文
     * @param productionDay 排产日
     * @return key = 分组名+主花纹：value = {key = Sku描述：value = 排产模具信息}
     */
    public Map<String, Map<String, SkuDayUsedMoldInfoHelper>> getFirstDayProductionMoldNumberByGroupPattern(Context context, Integer productionDay) {
        //有效排产日
        if (!context.isEffectiveProductionDay(productionDay)) {
            return Collections.emptyMap();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //分组信息存在
        Map<String, ProductionPlanGroupInfo> allGroupInfo = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupInfo)) {
            return Collections.emptyMap();
        }
        //分组+主花纹模具配比存在
        Map<String, MouldAllocationInfoVo> groupMainPatternMap = productionContext.getBaseDataContainer().getGroupMainPatternAllocationLimitMap();
        if (CollectionUtils.isEmpty(groupMainPatternMap)) {
            return Collections.emptyMap();
        }
        //构建各分组+主花纹在productionDay各续作Sku使用的模具信息
        Map<String, Map<String, SkuDayUsedMoldInfoHelper>> groupMainPatternUsedMap = Maps.newHashMap();
        groupMainPatternMap.forEach((groupMainPattern, singleAllocationInfo) -> {
            String groupName = singleAllocationInfo.getStructureName();
            ProductionPlanGroupInfo groupInfo = allGroupInfo.get(groupName);
            if (null == groupInfo) {
                groupMainPatternUsedMap.put(groupMainPattern, Collections.emptyMap());
                return;
            }
            Map<String, SkuDayUsedMoldInfoHelper> singleUsedMap = groupMainPatternUsedMap.get(groupMainPattern);
            if (null == singleUsedMap) {
                singleUsedMap = Maps.newHashMap();
            }
            Map<String, SkuDayUsedMoldInfoHelper> finalSingleUsedMap = singleUsedMap;
            addMainPatternUsedMoldNumberByProductionDay(productionContext, finalSingleUsedMap, groupInfo, groupMainPattern, productionDay);
            groupMainPatternUsedMap.put(groupMainPattern, singleUsedMap);
        });
        return groupMainPatternUsedMap;
    }

    /**
     * 获取超出主花纹 maxMouldNumber的排产日及对应排产Sku信息
     *
     * @param context        排产上下文
     * @param groupName      分组名
     * @param mainPattern    主花纹
     * @param maxMouldNumber 最大模具数
     * @return
     */
    public List<ContinueSkuDayUsedMouldInfoHelper> getGreaterThanAllocationMouldNumberByGroupPattern(Context context, String groupName, String mainPattern, Integer maxMouldNumber) {
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(mainPattern) || null == maxMouldNumber) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //分组信息存在
        Map<String, ProductionPlanGroupInfo> allGroupInfo = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupInfo)) {
            return Collections.emptyList();
        }
        ProductionPlanGroupInfo groupInfo = allGroupInfo.get(groupName);
        if (null == groupInfo) {
            return Collections.emptyList();
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyList();
        }
        List<ContinueSkuDayUsedMouldInfoHelper> allDayInfoList = Lists.newArrayList();
        //有排产信息
        dayProductionLimitInfo.forEach((productionDay, dayProductionInfo) -> {
            //有排产Sku明细信息
            Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = dayProductionInfo.getSkuProductionDetailInfo();
            if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
                return;
            }
            Map<String, SkuDayUsedMoldInfoHelper> singleDayUsedMap = Maps.newHashMap();
            skuProductionDetailInfo.forEach((materialDesc, dayDetailInfo) -> {
                if (CollectionUtils.isEmpty(dayDetailInfo)) {
                    return;
                }
                //结构+主花纹 需一致
                SkuDayProductionInfoHelper productionSkuInfo = dayDetailInfo.get(BigDecimal.ZERO.intValue());
                if (!(groupName.equals(productionSkuInfo.getGroupName()) && mainPattern.equals(productionSkuInfo.getMainPattern()))) {
                    return;
                }
                SkuDayUsedMoldInfoHelper singleSkuUsed = singleDayUsedMap.get(materialDesc);
                if (null == singleSkuUsed) {
                    singleSkuUsed = SkuDayUsedMoldInfoHelper.build(productionSkuInfo);
                }
                SkuDayUsedMoldInfoHelper finalUsed = singleSkuUsed;
                //累计使用模具数
                dayDetailInfo.forEach(singleUsedInfo -> finalUsed.addUsedMoldNumber(singleUsedInfo));
                singleDayUsedMap.put(materialDesc, finalUsed);
            });
            List<ContinueSkuDayUsedMouldInfoHelper> skuList = getDayContinueSkuUsedInfo(singleDayUsedMap);
            if (CollectionUtils.isEmpty(skuList)) {
                return;
            }
            Integer sumUsedMoldNumber = skuList.stream().mapToInt(ContinueSkuDayUsedMouldInfoHelper::getUsedMouldNumber).sum();
            if (sumUsedMoldNumber <= maxMouldNumber) {
                return;
            }
            allDayInfoList.addAll(skuList);
        });
        return allDayInfoList;
    }

    /**
     * 获取分组+主花纹在productionDay的使用模具数
     *
     * @param context          排产上下文
     * @param singleUsedMap    结构+花纹下的Sku排产信息
     * @param groupInfo        分组计划
     * @param groupMainPattern 分组+主花纹
     * @param productionDay    排产日
     * @return
     */
    private void addMainPatternUsedMoldNumberByProductionDay(Context context, Map<String, SkuDayUsedMoldInfoHelper> singleUsedMap, ProductionPlanGroupInfo groupInfo, String groupMainPattern, Integer productionDay) {
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupInfo.getDayProductionLimitInfo();
        //有排产信息
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return;
        }
        GroupPlanCxLhCapacityLimitHelper dayProductionInfo = dayProductionLimitInfo.get(productionDay);
        //有日排产信息
        if (null == dayProductionInfo) {
            return;
        }
        //有排产Sku明细信息
        Map<String, List<SkuDayProductionInfoHelper>> skuProductionDetailInfo = dayProductionInfo.getSkuProductionDetailInfo();
        if (CollectionUtils.isEmpty(skuProductionDetailInfo)) {
            return;
        }
        skuProductionDetailInfo.forEach((materialDesc, dayDetailInfo) -> {
            if (CollectionUtils.isEmpty(dayDetailInfo)) {
                return;
            }
            //结构+主花纹 需一致
            SkuDayProductionInfoHelper productionSkuInfo = dayDetailInfo.get(BigDecimal.ZERO.intValue());
            if (!groupMainPattern.equals(productionSkuInfo.getGroupMainPatternKey())) {
                return;
            }
            SkuDayUsedMoldInfoHelper singleSkuUsed = singleUsedMap.get(materialDesc);
            if (null == singleSkuUsed) {
                singleSkuUsed = SkuDayUsedMoldInfoHelper.build(productionSkuInfo);
            }
            SkuDayUsedMoldInfoHelper finalUsed = singleSkuUsed;
            //累计使用模具数
            dayDetailInfo.forEach(singleUsedInfo -> {
                finalUsed.addUsedMoldNumber(singleUsedInfo);
            });
            singleUsedMap.put(materialDesc, finalUsed);
        });
    }

    /**
     * 转化成日使用模具数信息对象集合
     *
     * @param singleDayUsedMap
     * @return
     */
    private List<ContinueSkuDayUsedMouldInfoHelper> getDayContinueSkuUsedInfo(Map<String, SkuDayUsedMoldInfoHelper> singleDayUsedMap) {
        if (CollectionUtils.isEmpty(singleDayUsedMap)) {
            return Collections.emptyList();
        }
        List<ContinueSkuDayUsedMouldInfoHelper> skuList = Lists.newArrayList();
        singleDayUsedMap.forEach((materialDesc, singleSkuInfo) -> {
            Integer moldNumber = singleSkuInfo.getUsedMoldNumber();
            String materialCode = singleSkuInfo.getMaterialCode();
            Integer productionDay = singleSkuInfo.getProductionDay();
            skuList.add(new ContinueSkuDayUsedMouldInfoHelper(materialDesc, materialCode, productionDay, moldNumber));
        });
        return skuList;
    }

}
