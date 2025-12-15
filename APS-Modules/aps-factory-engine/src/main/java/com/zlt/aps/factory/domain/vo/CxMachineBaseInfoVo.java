package com.zlt.aps.factory.domain.vo;

import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 成型基础信息对象
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class CxMachineBaseInfoVo implements Serializable {

    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 成型机台
     */
    private String cxMachineCode;

    /**
     * 是否零度供料架
     */
    private String isZeroRack;

    /**
     * 硫化上限
     */
    private Integer lhMachineMaxQty;

    /**
     * 固定结构1
     */
    private String fixedStructure1;

    /**
     * 固定结构2
     */
    private String fixedStructure2;

    /**
     * 固定结构3
     */
    private String fixedStructure3;

    /**
     * 固定SKU
     */
    private String fixedMaterialCode;

    /**
     * 不可作业结构
     */
    private String disableStructure;

    /**
     * 不可作业SKU
     */
    private String disableMaterialCode;

    /**
     * 停产日(包含维修及全局停产日)信息
     */
    private Set<Integer> stopDayInfo;
    /**
     * 最大可排产天数
     */
    private Integer maxProductionDays;

    /**
     * 剩余可分配天数
     */
    private Integer remainingDays;

    /**
     * 分配的分组计划集合(TBR按结构)
     */
    private List<CxMachineAllocationPlanHelper> allocationList;

    /**
     * 新增分配的分组计划信息
     *
     * @param addAllocationPlan
     */
    public void addAllocationPlanInfo(CxMachineAllocationPlanHelper addAllocationPlan) {
        if (null == addAllocationPlan) {
            return;
        }
        if (null == allocationList) {
            allocationList = new ArrayList<>();
        }
        allocationList.add(addAllocationPlan);
    }
}
