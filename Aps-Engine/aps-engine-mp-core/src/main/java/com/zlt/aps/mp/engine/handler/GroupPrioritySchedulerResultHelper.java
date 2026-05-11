package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Sets;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.enums.GroupCxMachinePriorityEnum;
import lombok.Getter;

import java.io.Serializable;
import java.util.Set;

/**
 * 分组优先级调度器结构辅助类
 * 用以辅助，分组优先级挑选的结果
 *
 * @author ZLT
 * @date 20260426
 */
@Getter
public class GroupPrioritySchedulerResultHelper implements Serializable {
    /**
     * 选中的分组计划对象
     */
    private ProductionPlanGroupInfo selectedGroup;
    /**
     * 选中的成型机台
     */
    private CxMachineBaseInfoVo selectedCxMachine;
    /**
     * 排产日集合
     */
    private Set<Integer> selectedProductionDaySet;
    /**
     * 排产优先值信息
     */
    private CxMachineGroupPriorityValueHelper priorityValue;

    public GroupPrioritySchedulerResultHelper(ProductionPlanGroupInfo selectedGroup, CxMachineBaseInfoVo selectedCxMachine) {
        this.selectedGroup = selectedGroup;
        this.selectedCxMachine = selectedCxMachine;
        if (null != selectedCxMachine) {
            //拷贝
            this.priorityValue = CxMachineGroupPriorityValueHelper.copy(selectedCxMachine.getPriorityValue());
            this.selectedProductionDaySet = Sets.newHashSet();
            selectedProductionDaySet.addAll(selectedCxMachine.getSelectedProductionDaySet());
        }
    }

    /**
     * 选择优先级-值
     *
     * @return
     */
    public Integer getSelectedPriorityValue() {
        if (null == priorityValue) {
            return GroupCxMachinePriorityEnum.DEFAULT_VALUE.getPriorityValue();
        }
        return priorityValue.getPriorityType().getPriorityValue();
    }

    /**
     * 选择优先级的差值：需求天数 - 成型产能天数
     *
     * @return
     */
    public Integer getSelectedPriorityDiffValue() {
        if (null == priorityValue) {
            return Integer.MAX_VALUE;
        }
        return priorityValue.getDiffValue();
    }

    /**
     * 选中的机台
     *
     * @return
     */
    public String getSelectedCxMachineCode() {
        if (null == selectedCxMachine) {
            return null;
        }
        return selectedCxMachine.getCxMachineCode();
    }
}
