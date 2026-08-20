package com.zlt.aps.mp.engine.handler.embryobalance;

import com.google.common.collect.Lists;
import com.zlt.aps.mp.engine.enums.EmbryoFindType;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 胎胚分配结果对象信息
 * 日胎胚
 *
 * @author ZLT
 * @date 20260820
 */
@Getter
public class SplitEmbryoGroupInfo implements Serializable {
    /**
     * 分组数
     */
    private int groupNumber;
    /**
     * 胎胚平均机台数
     */
    private int embryoAverageLhMachine;
    /**
     * 最大胎胚数
     */
    private int maxEmbryoCount;
    /**
     * 平衡分配，只在检测时赋值
     */
    private List<EmbryoUsedLhMachineInfo> groupSelected;

    /**
     * 创建初始化信息
     *
     * @param groupNumber
     * @param embryoAverageLhMachine
     * @param maxEmbryoCount
     * @return
     */
    public static SplitEmbryoGroupInfo buildInit(int groupNumber, int embryoAverageLhMachine, int maxEmbryoCount) {
        SplitEmbryoGroupInfo info = new SplitEmbryoGroupInfo();
        info.groupNumber = groupNumber;
        info.embryoAverageLhMachine = embryoAverageLhMachine;
        info.maxEmbryoCount = maxEmbryoCount;
        info.groupSelected = Lists.newArrayList();
        return info;
    }

    /**
     * 是否可加入
     *
     * @return
     */
    public boolean hasAddEmbryoLhMachines() {
        int currentSize = getCurrentEmbryoSize();
        return currentSize < maxEmbryoCount;
    }

    /**
     * 分配的胎胚种类数
     *
     * @return
     */
    public Integer getAllocationEmbryoCount() {
        return getCurrentEmbryoSize();
    }

    /**
     * 分配的硫化机台数
     *
     * @return
     */
    public Integer getAllocationLhMachines() {
        if (CollectionUtils.isEmpty(groupSelected)) {
            return BigDecimal.ZERO.intValue();
        }
        return groupSelected.stream().mapToInt(EmbryoUsedLhMachineInfo::getUsedLhMachines).sum();
    }

    /**
     * 获取下一个胎胚所占硫化机台数情形
     * EmbryoFindType.LT_AVERAGE 取小于平均数
     * EmbryoFindType.GT_AVERAGE 取大于平均数
     * EmbryoFindType.ET_AVERAGE 取等于平均数
     *
     * @return
     */
    public EmbryoFindType findGreaterThanEmbryoAverageLhMachine() {
        int currentSize = getCurrentEmbryoSize();
        int sumAverage = currentSize * embryoAverageLhMachine;
        if (CollectionUtils.isEmpty(groupSelected)) {
            return EmbryoFindType.GT_AVERAGE;
        }
        int sumLhMachines = groupSelected.stream().mapToInt(EmbryoUsedLhMachineInfo::getUsedLhMachines).sum();
        if (sumLhMachines < sumAverage) {
            return EmbryoFindType.GT_AVERAGE;
        }
        if (sumLhMachines == sumAverage) {
            return EmbryoFindType.ET_AVERAGE;
        }
        return EmbryoFindType.LT_AVERAGE;
    }

    /**
     * 加入分配
     *
     * @param singleEmbryoUsedInfo
     */
    public boolean addEmbryoLhMachines(EmbryoUsedLhMachineInfo singleEmbryoUsedInfo) {
        if (null == groupSelected) {
            return false;
        }
        return groupSelected.add(singleEmbryoUsedInfo);
    }

    /**
     * 是否达到限制
     *
     * @param cxMachineLimit
     * @return
     */
    public boolean checkReachLimit(GroupCxMachineConfiguration cxMachineLimit) {
        if (null == cxMachineLimit) {
            return true;
        }
        Integer usedEmbryoCount = getAllocationEmbryoCount();
        if (usedEmbryoCount > cxMachineLimit.getMaxEmbryoCodeCount()) {
            return true;
        }
        Integer usedLhMachines = getAllocationLhMachines();
        return usedLhMachines > cxMachineLimit.getRealMaxLhMachines();
    }

    /**
     * 当前分配胎胚数
     *
     * @return
     */
    private int getCurrentEmbryoSize() {
        if (CollectionUtils.isEmpty(groupSelected)) {
            return BigDecimal.ZERO.intValue();
        }
        return groupSelected.size();
    }


}
