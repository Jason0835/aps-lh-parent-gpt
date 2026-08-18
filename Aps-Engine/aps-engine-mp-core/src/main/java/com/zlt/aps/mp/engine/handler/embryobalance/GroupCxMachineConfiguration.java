package com.zlt.aps.mp.engine.handler.embryobalance;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 胎胚平衡检查业务
 * 结构-硫化配比信息
 *
 * @author ZLT
 * @date 20260814
 */
@Getter
public class GroupCxMachineConfiguration implements Serializable {
    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 分组名：TBR-结构
     */
    private String groupName;
    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;
    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachines;
    /**
     * 实际最大硫化机台数
     */
    private Integer realMaxLhMachines;
    /**
     * 指定胎胚
     */
    private Set<String> appointEmbryoCodeSet;
    /**
     * 平衡分配，只在检测时赋值
     */
    private List<EmbryoUsedLhMachineInfo> groupSelected;
    /**
     * 标记分配完成：指定情形下使用
     */
    private Boolean allocationFinish;

    /**
     * 构建配置信息
     *
     * @param cxMachineCode
     * @param groupName
     * @param maxEmbryoCodeCount
     * @param maxLhMachines
     * @param realMaxLhMachines
     * @param appointEmbryoCodeSet
     */
    public GroupCxMachineConfiguration(String cxMachineCode,
                                       String groupName,
                                       Integer maxEmbryoCodeCount,
                                       Integer maxLhMachines,
                                       Integer realMaxLhMachines,
                                       Set<String> appointEmbryoCodeSet) {
        this.cxMachineCode = cxMachineCode;
        this.groupName = groupName;
        this.maxEmbryoCodeCount = maxEmbryoCodeCount;
        this.maxLhMachines = maxLhMachines;
        this.realMaxLhMachines = realMaxLhMachines;
        this.appointEmbryoCodeSet = appointEmbryoCodeSet;
    }

    /**
     * 设置临时分配信息
     *
     * @param temporaryAssignment
     */
    public void setGroupAllocation(List<EmbryoUsedLhMachineInfo> temporaryAssignment) {
        groupSelected = null;
        if (CollectionUtils.isEmpty(temporaryAssignment)) {
            return;
        }
        groupSelected = Lists.newArrayList(temporaryAssignment);
    }

    /**
     * 指定机台分配信息
     *
     * @param temporaryAssignment
     */
    public void addGroupAllocationByAppoint(EmbryoUsedLhMachineInfo temporaryAssignment) {
        if (null == temporaryAssignment || StringUtils.isBlank(temporaryAssignment.getEmbryoCode())) {
            return;
        }
        if (null == temporaryAssignment.getUsedLhMachines() || temporaryAssignment.getUsedLhMachines() <= BigDecimal.ZERO.intValue()) {
            return;
        }
        if (CollectionUtils.isEmpty(groupSelected)) {
            return;
        }
        groupSelected.add(temporaryAssignment);
    }

    /**
     * 分配到的硫化机台数
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
     * 分配到的胎胚种类数
     *
     * @return
     */
    public Integer getAllocationEmbryoCodeCount() {
        if (CollectionUtils.isEmpty(groupSelected)) {
            return BigDecimal.ZERO.intValue();
        }
        Set<String> embryoCodeSet = groupSelected.stream().map(EmbryoUsedLhMachineInfo::getEmbryoCode).collect(Collectors.toSet());
        return embryoCodeSet.size();
    }

    /**
     * 是否达到限制 true 超出 false 没有超出
     *
     * @param extraLhMachines
     * @return
     */
    public boolean isReachLimit(Integer extraLhMachines) {
        Integer usedLhMachines = getAllocationLhMachines();
        Integer max = this.maxLhMachines;
        if (null != extraLhMachines && extraLhMachines > BigDecimal.ZERO.intValue()) {
            max = max + extraLhMachines;
        }
        if (null != extraLhMachines && extraLhMachines < BigDecimal.ZERO.intValue()) {
            usedLhMachines = usedLhMachines + extraLhMachines;
        }
        return usedLhMachines > max;
    }

    /**
     * 标记指定分配完成
     */
    public void flagAppointAllocationFinish() {
        allocationFinish = Boolean.TRUE;
    }
}
