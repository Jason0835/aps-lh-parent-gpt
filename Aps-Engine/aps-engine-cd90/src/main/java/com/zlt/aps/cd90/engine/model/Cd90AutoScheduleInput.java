package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 第1至5步所需的自动排程统一输入模型。
 *
 * <p>数据加载层负责把成型排程、施工、库存和库排宽表转换为该模型，纯算法不直接依赖数据库实体。</p>
 */
@Data
@Builder
public class Cd90AutoScheduleInput {

    /** 成型排程来源数据。 */
    private List<Cd90FormingScheduleSource> formingSchedules;
    /** 施工信息拆解后的帘布代码和单耗。 */
    private List<Cd90ConstructionMaterial> constructionMaterials;
    /** 6点库存来源数据。 */
    private List<Cd90StockSource> stocksAtSix;
    /** 胎胚月计划剩余量。 */
    private List<Cd90EmbryoPlanSurplus> embryoPlanSurpluses;

    /** 当前班次需求窗口明细。 */
    private List<Cd90DemandShift> demandShifts;
    /** 6点库排原始快照。 */
    private List<Cd90StorageLaneState> storageLanesAtSix;
    /** 班次开始前的实际或计划直裁入库。 */
    private List<Cd90InboundRecord> inboundRecords;
    /** 实际库存和压延计划转换后的大卷成熟流水。 */
    private List<Cd90BigRollAgingStock> bigRollAgingStocks;
    /** 无法确定成熟时间的大卷编码，候选规格按DATA_MISSING处理。 */
    private Set<String> bigRollAgingDataMissingCodes;
}
