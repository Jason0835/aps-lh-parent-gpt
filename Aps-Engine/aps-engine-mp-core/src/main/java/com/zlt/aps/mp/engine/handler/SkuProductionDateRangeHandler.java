package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.enums.SkuDayMoldTypeEnum;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SkuNeedProductionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sku排产日范围业务处理器
 *
 * @author ZLT
 * @date 20260526
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkuProductionDateRangeHandler {

    /**
     * 获取分组(TBR-指结构)计划在分配段dayProductionLimitInfo，在即将排产selectedSku时
     * 判断selectedSku是否已经执行了降膜排产，如果执行了降膜排产，则从降膜日开始进行衔接
     *
     * @param context                排产上下文
     * @param groupPlanInfo          分组对象
     * @param selectedSku            当前选中需要排产的Sku
     * @param dayProductionLimitInfo 分组对象要排产的分配段日排产限制信息(已经转化)
     * @param startDay               开始日
     * @param endDay                 结束日
     * @return
     */
    public Set<Integer> getSelectedSkuNextProductionDateRange(Context context, ProductionPlanGroupInfo groupPlanInfo, SkuNeedProductionInfo selectedSku, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo, Integer startDay, Integer endDay) {
        if (!isEffectiveParam(groupPlanInfo, selectedSku, dayProductionLimitInfo)) {
            return Collections.emptySet();
        }
        if (null == startDay || null == endDay || startDay > endDay) {
            return Collections.emptySet();
        }
        String materialDesc = selectedSku.getMaterialDesc();
        List<MouldDayUsedNumber> plannedDayList = getDayMoldUsedInfo(dayProductionLimitInfo, materialDesc);
        if (CollectionUtils.isEmpty(plannedDayList)) {
            return Collections.emptySet();
        }
        List<MouldDayUsedNumber> rangeList = plannedDayList.stream().filter(singleDay -> singleDay.isRange(startDay, endDay)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(rangeList)) {
            return Collections.emptySet();
        }
        if (rangeList.size() == BigDecimal.ONE.intValue()) {
            return Collections.emptySet();
        }
        return getEarliestReducedMoldRange(rangeList);
    }

    /**
     * 是否为有效参数
     * 参数不可为空
     *
     * @param groupPlanInfo
     * @param selectedSku
     * @param dayProductionLimitInfo
     * @return
     */
    private boolean isEffectiveParam(ProductionPlanGroupInfo groupPlanInfo, SkuNeedProductionInfo selectedSku, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo) {
        if (null == groupPlanInfo || null == selectedSku || CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return false;
        }
        String groupName = selectedSku.getGroupName();
        if (StringUtils.isBlank(selectedSku.getMaterialDesc()) || StringUtils.isBlank(groupName)) {
            return false;
        }
        if (!groupName.equals(groupPlanInfo.getGroupName())) {
            return false;
        }
        return true;
    }

    /**
     * Sku日排产模具数
     *
     * @param dayProductionLimitInfo 日排产信息集合
     * @param materialDesc           排产Sku信息
     * @return
     */
    private List<MouldDayUsedNumber> getDayMoldUsedInfo(Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo, String materialDesc) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || StringUtils.isBlank(materialDesc)) {
            return Collections.emptyList();
        }
        List<MouldDayUsedNumber> skuDayUsedMoldInfo = Lists.newArrayList();
        dayProductionLimitInfo.forEach((productionDay, productionLimitInfo) -> {
            Map<String, Set<String>> skuProductionMouldMap = productionLimitInfo.getSkuProductionMouldMap();
            if (CollectionUtils.isEmpty(skuProductionMouldMap)) {
                return;
            }
            Set<String> productionMoldSet = skuProductionMouldMap.get(materialDesc);
            if (CollectionUtils.isEmpty(productionMoldSet)) {
                return;
            }
            //sku-日排产模具数
            skuDayUsedMoldInfo.add(new MouldDayUsedNumber(productionDay, productionMoldSet.size()));
        });
        if (CollectionUtils.isEmpty(skuDayUsedMoldInfo)) {
            return Collections.emptyList();
        }
        return skuDayUsedMoldInfo;
    }

    /**
     * 获取最早降模的排产日范围
     * 从首次降模~再次增模日之间
     * 如果没有再次增模，则一直到最后
     *
     * @param dayUsedMoldInfo Sku日使用模具信息
     * @return
     */
    private Set<Integer> getEarliestReducedMoldRange(List<MouldDayUsedNumber> dayUsedMoldInfo) {
        Integer minSize = BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue();
        if (CollectionUtils.isEmpty(dayUsedMoldInfo) || dayUsedMoldInfo.size() < minSize) {
            return Collections.emptySet();
        }
        //按日期从小到大排序
        dayUsedMoldInfo.sort(Comparator.comparing(MouldDayUsedNumber::getProductionDay));
        Set<Integer> rangeSet = Sets.newHashSet();
        Integer firstAddMoldDay = null;
        int index = BigDecimal.ZERO.intValue();
        int endIndex = dayUsedMoldInfo.size() - BigDecimal.ONE.intValue();
        for (MouldDayUsedNumber singleDayInfo : dayUsedMoldInfo) {
            int currentIndex = index;
            index = index + BigDecimal.ONE.intValue();
            //第一天，跳过
            if (currentIndex == BigDecimal.ZERO.intValue()) {
                continue;
            }
            //已经确定范围
            if (!CollectionUtils.isEmpty(rangeSet) && null != firstAddMoldDay) {
                break;
            }
            MouldDayUsedNumber beforeDayInfo = getBeforeDayInfo(dayUsedMoldInfo, currentIndex);
            if (null == beforeDayInfo) {
                break;
            }
            SkuDayMoldTypeEnum type = getSkuDayMoldType(beforeDayInfo, singleDayInfo);
            //首个降模
            if (CollectionUtils.isEmpty(rangeSet) && null == firstAddMoldDay && SkuDayMoldTypeEnum.REDUCED_MOLD == type) {
                rangeSet.add(singleDayInfo.getProductionDay());
                continue;
            }
            if (!CollectionUtils.isEmpty(rangeSet) && null == firstAddMoldDay) {
                //降模后，首个增模
                if (SkuDayMoldTypeEnum.ADD_MOLD == type) {
                    firstAddMoldDay = singleDayInfo.getProductionDay();
                    break;
                }
                rangeSet.add(singleDayInfo.getProductionDay());
            }
        }
        if (CollectionUtils.isEmpty(rangeSet)) {
            return Collections.emptySet();
        }
        return rangeSet;
    }

    /**
     * 获取前一天的信息
     * 前提：对dayUsedMoldInfo已经按日期从小到大排序
     *
     * @param dayUsedMoldInfo 天排产信息集合(需要按日期排序)
     * @param currentIndex    当前下标
     * @return
     */
    private MouldDayUsedNumber getBeforeDayInfo(List<MouldDayUsedNumber> dayUsedMoldInfo, int currentIndex) {
        int maxIndex = dayUsedMoldInfo.size() - BigDecimal.ONE.intValue();
        if (currentIndex <= BigDecimal.ZERO.intValue() || currentIndex > maxIndex) {
            return null;
        }
        return dayUsedMoldInfo.get(currentIndex - BigDecimal.ONE.intValue());
    }

    /**
     * 找从beforeDay到currentDay进行降膜
     *
     * @param beforeDayInfo  前一个日
     * @param currentDayInfo 后一日
     * @return 类型 1 降模 2 增模
     */
    private SkuDayMoldTypeEnum getSkuDayMoldType(MouldDayUsedNumber beforeDayInfo, MouldDayUsedNumber currentDayInfo) {
        if (null == beforeDayInfo || null == currentDayInfo) {
            return null;
        }
        Integer beforeDay = beforeDayInfo.getProductionDay();
        Integer currentDay = currentDayInfo.getProductionDay();
        if (null == beforeDay || null == currentDay || beforeDay >= currentDay) {
            return null;
        }
        Integer beforeUsedLhMachine = beforeDayInfo.getUsedLhMachineCount();
        Integer currentUsedLhMachine = currentDayInfo.getUsedLhMachineCount();
        if (beforeUsedLhMachine > currentUsedLhMachine) {
            return SkuDayMoldTypeEnum.REDUCED_MOLD;
        }
        if (beforeUsedLhMachine < currentUsedLhMachine) {
            return SkuDayMoldTypeEnum.ADD_MOLD;
        }
        return null;
    }
}
