package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 第1至5步所需的自动排程统一输入模型。
 *
 * <p>数据加载层负责把成型排程、施工、库存和库排宽表转换为该模型，纯算法不直接依赖数据库实体。</p>
 */
@Data
@Builder
public class Cd15AutoScheduleInput {

    /** 成型排程来源数据。 */
    private List<Cd15FormingScheduleSource> formingSchedules;
    /** 施工信息拆解后的钢带代码和单耗。 */
    private List<Cd15ConstructionMaterial> constructionMaterials;
    /** 6点库存来源数据。 */
    private List<Cd15StockSource> stocksAtSix;
    /** 胎胚月计划剩余量。 */
    private List<Cd15EmbryoPlanSurplus> embryoPlanSurpluses;
    /** 按钢带汇总的成型来源追溯信息。 */
    private Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip;

    /** 当前班次需求窗口明细。 */
    private List<Cd15DemandShift> demandShifts;
    /** 去除新增规格提前需求后的计划需求视图，原始需求仍用于成型消耗。 */
    private List<Cd15DemandShift> planningDemandShifts;
    /** 新增规格提前生产证据，按钢带代号分组。 */
    private Map<String, Cd15NewSpecAdvanceInfo> newSpecAdvanceInfoBySteelStrip;
    /** 按钢带代码匹配的备库班数，同时作为需求深度和库存保证阈值。 */
    private Map<String, BigDecimal> depthClassQtyBySteelStrip;
    /** 6点库排原始快照。 */
    private List<Cd15StorageLaneState> storageLanesAtSix;
    /** 班次开始前的实际或计划斜裁入库。 */
    private List<Cd15InboundRecord> inboundRecords;
    /** 实际库存和压延计划转换后的大卷成熟流水。 */
    private List<Cd15BigRollAgingStock> bigRollAgingStocks;
    /** 无法确定成熟时间的大卷编码，候选规格按DATA_MISSING处理。 */
    private Set<String> bigRollAgingDataMissingCodes;
}
