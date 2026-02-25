package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 单计划排产量对象
 *
 * @author ZLT
 * @date 20250311
 */
@Data
public class SinglePlanInfoHelper implements Serializable {
    /**
     * 排产计划ID
     */
    private Long planId;
    /**
     * 排产数量
     */
    private Long qty;
    /**
     * 排产顺序
     */
    private Long seq;

    public SinglePlanInfoHelper() {
    }

    /**
     * 全参构造函数
     *
     * @param planId 计划ID
     * @param qty    数量
     * @param seq    排产顺序
     */
    public SinglePlanInfoHelper(Long planId, Long qty, Long seq) {
        this.planId = planId;
        this.qty = qty;
        this.seq = seq;
    }


}
