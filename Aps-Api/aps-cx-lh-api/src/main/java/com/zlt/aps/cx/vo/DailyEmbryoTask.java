package com.zlt.aps.cx.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 日胎胚任务 - 排程流水线中的核心任务对象。
 *
 * <p>语义：某排程日、某硫化任务（lhId）对应的一条成型待排记录；同一胎胚可因多物料/多任务出现多条实例。
 *
 * <p><b>写入方</b>：主要由 TaskGroupService 在 S5.2 构建与变异。
 * <b>读取方</b>：ContinueTaskProcessor / TrialTaskProcessor / NewTaskProcessor / BalancingService / ShiftScheduleService。
 *
 * @author APS Team
 */
@Data
public class DailyEmbryoTask {

    // --- 身份与关联 ---
    /** 胎胚编码（成型产出物） */
    private String embryoCode;
    /** 成品物料编码（硫化侧物料，用于主销判定、结果表关联） */
    private String materialCode;
    /** 成品物料描述 */
    private String materialDesc;
    /** 主物料/胎胚描述（展示用） */
    private String mainMaterialDesc;
    /** 结构名称（机台配置、配比、DFS 按结构分组键） */
    private String structureName;
    /** 硫化排程任务主键；关联 context.materialStockMap 中按任务分配的库存 */
    private Long lhId;
    /** 硫化机台编号（同一台硫化机L+R模共享，用于硫化机台数去重） */
    private String lhMachineCode;
    /** 月计划排产版本（PRODUCTION_VERSION），过滤结构可用机台 */
    private String productionVersion;
    /** 施工阶段：00 无工艺 / 01 试制 / 02 量试 / 03 正式（来自硫化任务） */
    private String constructionStage;

    // --- 需求量与计划量（注意单位）---
    /** 日需求量（条）- 早期净需求字段，部分路径仍作 quantity 回填兜底。 */
    private Integer demandQuantity;
    /** 已分配量（条，历史字段，均衡阶段较少使用） */
    private Integer assignedQuantity;
    /** 剩余待分配量（条，历史字段） */
    private Integer remainingQuantity;
    /** 待排产量（条）- R1 计算并整车取整后的计划量，主要用于日志与展示。 */
    private Integer plannedProduction;
    /** 【下游关键】最终待生产条数 - TaskGroupService 收尾/立库封顶后的实际排产量。 */
    private Integer endingExtraInventory;
    /** 【下游关键】硫化机台数 - DFS 均衡的负荷单位（非条数）。 */
    private Integer vulcanizeMachineCount;
    /** 硫化模数（单模/双模，产量换算用） */
    private Integer vulcanizeMoldCount;
    /** 硫化侧计划需求量（条，来自 LhScheduleResult，分组参考） */
    private Integer vulcanizeDemand;
    /** 需要的车数 = 待排条数 / 单车容量（精排波浪分配输入） */
    private Integer requiredCars;
    /** R2 暂存任务剩余需求（条） */
    private Integer deferredRemainingDemand;

    // --- 库存与优先级 ---
    /** 当前库存（条，任务级快照） */
    private Integer currentStock;
    /** 库存可供硫化时长（小时） */
    private BigDecimal stockHours;
    /** 库存是否高预警（>18 小时） */
    private Boolean isStockHighWarning;
    /** 三层优先级体系计算后的分值，越大越优先 */
    private Integer priority;
    /** 月计划优先级（排序辅助） */
    private Integer monthPlanPriority;
    /** 是否主销产品（影响收尾补整车/舍弃阈值） */
    private Boolean isMainProduct;

    // --- 任务类型标志 ---
    /** 是否试制任务（constructionStage=01） */
    private Boolean isTrialTask;
    /** 试制号 */
    private String trialNo;
    /** 是否量试任务（constructionStage=02） */
    private Boolean isProductionTrial;
    /** 是否续作任务 */
    private Boolean isContinueTask;
    /** 是否首任务/新开规格 */
    private Boolean isFirstTask;
    /** 是否新胎胚（无历史生产记录，排序用） */
    private Boolean isNewEmbryo;
    /** 量试约束机台 */
    private String constrainedMachineCode;
    /** 续作机台列表 */
    private List<String> continueMachineCodes;
    /** 推荐机台列表（结构排产配置，机台产能管控用） */
    private List<String> recommendedMachines;

    // --- 收尾属性 ---
    /** 是否收尾任务（剩余成型余量≤0） */
    private Boolean isEndingTask;
    /** 收尾余量（条） */
    private Integer endingSurplusQty;
    /** 硫化余量（条） */
    private Integer vulcanizeSurplusQty;
    /** 收尾日 */
    private LocalDate endingDate;
    /** 距收尾日天数 */
    private Integer daysToEnding;
    /** 是否紧急收尾（≤3 天或成型余量≤紧急阈值） */
    private Boolean isUrgentEnding;
    /** 是否近期收尾（≤10 天） */
    private Boolean isNearEnding;
    /** 是否收尾最后一批（影响精排是否补整车） */
    private Boolean isLastEndingBatch;
    /** 收尾是否被舍弃（非主销且余量≤舍弃阈值） */
    private Boolean endingAbandoned;
    /** 舍弃数量（条） */
    private Integer endingAbandonedQty;
    /** 是否需要月计划调整（满产追不上） */
    private Boolean needMonthPlanAdjust;
    /** 追赶量（条，平摊到未来天数） */
    private Integer catchUpQuantity;

    // --- 开停产与反推 ---
    /** 是否开产日相关任务 */
    private Boolean isOpeningDayTask;
    /** 是否停产日相关任务 */
    private Boolean isClosingDayTask;
    /** 开产首班产能封顶（条） */
    private Integer openingShiftCapacity;
    /** 是否关键产品且开产首班需跳过 */
    private Boolean isKeyProductOnOpening;
    /** 是否结束生产 */
    private Boolean isEndProduction;
    /** 停锅班次序号 */
    private Integer closingShiftOrder;
    /** 停产反推所需胎胚总量（条） */
    private Integer closingRequiredStock;
    /** 硫化开产班次序号 */
    private Integer lhOpeningShiftOrder;
    /** 成型开产班次序号（= 硫化开产 −1） */
    private Integer formingOpeningShiftOrder;

    // --- 精排与精度计划辅助 ---
    /** 机台小时产能（条/小时） */
    private Integer hourCapacity;
    /** 班次编码 -> 计划量 */
    private Map<String, Integer> shiftAllocation;
    /** 是否已被精度计划扣减产量 */
    private Boolean precisionDeducted;

    // ==================== 辅助方法 ====================

    /**
     * 将本任务转化为机台级任务分配对象。
     *
     * <p>除 {@code quantity} 和 {@code vulcanizeMachineCount} 由调用方传入外，
     * 其余字段均从本任务直接拷贝。
     *
     * @param quantity              计划条数（各调用方语义不同：续作=endingExtraInventory|demandQuantity，新增=同上，试制=plannedProduction）
     * @param vulcanizeMachineCount 硫化机台数
     * @return 填充完整的 TaskAllocation 对象
     */
    public TaskAllocation toTaskAllocation(int quantity, int vulcanizeMachineCount) {
        TaskAllocation ta = new TaskAllocation();
        ta.setEmbryoCode(this.embryoCode);
        ta.setMaterialCode(this.materialCode);
        ta.setMaterialDesc(this.materialDesc);
        ta.setMainMaterialDesc(this.mainMaterialDesc);
        ta.setStructureName(this.structureName);
        ta.setQuantity(quantity);
        ta.setVulcanizeMachineCount(vulcanizeMachineCount);
        ta.setEndingExtraInventory(this.endingExtraInventory);
        ta.setPriority(this.priority);
        ta.setStockHours(this.stockHours);
        ta.setIsTrialTask(this.isTrialTask);
        ta.setIsProductionTrial(this.isProductionTrial);
        ta.setIsContinueTask(this.isContinueTask);
        ta.setIsEndingTask(this.isEndingTask);
        ta.setEndingSurplusQty(this.endingSurplusQty);
        ta.setIsMainProduct(this.isMainProduct);
        ta.setLhId(this.lhId);
        ta.setLhMachineCode(this.lhMachineCode);
        ta.setIsLastEndingBatch(this.isLastEndingBatch);
        ta.setIsEndProduction(this.isEndProduction);
        ta.setEndingAbandoned(this.endingAbandoned);
        ta.setIsOpeningDayTask(this.isOpeningDayTask);
        ta.setIsClosingDayTask(this.isClosingDayTask);
        ta.setConstructionStage(this.constructionStage);
        ta.setIsFirstTask(this.isFirstTask);
        ta.setIsUrgentEnding(this.isUrgentEnding);
        ta.setIsNearEnding(this.isNearEnding);
        return ta;
    }
}
