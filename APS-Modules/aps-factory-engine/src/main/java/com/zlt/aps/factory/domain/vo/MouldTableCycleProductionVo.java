package com.zlt.aps.factory.domain.vo;

import lombok.Getter;

import java.io.Serializable;

/**
 * 模台排产周期对象
 *
 * @author ZLT
 * @date 20250724
 */
@Getter
public class MouldTableCycleProductionVo implements Serializable {
    /**
     * 最早的起始日数
     */
    private Integer minStartDay;
    /**
     * 最晚的起始日数
     */
    private Integer maxEndDay;

    /**
     * 模台排产周期日
     * 最早日~最晚日
     *
     * @param minStartDay 最早日
     * @param maxEndDay   最晚日
     */
    public MouldTableCycleProductionVo(Integer minStartDay, Integer maxEndDay) {
        this.minStartDay = minStartDay;
        this.maxEndDay = maxEndDay;
    }
}
