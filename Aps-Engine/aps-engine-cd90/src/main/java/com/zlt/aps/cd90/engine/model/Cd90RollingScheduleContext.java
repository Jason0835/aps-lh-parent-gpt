package com.zlt.aps.cd90.engine.model;

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
public class Cd90RollingScheduleContext {

    /** 6点库排原始快照，后续班次不得直接覆盖。 */
    private List<Cd90StorageLaneState> storageLanesAtSix;
    /** 6点至当前班次开始前的累计成型消耗量，按帘布代号分组。 */
    private Map<String, BigDecimal> cumulativeConsumptionByCloth;
    /** 已知MES实际入库记录。 */
    private List<Cd90InboundRecord> actualInboundRecords;
    /** 前序班次生成的计划入库记录。 */
    private List<Cd90InboundRecord> plannedInboundRecords;
    /** 已提交的全部班次任务。 */
    private List<Cd90ShiftScheduleTask> committedTasks;
    /** 前序班次真实部分排后尚未覆盖的续作需求量，按帘布代号分组。 */
    private Map<String, BigDecimal> continueDemandByCloth;
    /** 跨班待排任务，保留来源记录、原顺序、机台约束和剩余量。 */
    private List<Cd90RollingPendingTask> pendingTasks;
    /** 首班锁定的新增规格提前生产证据。 */
    private Map<String, Cd90NewSpecAdvanceInfo> newSpecAdvanceInfoByCloth;
    /** 尚未转入真实续作的新增规格提前需求剩余量。 */
    private Map<String, BigDecimal> newSpecAdvanceRemainingByCloth;
    /** 已按施工宽度换算过的新增规格剩余量，后续班次不得重复换算。 */
    private Set<String> normalizedNewSpecAdvanceClothCodes;
    /** 跨班保留的大卷成熟库存及已分配米数。 */
    private List<Cd90BigRollAgingStock> bigRollAgingStocks;
    /** 各机台最近一次已提交任务的机尾规格。 */
    private Map<String, String> tailSpecByMachine;
    /** 每个直裁规格最近一次实际生产机台，用于跨班续作优先回原机台。 */
    private Map<String, String> lastMachineByCloth;
    /** 各机台最近一次已提交任务的机尾大卷与直裁规格。 */
    private Map<String, Cd90MachineTailState> tailByMachine;
}
