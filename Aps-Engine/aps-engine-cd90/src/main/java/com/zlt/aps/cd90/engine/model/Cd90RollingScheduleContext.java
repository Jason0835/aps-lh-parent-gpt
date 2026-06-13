package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    /** 6点至当前班次开始前的累计成型消耗量。 */
    private BigDecimal cumulativeConsumption;
    /** 已知MES实际入库记录。 */
    private List<Cd90InboundRecord> actualInboundRecords;
    /** 前序班次生成的计划入库记录。 */
    private List<Cd90InboundRecord> plannedInboundRecords;
    /** 已提交的全部班次任务。 */
    private List<Cd90ShiftScheduleTask> committedTasks;
    /** 各机台最近一次已提交任务的机尾规格。 */
    private Map<String, String> tailSpecByMachine;
}
