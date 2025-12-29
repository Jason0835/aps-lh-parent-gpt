package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * 分组计划 - TBR为结构，PCR为英寸(寸口、寸别)
 * 成型硫化产能限制信息对象
 * 最大胎胚种类数
 * 最大硫化机台数
 * 实单最低硫化机台数
 * 实际已排产的胎胚信息
 * 实际已排产的模具信息
 * <p>
 * 用以值传递，没有其它特殊含义
 *
 * @author ZLT
 * @date 20251229
 */
@Getter
public class GroupPlanCxLhCapacityLimitHelper {

    /**
     * 排产日 处于排产周期内第几天
     */
    private Integer day;

    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;

    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachineCount;

    /**
     * 实单最低硫化机台数
     */
    private Integer minLhMachineCount;

    /**
     * 实际排产的胎胚信息
     */
    private Set<String> productionEmbryoCodeSet;

    /**
     * 实际排产的模具信息
     */
    private Set<String> productionMouldSet;

    /**
     * 构建空数据对象实例
     *
     * @param day                排产日
     * @param maxEmbryoCodeCount 最大胎胚种类数
     * @param maxLhMachineCount  最大硫化机台数
     * @param minLhMachineCount  实单最低硫化机台数
     * @return
     */
    public static GroupPlanCxLhCapacityLimitHelper buildEmptyData(Integer day, Integer maxEmbryoCodeCount, Integer maxLhMachineCount, Integer minLhMachineCount) {
        if (null == day) {
            return null;
        }
        GroupPlanCxLhCapacityLimitHelper limitHelper = new GroupPlanCxLhCapacityLimitHelper(day, maxEmbryoCodeCount, maxLhMachineCount, minLhMachineCount);
        return limitHelper;
    }

    /**
     * 构造函数
     *
     * @param day                排产日
     * @param maxEmbryoCodeCount 最大胎胚种类数
     * @param maxLhMachineCount  最大硫化机台数
     * @param minLhMachineCount  实单最小硫化机台数
     */
    public GroupPlanCxLhCapacityLimitHelper(Integer day, Integer maxEmbryoCodeCount, Integer maxLhMachineCount, Integer minLhMachineCount) {
        this.day = day;
        this.maxEmbryoCodeCount = maxEmbryoCodeCount;
        this.maxLhMachineCount = maxLhMachineCount;
        this.minLhMachineCount = minLhMachineCount;
        this.productionEmbryoCodeSet = new HashSet<>();
        this.productionMouldSet = new HashSet<>();
    }
}
