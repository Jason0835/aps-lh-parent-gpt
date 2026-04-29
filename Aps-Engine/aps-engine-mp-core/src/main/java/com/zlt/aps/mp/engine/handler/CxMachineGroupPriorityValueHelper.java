package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.enums.GroupCxMachinePriorityEnum;
import lombok.Getter;

import java.io.Serializable;

/**
 * 以机台为维度，匹配结构的优先等级以及需求天数，与机台产能天数
 *
 * @author ZLT
 * @date 20260426
 */
@Getter
public class CxMachineGroupPriorityValueHelper implements Serializable {
    /**
     * 优先级别
     */
    private GroupCxMachinePriorityEnum priorityType;
    /**
     * 需求天数
     */
    private Integer needDays;
    /**
     * 机台产能天数
     */
    private Integer capacityDays;

    public CxMachineGroupPriorityValueHelper(GroupCxMachinePriorityEnum priorityType, Integer needDays, Integer capacityDays) {
        this.priorityType = priorityType;
        this.needDays = needDays;
        this.capacityDays = capacityDays;
    }

    /**
     * 拷贝
     *
     * @param origin
     * @return
     */
    public static CxMachineGroupPriorityValueHelper copy(CxMachineGroupPriorityValueHelper origin) {
        return new CxMachineGroupPriorityValueHelper(origin.getPriorityType(), origin.getNeedDays(), origin.getCapacityDays());
    }

    /**
     * 需求可覆盖成型产能-差值
     *
     * @return
     */
    public Integer getDiffValue() {
        return needDays - capacityDays;
    }
}
