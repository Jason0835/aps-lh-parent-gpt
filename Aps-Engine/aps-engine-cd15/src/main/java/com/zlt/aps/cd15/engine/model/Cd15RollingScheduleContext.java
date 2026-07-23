package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多班滚动排程共享的内存上下文。
 *
 * <p>每班资源必须从6点原始库排重建，本对象只累计消耗、计划入库、任务和机尾规格。</p>
 */
@Data
@Builder
public class Cd15RollingScheduleContext {

    /** 6点库排原始快照，后续班次不得直接覆盖。 */
    private List<Cd15StorageLaneState> storageLanesAtSix;
    /** 6点至当前班次开始前的累计成型消耗量，按钢带代号分组。 */
    private Map<String, BigDecimal> cumulativeConsumptionBySteelStrip;
    /** 已知MES实际入库记录。 */
    private List<Cd15InboundRecord> actualInboundRecords;
    /** 前序班次生成的计划入库记录。 */
    private List<Cd15InboundRecord> plannedInboundRecords;
    /** 已提交的全部班次任务。 */
    private List<Cd15ShiftScheduleTask> committedTasks;
    /** 前序班次真实部分排后尚未覆盖的续作需求量，按钢带代号分组。 */
    private Map<String, BigDecimal> continueDemandBySteelStrip;
    /** 跨班待排任务，保留来源记录、原顺序、机台约束和剩余量。 */
    private List<Cd15RollingPendingTask> pendingTasks;
    /** 首班锁定的新增规格提前生产证据。 */
    private Map<String, Cd15NewSpecAdvanceInfo> newSpecAdvanceInfoBySteelStrip;
    /** 尚未转入真实续作的新增规格提前需求剩余量。 */
    private Map<String, BigDecimal> newSpecAdvanceRemainingBySteelStrip;
    /** 已按施工宽度换算过的新增规格剩余量，后续班次不得重复换算。 */
    private Set<String> normalizedNewSpecAdvanceSteelStripCodes;
    /** 已执行首次均分、等待下一班完成的施工材料稳定键。 */
    private Set<String> equalSharePendingMaterialKeys;
    /** 跨班保留的大卷成熟库存及已分配米数。 */
    private List<Cd15BigRollAgingStock> bigRollAgingStocks;
    /** 各机台最近一次已提交任务的机尾规格。 */
    private Map<String, String> tailSpecByMachine;
    /** 每个斜裁规格最近一次实际生产机台，用于跨班续作优先回原机台。 */
    private Map<String, String> lastMachineBySteelStrip;
    /** 各机台最近一次已提交任务的机尾大卷与斜裁规格。 */
    private Map<String, Cd15MachineTailState> tailByMachine;
}
