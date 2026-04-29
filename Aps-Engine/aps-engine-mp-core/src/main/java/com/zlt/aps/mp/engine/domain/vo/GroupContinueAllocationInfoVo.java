package com.zlt.aps.mp.engine.domain.vo;

import lombok.Getter;

import java.io.Serializable;

/**
 * 分组在续作测算后的分配初始信息
 *
 * @author ZLT
 * @date 20260428
 */
@Getter
public class GroupContinueAllocationInfoVo implements Serializable {
    /**
     * 剩余需要分配的天数
     */
    private Integer leftOverNeedAllocationDays;
    /**
     * 是否分配完毕 1 分配完成
     */
    private Integer isAllocationFinish;

    public GroupContinueAllocationInfoVo(Integer leftOverNeedAllocationDays, Integer isAllocationFinish) {
        this.leftOverNeedAllocationDays = leftOverNeedAllocationDays;
        this.isAllocationFinish = isAllocationFinish;
    }
}
