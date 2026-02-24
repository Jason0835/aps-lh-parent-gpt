package com.zlt.aps.mp.engine.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 在机结构-月初可释放机台数，和保留时，最早收尾时间点
 *
 * @author zlt
 * @date 20251224
 */
@Data
public class CxContinueMachineReleaseHelper implements Serializable {
    /**
     * 月初可直接释放机台数
     */
    private Integer releaseMachineCount;
    /**
     * 扣减机台数，用以判断续作最早收尾时间点
     * 传递值-进行递归迭代使用
     */
    private Integer deductionMachineCount;
    /**
     * 续作最早收尾时间点
     */
    private Integer earliestConclusionDay;

    /**
     * 构造函数
     *
     * @param releaseMachineCount   月初可释放机台
     * @param deductionMachineCount 扣减机台数，用以确认续作最早收尾时间点
     */
    public CxContinueMachineReleaseHelper(Integer releaseMachineCount, Integer deductionMachineCount) {
        this.releaseMachineCount = releaseMachineCount;
        this.deductionMachineCount = deductionMachineCount;
    }
}
