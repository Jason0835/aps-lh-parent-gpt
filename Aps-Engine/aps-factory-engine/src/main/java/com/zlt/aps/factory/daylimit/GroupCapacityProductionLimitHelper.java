package com.zlt.aps.factory.daylimit;

import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 分组计划产能排产限制结果对象
 *
 * @author ZLT
 * @date 20260125
 */
@Getter
public class GroupCapacityProductionLimitHelper implements Serializable {
    /**
     * 可排产日集合
     */
    private Set<Integer> productionDaySet;
    /**
     * 限制类型
     */
    private GroupAllocationCapacityLimitTypeEnum limitType;

    /**
     * 构建空排产日集合但又无限制的对象实例
     *
     * @return
     */
    public static GroupCapacityProductionLimitHelper createNoLimitEmptyProductionSet() {
        return new GroupCapacityProductionLimitHelper(Collections.emptySet(), GroupAllocationCapacityLimitTypeEnum.NO_ENTER_LIMIT);
    }

    public GroupCapacityProductionLimitHelper(Set<Integer> productionDaySet, GroupAllocationCapacityLimitTypeEnum limitType) {
        this.productionDaySet = productionDaySet;
        this.limitType = limitType;
    }

}
