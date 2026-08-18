package com.zlt.aps.mp.engine.handler.embryobalance;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎胚平衡检查业务
 * 日胎胚使用硫化机台信息
 *
 * @author ZLT
 * @date 20260814
 */
@Getter
public class DayEmbryoUsedInfo implements Serializable {
    /**
     * 排产日
     */
    private Integer day;
    /**
     * 分组名：TBR-结构
     */
    private String groupName;
    /**
     * 额外处理机台信息
     */
    private Integer extraLhMachines;
    /**
     * 总使用硫化机台数
     */
    private Integer sumUsedLhMachines;
    /**
     * 分配的机台信息
     */
    private List<GroupCxMachineConfiguration> cxMachineConfigurationList;
    /**
     * 胎胚使用硫化机台信息
     */
    private List<EmbryoUsedLhMachineInfo> embryoUsedInfo;

    /**
     * 构造函数
     *
     * @param day                        排产日信息
     * @param groupName                  结构名
     * @param sumUsedLhMachines          当前使用总硫化机台数
     * @param extraLhMachines            额外处理 多台+或是减
     * @param cxMachineConfigurationList 成型结构硫化配比信息
     * @param embryoUsedInfo             胎胚使用硫化机台数信息
     */
    public DayEmbryoUsedInfo(Integer day,
                             String groupName,
                             Integer extraLhMachines,
                             Integer sumUsedLhMachines,
                             List<GroupCxMachineConfiguration> cxMachineConfigurationList,
                             List<EmbryoUsedLhMachineInfo> embryoUsedInfo) {
        this.day = day;
        this.groupName = groupName;
        this.extraLhMachines = extraLhMachines;
        this.sumUsedLhMachines = sumUsedLhMachines;
        this.cxMachineConfigurationList = cxMachineConfigurationList;
        this.embryoUsedInfo = embryoUsedInfo;
    }

    /**
     * 校验是否可平衡分布到多台成型机上
     * true 表示可以
     * false 表示不可以
     *
     * @return
     */
    public boolean checkIsBalanceAllocation() {
        if (null == day) {
            return false;
        }
        if (CollectionUtils.isEmpty(cxMachineConfigurationList)) {
            return false;
        }
        Integer dayMaxLhMachines = cxMachineConfigurationList.stream().mapToInt(GroupCxMachineConfiguration::getRealMaxLhMachines).sum();
        if (null != extraLhMachines) {
            dayMaxLhMachines = dayMaxLhMachines + extraLhMachines;
        }
        if (sumUsedLhMachines > dayMaxLhMachines) {
            return false;
        }
        if (CollectionUtils.isEmpty(embryoUsedInfo)) {
            return true;
        }
        Set<String> cxMachineCodeSet = cxMachineConfigurationList.stream().map(GroupCxMachineConfiguration::getCxMachineCode).collect(Collectors.toSet());
        if (cxMachineCodeSet.size() == BigDecimal.ONE.intValue()) {
            return checkSingleCxMachine();
        }
        //多机台
        return checkMultipleCxMachine();
    }

    /**
     * 单机台
     *
     * @return
     */
    private boolean checkSingleCxMachine() {
        Set<String> embryoCodeSet = embryoUsedInfo.stream().map(EmbryoUsedLhMachineInfo::getEmbryoCode).collect(Collectors.toSet());
        Integer embryoCodeCount = embryoCodeSet.size();
        Integer maxEmbryoCodeCount = cxMachineConfigurationList.get(BigDecimal.ZERO.intValue()).getMaxEmbryoCodeCount();
        return embryoCodeCount <= maxEmbryoCodeCount;
    }

    /**
     * 多机台
     *
     * @return
     */
    private boolean checkMultipleCxMachine() {
        //先判定是否有指定
        boolean isProductionAppointEmbryo = isAppointEmbryoProduction();
        if (isProductionAppointEmbryo) {
            //有排产指定机台胎胚
            return checkAppointMultipleCxMachine();
        }
        //没有指定
        return checkNoAppointMultipleCxMachine(sumUsedLhMachines, extraLhMachines, embryoUsedInfo, cxMachineConfigurationList);
    }

    /**
     * 有胎胚指定机台时
     * 先将指定机台分满
     *
     * @return
     */
    private boolean checkAppointMultipleCxMachine() {
        //获取有指定机台的胎胚机台信息
        Map<String, GroupCxMachineConfiguration> appointEmbryoInfoMap = getEmbryoAppointInfo();
        Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> sameAppointMap = getSameCxMachineAppoint(appointEmbryoInfoMap);
        if (CollectionUtils.isEmpty(sameAppointMap)) {
            return checkNoAppointMultipleCxMachine(sumUsedLhMachines, extraLhMachines, embryoUsedInfo, cxMachineConfigurationList);
        }
        //获取除指定机台胎胚外其它剩余待分配胎胚信息
        List<EmbryoUsedLhMachineInfo> leftOverList = getLeftOverEmbryoByAppoint(sameAppointMap);
        if (CollectionUtils.isEmpty(leftOverList)) {
            return checkAllAppointBalance(sameAppointMap);
        }
        return extractMaxLhMachinesAndCheckLeftOver(cxMachineConfigurationList, leftOverList, sameAppointMap);
    }

    /**
     * 如果提取指定机台的胎胚总使用硫化机台数为最大
     * 则需要将最大硫化机台数指定机台分配满，再对剩余的进行下一循环分配
     *
     * @param cxMachineConfigurationList 所有机台信息
     * @param leftOverList               剩余待分配胎胚信息
     * @param sameAppointMap             同指定机台分配信息
     * @return
     */
    private boolean extractMaxLhMachinesAndCheckLeftOver(List<GroupCxMachineConfiguration> cxMachineConfigurationList, List<EmbryoUsedLhMachineInfo> leftOverList, Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> sameAppointMap) {
        if (CollectionUtils.isEmpty(leftOverList) || CollectionUtils.isEmpty(cxMachineConfigurationList) && CollectionUtils.isEmpty(sameAppointMap)) {
            return true;
        }
        //获取剩余指定未分配完成的
        Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> leftOverNoFinishAppointMap = Maps.newHashMap();
        sameAppointMap.forEach((singleCxMachineInfo, appointInfoList) -> {
            if (Boolean.TRUE.equals(singleCxMachineInfo.getAllocationFinish())) {
                return;
            }
            leftOverNoFinishAppointMap.put(singleCxMachineInfo, appointInfoList);
        });
        //指定胎胚机台已经分配完成
        if (CollectionUtils.isEmpty(leftOverNoFinishAppointMap)) {
            //获取剩余未指定的机台
            List<GroupCxMachineConfiguration> leftOverNoAppointList = Lists.newArrayList();
            cxMachineConfigurationList.forEach(singleCxMachineInfo -> {
                if (sameAppointMap.containsKey(singleCxMachineInfo)) {
                    return;
                }
                leftOverNoAppointList.add(singleCxMachineInfo);
            });
            if (CollectionUtils.isEmpty(leftOverNoAppointList)) {
                //还有剩余胎胚：
                return false;
            }
            //验证非指定是否可分配
            Integer sumUsedLhMachines = leftOverList.stream().mapToInt(EmbryoUsedLhMachineInfo::getUsedLhMachines).sum();
            return checkNoAppointMultipleCxMachine(sumUsedLhMachines, BigDecimal.ZERO.intValue(), leftOverList, leftOverNoAppointList);
        }
        //获取剩余待分配胎胚的最大值
        leftOverList.sort(Comparator.comparing(EmbryoUsedLhMachineInfo::getUsedLhMachines, Comparator.reverseOrder()));
        Integer leftOverMaxLhMachines = leftOverList.get(BigDecimal.ZERO.intValue()).getUsedLhMachines();
        //判断指定机台所有指定胎胚是否为使用机台最大，不是则直接返回true
        boolean isNeedGroup = isNeedAllocationByAppoint(leftOverMaxLhMachines, leftOverNoFinishAppointMap);
        if (!isNeedGroup) {
            return true;
        }
        //对使用硫化机台数最多的指定机台先分配满
        leftOverNoFinishAppointMap.forEach((singleCxMachineInfo, appointInfoList) -> singleCxMachineInfo.setGroupAllocation(appointInfoList));
        List<GroupCxMachineConfiguration> appointList = Lists.newArrayList(sameAppointMap.keySet());
        appointList.sort(Comparator.comparing(GroupCxMachineConfiguration::getAllocationLhMachines, Comparator.reverseOrder()));
        GroupCxMachineConfiguration maxAppointGroup = appointList.get(BigDecimal.ZERO.intValue());
        List<EmbryoUsedLhMachineInfo> leftOverByFullAppoint = getLeftOverEmbryoUsedInfoAndFullAppoint(maxAppointGroup, leftOverList);
        return extractMaxLhMachinesAndCheckLeftOver(cxMachineConfigurationList, leftOverByFullAppoint, sameAppointMap);
    }

    /**
     * 验证没有指定情形下，多台成型机台能否平衡分配
     * true 表示可分配 false 表示不可分配
     *
     * @param sumUsedLhMachines          总机台数
     * @param extraLhMachines            额外机台数
     * @param embryoUsedInfo             胎胚硫化机台数
     * @param cxMachineConfigurationList 机台配比配置
     * @return
     */
    private boolean checkNoAppointMultipleCxMachine(Integer sumUsedLhMachines, Integer extraLhMachines, List<EmbryoUsedLhMachineInfo> embryoUsedInfo, List<GroupCxMachineConfiguration> cxMachineConfigurationList) {
        if (CollectionUtils.isEmpty(cxMachineConfigurationList)) {
            return false;
        }
        if (CollectionUtils.isEmpty(embryoUsedInfo)) {
            return true;
        }
        Integer maxEmbryoCodeCount = cxMachineConfigurationList.stream().mapToInt(GroupCxMachineConfiguration::getMaxEmbryoCodeCount).sum();
        Integer maxLhMachines = cxMachineConfigurationList.stream().mapToInt(GroupCxMachineConfiguration::getRealMaxLhMachines).sum();
        if (null != extraLhMachines) {
            maxLhMachines = maxLhMachines + extraLhMachines;
        }
        Set<String> embryoCodeSet = embryoUsedInfo.stream().map(EmbryoUsedLhMachineInfo::getEmbryoCode).collect(Collectors.toSet());
        Integer embryoCodeCount = embryoCodeSet.size();
        if (cxMachineConfigurationList.size() == BigDecimal.ONE.intValue()) {
            //只剩一台时
            return embryoCodeCount <= maxEmbryoCodeCount && sumUsedLhMachines <= maxLhMachines;
        }
        //多台时，胎胚种类数超
        if (embryoCodeCount > maxEmbryoCodeCount) {
            return false;
        }
        //胎胚未满
        if (embryoCodeCount < maxEmbryoCodeCount) {
            return true;
        }
        //胎胚刚好满了
        return checkNoAppointFullEmbryoCountBalance(extraLhMachines, embryoUsedInfo, cxMachineConfigurationList);
    }

    /**
     * 获取除指定成型机台外的其它排产胎胚信息
     *
     * @param sameAppointMap 同机台指定胎胚信息
     * @return
     */
    private List<EmbryoUsedLhMachineInfo> getLeftOverEmbryoByAppoint(Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> sameAppointMap) {
        if (CollectionUtils.isEmpty(sameAppointMap)) {
            return Collections.emptyList();
        }
        Set<String> selectedEmbryoSet = Sets.newHashSet();
        sameAppointMap.forEach((singleCxMachine, appointEmbryoInfo) -> {
            if (CollectionUtils.isEmpty(appointEmbryoInfo)) {
                return;
            }
            appointEmbryoInfo.forEach(singleEmbryoInfo -> {
                String embryoCode = singleEmbryoInfo.getEmbryoCode();
                if (StringUtils.isBlank(embryoCode)) {
                    return;
                }
                selectedEmbryoSet.add(embryoCode);
            });
        });
        if (CollectionUtils.isEmpty(selectedEmbryoSet)) {
            return embryoUsedInfo;
        }
        return getLeftOverEmbryoUsedInfo(embryoUsedInfo, selectedEmbryoSet);
    }

    /**
     * 因胎胚指定机台，判断是否需要分机台分配
     * 如果指定胎胚的硫化机台数不是最大，则无需分配
     * 否则需要分配
     *
     * @param leftOverMaxLhMachines 无指定胎胚最大使用机台数
     * @param sameAppointMap        指定胎胚使用机台数信息
     * @return
     */
    private boolean isNeedAllocationByAppoint(Integer leftOverMaxLhMachines, Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> sameAppointMap) {
        for (Map.Entry<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> entry : sameAppointMap.entrySet()) {
            List<EmbryoUsedLhMachineInfo> findList = entry.getValue();
            if (CollectionUtils.isEmpty(findList)) {
                continue;
            }
            Integer sumLhMachines = findList.stream().mapToInt(EmbryoUsedLhMachineInfo::getUsedLhMachines).sum();
            if (sumLhMachines > leftOverMaxLhMachines) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对指定的机台分配满
     *
     * @param maxAppointGroup 指定
     * @param leftOverList    剩余可分配胎胚
     * @return
     */
    private List<EmbryoUsedLhMachineInfo> getLeftOverEmbryoUsedInfoAndFullAppoint(GroupCxMachineConfiguration maxAppointGroup, List<EmbryoUsedLhMachineInfo> leftOverList) {
        //对剩余分配胎胚按从小到大分配
        leftOverList.sort(Comparator.comparing(EmbryoUsedLhMachineInfo::getUsedLhMachines));
        Integer usedCount = maxAppointGroup.getAllocationEmbryoCodeCount();
        Integer leftOverCount = maxAppointGroup.getMaxEmbryoCodeCount() - usedCount;
        Integer usedLhMachine = maxAppointGroup.getAllocationLhMachines();
        Integer leftOverLhMachine = maxAppointGroup.getRealMaxLhMachines() - usedLhMachine;
        if (leftOverLhMachine <= BigDecimal.ZERO.intValue() || leftOverCount <= BigDecimal.ZERO.intValue()) {
            maxAppointGroup.flagAppointAllocationFinish();
            return leftOverList;
        }
        List<EmbryoUsedLhMachineInfo> leftOverResult = Lists.newArrayList();
        int size = leftOverList.size();
        int startIndex = BigDecimal.ZERO.intValue();
        for (; startIndex < leftOverCount; ) {
            if (leftOverLhMachine == BigDecimal.ZERO.intValue()) {
                break;
            }
            EmbryoUsedLhMachineInfo selectedEmbryo = leftOverList.get(startIndex);
            String embryoCode = selectedEmbryo.getEmbryoCode();
            Integer preAllocationCount = selectedEmbryo.getUsedLhMachines();
            Integer surplusLhMachine = preAllocationCount - leftOverLhMachine;
            //到这里，表示该胎胚一定会分走些
            startIndex = startIndex + BigDecimal.ONE.intValue();
            if (surplusLhMachine > BigDecimal.ZERO.intValue()) {
                //需要拆分
                EmbryoUsedLhMachineInfo split = new EmbryoUsedLhMachineInfo(embryoCode, surplusLhMachine);
                leftOverResult.add(split);
                EmbryoUsedLhMachineInfo allocationSplit = new EmbryoUsedLhMachineInfo(embryoCode, leftOverLhMachine);
                maxAppointGroup.addGroupAllocationByAppoint(allocationSplit);
                break;
            }
            leftOverLhMachine = leftOverLhMachine - preAllocationCount;
            maxAppointGroup.addGroupAllocationByAppoint(selectedEmbryo);
        }
        maxAppointGroup.flagAppointAllocationFinish();
        //还没有分配的加入
        if (startIndex >= size) {
            return leftOverResult;
        }
        List<EmbryoUsedLhMachineInfo> noAllocationList = leftOverList.subList(startIndex, size);
        leftOverResult.addAll(noAllocationList);
        if (CollectionUtils.isEmpty(leftOverResult)) {
            return Collections.emptyList();
        }
        return leftOverResult;
    }

    /**
     * 校验全是指定是是否可平衡
     * true 可平衡分配 false 表示不可平衡分配
     *
     * @param sameAppointMap
     * @return
     */
    private boolean checkAllAppointBalance(Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> sameAppointMap) {
        if (CollectionUtils.isEmpty(sameAppointMap)) {
            return true;
        }
        List<GroupCxMachineConfiguration> allAppointGroupList = Lists.newArrayList();
        for (Map.Entry<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> entry : sameAppointMap.entrySet()) {
            GroupCxMachineConfiguration singleCxMachineInfo = entry.getKey();
            List<EmbryoUsedLhMachineInfo> findList = entry.getValue();
            singleCxMachineInfo.setGroupAllocation(findList);
            allAppointGroupList.add(singleCxMachineInfo);
        }
        return checkAllByGroup(extraLhMachines, allAppointGroupList);
    }

    /**
     * 多台成型机且满胎胚且没有胎胚指定机台下，能否平衡分配
     *
     * @param extraLhMachines            额外处理的机台
     * @param embryoUsedInfo             胎胚硫化机台数
     * @param cxMachineConfigurationList 成型机台配置信息
     * @return
     */
    private boolean checkNoAppointFullEmbryoCountBalance(Integer extraLhMachines, List<EmbryoUsedLhMachineInfo> embryoUsedInfo, List<GroupCxMachineConfiguration> cxMachineConfigurationList) {
        //胎胚种类大，且硫化机台数大的先组合
        Comparator sort = Comparator.comparing(GroupCxMachineConfiguration::getMaxEmbryoCodeCount, Comparator.reverseOrder())
                .thenComparing(GroupCxMachineConfiguration::getRealMaxLhMachines, Comparator.reverseOrder());
        cxMachineConfigurationList.sort(sort);
        Set<String> selectedEmbryoSet = Sets.newHashSet();
        //分组处理
        cxMachineConfigurationList.forEach(singleCxMachineInfo -> {
            List<EmbryoUsedLhMachineInfo> canAllocationList = getLeftOverEmbryoUsedInfo(embryoUsedInfo, selectedEmbryoSet);
            List<EmbryoUsedLhMachineInfo> findList = selectFullEmbryoAllocation(canAllocationList, singleCxMachineInfo);
            singleCxMachineInfo.setGroupAllocation(findList);
            if (CollectionUtils.isEmpty(findList)) {
                return;
            }
            findList.forEach(singleFind -> {
                selectedEmbryoSet.add(singleFind.getEmbryoCode());
            });
        });
        return checkAllByGroup(extraLhMachines, cxMachineConfigurationList);
    }

    /**
     * 检测所有分配组，只要有一个不成立，则表示不能平衡分配
     *
     * @param extraLhMachines            额外处理机台数
     * @param cxMachineConfigurationList 已分配的组
     * @return
     */
    private boolean checkAllByGroup(Integer extraLhMachines, List<GroupCxMachineConfiguration> cxMachineConfigurationList) {
        //额外处理的加到最大硫化机台数上
        Integer handlerLhMachines = BigDecimal.ZERO.intValue();
        if (null != extraLhMachines) {
            handlerLhMachines = extraLhMachines;
        }
        //从分配最多的开始
        cxMachineConfigurationList.sort(Comparator.comparing(GroupCxMachineConfiguration::getAllocationLhMachines, Comparator.reverseOrder()));
        int index = BigDecimal.ZERO.intValue();
        for (GroupCxMachineConfiguration singleCxMachineInfo : cxMachineConfigurationList) {
            Integer realLhMachines = BigDecimal.ZERO.intValue();
            if (index == BigDecimal.ZERO.intValue()) {
                realLhMachines = handlerLhMachines;
            }
            if (singleCxMachineInfo.isReachLimit(realLhMachines)) {
                return false;
            }
            index = index + BigDecimal.ONE.intValue();
        }
        return true;
    }

    /**
     * 获取满胎胚硫化机台数信息
     * 以最大硫化机台数的胎胚 与 最小硫化机台数的胎胚进行组合
     *
     * @param canAllocationList   可分配的胎胚
     * @param singleCxMachineInfo 成型硫化配比配置
     * @return
     */
    private List<EmbryoUsedLhMachineInfo> selectFullEmbryoAllocation(List<EmbryoUsedLhMachineInfo> canAllocationList, GroupCxMachineConfiguration singleCxMachineInfo) {
        if (null == singleCxMachineInfo || null == singleCxMachineInfo.getMaxEmbryoCodeCount()) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(canAllocationList)) {
            return Collections.emptyList();
        }
        int maxSize = singleCxMachineInfo.getMaxEmbryoCodeCount();
        Set<String> selectedEmbryoSet = Sets.newHashSet();
        List<EmbryoUsedLhMachineInfo> findList = Lists.newArrayList();
        //硫化机台数从大到小排序
        canAllocationList.sort(Comparator.comparing(EmbryoUsedLhMachineInfo::getUsedLhMachines, Comparator.reverseOrder()));
        //取得最大占用硫化机台数的胎胚
        EmbryoUsedLhMachineInfo maxUsedLhMachineEmbryo = canAllocationList.get(BigDecimal.ZERO.intValue());
        findList.add(maxUsedLhMachineEmbryo);
        selectedEmbryoSet.add(maxUsedLhMachineEmbryo.getEmbryoCode());
        //再硫化机台数从小到大排序
        canAllocationList.sort(Comparator.comparing(EmbryoUsedLhMachineInfo::getUsedLhMachines));
        for (EmbryoUsedLhMachineInfo singleEmbryo : canAllocationList) {
            if (findList.size() == maxSize) {
                break;
            }
            if (selectedEmbryoSet.contains(singleEmbryo.getEmbryoCode())) {
                continue;
            }
            findList.add(singleEmbryo);
            selectedEmbryoSet.add(singleEmbryo.getEmbryoCode());
        }
        if (CollectionUtils.isEmpty(findList)) {
            return Collections.emptyList();
        }
        return findList;
    }

    /**
     * 获取剩余还未分配的胎胚信息
     *
     * @param embryoUsedInfo    全局信息
     * @param selectedEmbryoSet 已分配信息
     * @return
     */
    private List<EmbryoUsedLhMachineInfo> getLeftOverEmbryoUsedInfo(List<EmbryoUsedLhMachineInfo> embryoUsedInfo, Set<String> selectedEmbryoSet) {
        if (CollectionUtils.isEmpty(embryoUsedInfo)) {
            return Collections.emptyList();
        }
        Set<String> excludeSet = Optional.ofNullable(selectedEmbryoSet).orElse(Sets.newHashSet());
        List<EmbryoUsedLhMachineInfo> result = embryoUsedInfo.stream().filter(singleEmbryo -> !excludeSet.contains(singleEmbryo.getEmbryoCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * 是否有排产指定胎胚
     *
     * @return
     */
    private boolean isAppointEmbryoProduction() {
        Map<String, GroupCxMachineConfiguration> appointEmbryoInfoMap = getEmbryoAppointInfo();
        if (CollectionUtils.isEmpty(appointEmbryoInfoMap)) {
            return false;
        }
        for (EmbryoUsedLhMachineInfo singleEmbryoInfo : embryoUsedInfo) {
            if (appointEmbryoInfoMap.containsKey(singleEmbryoInfo.getEmbryoCode())) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取有指定机台的胎胚信息
     * key 指定的胎胚编码 value 指定的机台
     *
     * @return
     */
    private Map<String, GroupCxMachineConfiguration> getEmbryoAppointInfo() {
        Map<String, List<GroupCxMachineConfiguration>> appointEmbryoMap = Maps.newHashMap();
        cxMachineConfigurationList.forEach(singleCxMachineInfo -> {
            Set<String> appointEmbryoCodeSet = singleCxMachineInfo.getAppointEmbryoCodeSet();
            if (CollectionUtils.isEmpty(appointEmbryoCodeSet)) {
                return;
            }
            appointEmbryoCodeSet.forEach(embryoCode -> {
                List<GroupCxMachineConfiguration> appointInfoList = appointEmbryoMap.get(embryoCode);
                if (null == appointInfoList) {
                    appointInfoList = Lists.newArrayList();
                    appointEmbryoMap.put(embryoCode, appointInfoList);
                }
                appointInfoList.add(singleCxMachineInfo);
            });
        });
        if (CollectionUtils.isEmpty(appointEmbryoMap)) {
            return Collections.emptyMap();
        }
        Map<String, GroupCxMachineConfiguration> appointSingleMachineMap = Maps.newHashMap();
        appointEmbryoMap.forEach((embryoCode, appointInfo) -> {
            if (CollectionUtils.isEmpty(appointInfo)) {
                return;
            }
            if (appointInfo.size() > BigDecimal.ONE.intValue()) {
                return;
            }
            appointSingleMachineMap.put(embryoCode, appointInfo.get(BigDecimal.ZERO.intValue()));
        });
        if (CollectionUtils.isEmpty(appointSingleMachineMap)) {
            return Collections.emptyMap();
        }
        return appointSingleMachineMap;
    }

    /**
     * 获取相同指定的机台胎胚使用硫化机台信息
     *
     * @param appointEmbryoInfoMap
     * @return
     */
    private Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> getSameCxMachineAppoint(Map<String, GroupCxMachineConfiguration> appointEmbryoInfoMap) {
        if (CollectionUtils.isEmpty(appointEmbryoInfoMap)) {
            return Collections.emptyMap();
        }
        Map<GroupCxMachineConfiguration, List<EmbryoUsedLhMachineInfo>> sameResult = Maps.newHashMap();
        embryoUsedInfo.forEach(singleEmbryoUsedInfo -> {
            String embryoCode = singleEmbryoUsedInfo.getEmbryoCode();
            if (StringUtils.isBlank(embryoCode)) {
                return;
            }
            if (!appointEmbryoInfoMap.containsKey(embryoCode)) {
                return;
            }
            GroupCxMachineConfiguration cxMachineInfo = appointEmbryoInfoMap.get(embryoCode);
            List<EmbryoUsedLhMachineInfo> appointEmbryoList = sameResult.get(cxMachineInfo);
            if (null == appointEmbryoList) {
                appointEmbryoList = Lists.newArrayList();
                sameResult.put(cxMachineInfo, appointEmbryoList);
            }
            appointEmbryoList.add(singleEmbryoUsedInfo);
        });
        if (CollectionUtils.isEmpty(sameResult)) {
            return Collections.emptyMap();
        }
        return sameResult;
    }

}
