package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.util.Date;

/**
 * 当前候选的维修组合输入，仅在本次解析内使用，不登记生产资源。
 *
 * @author APS
 * @date 2026/10/21
 */
@Data
public class RepairPlanRequest {

    /**
     * 运行侧机台编码。
     */
    private String machineCode;
    /**
     * 候选物料编码；仅时间计算入口可不提供。
     */
    private String materialCode;
    /**
     * 候选产品状态；不参与维修来源身份。
     */
    private String productStatus;
    /**
     * 消费场景，包括续作、换模、换活字块和预演。
     */
    private String scenario;
    /**
     * 上一段真实生产结束时间，用于排除已完成的维修。
     */
    private Date previousProductionEndTime;
    /**
     * 当前切换开始时间。
     */
    private Date switchStartTime;
    /**
     * 当前切换自身完成时间，不含维修预热。
     */
    private Date switchEndTime;

    /**
     * 复制本次完整输入，结果对象不得共享调用方可变日期。
     *
     * @return 与原请求日期独立的输入快照
     */
    public RepairPlanRequest copy() {
        RepairPlanRequest target = new RepairPlanRequest();
        target.setMachineCode(machineCode);
        target.setMaterialCode(materialCode);
        target.setProductStatus(productStatus);
        target.setScenario(scenario);
        target.setPreviousProductionEndTime(RepairPlanRuntimeState.copyDate(previousProductionEndTime));
        target.setSwitchStartTime(RepairPlanRuntimeState.copyDate(switchStartTime));
        target.setSwitchEndTime(RepairPlanRuntimeState.copyDate(switchEndTime));
        return target;
    }
}
