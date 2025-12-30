package com.zlt.aps.factory.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 分组计划 - TBR为结构，PCR为英寸(寸口、寸别)
 * 日排产汇总信息对象
 *
 * @author ZLT
 * @date 20251230
 */
@Data
public class GroupDayProductionSummaryHelper implements Serializable {
    /**
     * 分组名
     * TBR 结构名
     * PCR 英寸
     */
    private String groupName;
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 胎胚种类数
     */
    private Integer embryoCount;
    /**
     * 硫化组
     */
    private Integer lhGroupCount;
    /**
     * 胎胚种类数集合
     */
    private Set<String> embryoCodeSet;
    /**
     * 硫化组集合
     */
    private Set<Integer> lhGroupNoSet;

    /**
     * 是否符合减机台条件
     *
     * @param maxEmbryoCount  最大胎胚种类数
     * @param maxLhGroupCount 最大硫化组机台数
     * @return
     */
    public boolean isMatchDeductionMachine(Integer maxEmbryoCount, Integer maxLhGroupCount) {
        if (embryoCount > maxEmbryoCount) {
            return false;
        }
        return lhGroupCount <= maxLhGroupCount;
    }

    /**
     * 创建空数据对象
     *
     * @param groupName     分组名
     * @param productionDay 排产日
     * @return
     */
    public static GroupDayProductionSummaryHelper buildEmpty(String groupName, Integer productionDay) {
        return new GroupDayProductionSummaryHelper(groupName, productionDay);
    }

    /**
     * 构造函数-默认分组名，排产日
     *
     * @param groupName     分组名
     * @param productionDay 排产日
     */
    public GroupDayProductionSummaryHelper(String groupName, Integer productionDay) {
        this.groupName = groupName;
        this.productionDay = productionDay;
        this.embryoCodeSet = new HashSet<>();
        this.lhGroupNoSet = new HashSet<>();
        this.embryoCount = BigDecimal.ZERO.intValue();
        this.lhGroupCount = BigDecimal.ZERO.intValue();
    }
}
