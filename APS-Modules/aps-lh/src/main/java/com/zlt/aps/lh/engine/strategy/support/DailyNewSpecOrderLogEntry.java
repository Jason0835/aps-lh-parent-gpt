package com.zlt.aps.lh.engine.strategy.support;

import java.io.Serializable;

/**
 * 新增按天排产的单条 SKU 实际执行顺序日志明细。
 *
 * <p>该对象只保存最终展示所需的标量字段，不持有 {@code SkuScheduleDTO}、候选机台列表、
 * 排产结果或排程上下文，避免每日顺序日志延长大对象生命周期。每次真实进入新增主循环都会
 * 创建独立明细，同一 SKU 跨阶段或跨轮次再次进入时不做去重。</p>
 *
 * @author APS
 */
public class DailyNewSpecOrderLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 物料编码/SKU */
    private final String materialCode;
    /** 当前真实执行的新增排产阶段名称 */
    private final String productionPhase;
    /** 当前 SKU 在本业务日真正进入新增选机流程时的稳定顺序 */
    private final int selectionOrder;
    /** 当前进入新增排产入口的主来源类型 */
    private final String materialSourceType;
    /** 当前实际业务日的原始月计划 dayN 数量 */
    private final int originalDayPlanQty;
    /** 是否命中现有结构切换或结构收尾提前生产决策 */
    private final boolean structureEarlyProduction;
    /** 当前业务日根据现有规则需要安排的目标机台数量 */
    private int requiredMachineCount;

    /**
     * 创建每日新增排产顺序明细。
     *
     * @param materialCode 物料编码/SKU
     * @param selectionOrder 当前 SKU 在本业务日真正进入新增选机流程的顺序
     * @param productionPhase 当前真实执行阶段名称
     * @param materialSourceType 当前新增入口主来源类型
     * @param originalDayPlanQty 当前实际业务日原始月计划 dayN 数量
     * @param structureEarlyProduction 是否结构提前
     * @param requiredMachineCount 当前业务日初始目标机台数
     */
    public DailyNewSpecOrderLogEntry(String materialCode,
                                     int selectionOrder,
                                     String productionPhase,
                                     String materialSourceType,
                                     int originalDayPlanQty,
                                     boolean structureEarlyProduction,
                                     int requiredMachineCount) {
        this.materialCode = materialCode;
        this.selectionOrder = selectionOrder;
        this.productionPhase = productionPhase;
        this.materialSourceType = materialSourceType;
        // 日志必须展示现有计划数据的原值，不在观察对象中二次修正。
        this.originalDayPlanQty = originalDayPlanQty;
        this.structureEarlyProduction = structureEarlyProduction;
        this.requiredMachineCount = requiredMachineCount;
    }

    /**
     * 使用同一次真实排产计算得到的更大目标机台数更新日志。
     *
     * <p>多机台拆量会先登记当前机台目标槽位，再由 dayN 动态扩机计算回填完整目标数；
     * 这里只允许目标数增加，避免候选机台失败或最终成功数较小反向覆盖规则目标数。</p>
     *
     * @param requiredMachineCount 当前业务日最新目标机台数
     */
    public void updateRequiredMachineCount(int requiredMachineCount) {
        if (requiredMachineCount > this.requiredMachineCount) {
            this.requiredMachineCount = requiredMachineCount;
        }
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public String getProductionPhase() {
        return productionPhase;
    }

    public int getSelectionOrder() {
        return selectionOrder;
    }

    public String getMaterialSourceType() {
        return materialSourceType;
    }

    public int getOriginalDayPlanQty() {
        return originalDayPlanQty;
    }

    public boolean isStructureEarlyProduction() {
        return structureEarlyProduction;
    }

    public int getRequiredMachineCount() {
        return requiredMachineCount;
    }
}
