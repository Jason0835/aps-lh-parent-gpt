package com.zlt.aps.factory.daylimit;

import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 某个工装类型的日排产信息对象
 *
 * @author ZLT
 * @date 20260120
 */
@Getter
public class WorkWeakTireDrumDayInfoHelper implements Serializable {
    /**
     * 鼓类型：01 成型鼓 02 胎体鼓 03 带束层鼓
     */
    private String workWearType;
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 鼓分组Id计划
     */
    private Set<String> groupIdSet;
    /**
     * 最大值
     */
    private Integer sumMaxLimitQty;
    /**
     * 当前使用量
     */
    private Integer sumUsedQty;
    /**
     * 已使用成型机台
     */
    private Set<String> sumUsedCxMachineSet;

    /**
     * 根据鼓分类的配置集合，构建对应的日排产集合信息
     *
     * @param limitGroupList
     * @return
     */
    public static List<WorkWeakTireDrumDayInfoHelper> buildSummery(List<TireDrumInfoVo> limitGroupList) {
        if (CollectionUtils.isEmpty(limitGroupList)) {
            return Collections.emptyList();
        }
        Set<String> workWeakTypeSet = limitGroupList.stream().map(TireDrumInfoVo::getWorkWearType).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(workWeakTypeSet)) {
            return Collections.emptyList();
        }
        if (workWeakTypeSet.size() > BigDecimal.ONE.intValue()) {
            return Collections.emptyList();
        }
        Set<Integer> productionDaySet = new HashSet<>();
        List<TireDrumInfoVo> effectiveList = new ArrayList<>();
        limitGroupList.forEach(singleGroup -> {
            Map<Integer, TireDrumDayInfoHelper> dayLimitInfoMap = singleGroup.getDayLimitInfoMap();
            if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
                return;
            }
            effectiveList.add(singleGroup);
            productionDaySet.addAll(dayLimitInfoMap.keySet());
        });
        if (CollectionUtils.isEmpty(productionDaySet)) {
            return Collections.emptyList();
        }
        Set<String> groupIdSet = effectiveList.stream().map(TireDrumInfoVo::getGroupId).collect(Collectors.toSet());
        String workWeakType = effectiveList.get(BigDecimal.ZERO.intValue()).getWorkWearType();
        List<WorkWeakTireDrumDayInfoHelper> summeryWorkWeakList = new ArrayList<>();
        productionDaySet.forEach(productionDay -> {
            WorkWeakTireDrumDayInfoHelper workWeakDayInfo = new WorkWeakTireDrumDayInfoHelper(workWeakType, groupIdSet);
            workWeakDayInfo.productionDay = productionDay;
            summeryWorkWeakList.add(workWeakDayInfo);
            List<TireDrumDayInfoHelper> summaryList = getProductDayByGroupList(productionDay, effectiveList);
            if (CollectionUtils.isEmpty(summaryList)) {
                workWeakDayInfo.sumMaxLimitQty = BigDecimal.ZERO.intValue();
                workWeakDayInfo.sumUsedQty = BigDecimal.ZERO.intValue();
                workWeakDayInfo.sumUsedCxMachineSet = Collections.emptySet();
                return;
            }
            Set<String> usedCxMachineSet = new HashSet<>();
            summaryList.forEach(singleGroup -> usedCxMachineSet.addAll(singleGroup.getUsedCxMachineSet()));
            workWeakDayInfo.sumMaxLimitQty = summaryList.stream().mapToInt(TireDrumDayInfoHelper::getMaxLimitQty).sum();
            workWeakDayInfo.sumUsedQty = summaryList.stream().mapToInt(TireDrumDayInfoHelper::getUsedQty).sum();
            workWeakDayInfo.sumUsedCxMachineSet = usedCxMachineSet;
        });
        return summeryWorkWeakList;
    }

    /**
     * 获取剩余量
     *
     * @return
     */
    public Integer getLeftOverCount() {
        if (null == sumMaxLimitQty || sumMaxLimitQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        Integer usedQty;
        if (null == sumUsedQty || sumUsedQty <= BigDecimal.ZERO.intValue()) {
            usedQty = BigDecimal.ZERO.intValue();
        } else {
            usedQty = sumUsedQty;
        }
        return sumMaxLimitQty - usedQty;
    }

    /**
     * 从多个分组集合中提取某日的排产信息
     *
     * @param productionDay 排产日
     * @param effectiveList 有效的分组信息集合
     * @return
     */
    private static List<TireDrumDayInfoHelper> getProductDayByGroupList(Integer productionDay, List<TireDrumInfoVo> effectiveList) {
        if (null == productionDay || CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        List<TireDrumDayInfoHelper> summaryList = new ArrayList<>();
        effectiveList.forEach(singleGroup -> {
            Map<Integer, TireDrumDayInfoHelper> dayLimitInfoMap = singleGroup.getDayLimitInfoMap();
            if (CollectionUtils.isEmpty(dayLimitInfoMap)) {
                return;
            }
            summaryList.add(dayLimitInfoMap.get(productionDay));
        });
        return summaryList;
    }

    /**
     * 构造函数
     *
     * @param workWearType
     * @param groupIdSet
     */
    public WorkWeakTireDrumDayInfoHelper(String workWearType, Set<String> groupIdSet) {
        this.workWearType = workWearType;
        this.groupIdSet = groupIdSet;
    }
}
