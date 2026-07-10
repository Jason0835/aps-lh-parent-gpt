package com.zlt.aps.cx.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机台级任务分配 - {@link MachineAllocationResult} 内的最小排产单元。
 *
 * <p>由 ContinueTaskProcessor、NewTaskProcessor、TrialTaskProcessor 构建。
 *
 * <p><b>quantity 与 vulcanizeMachineCount</b> 不可互换：
 * quantity/endingExtraInventory = 条数；vulcanizeMachineCount = 本机承担该任务的硫化机台数。
 *
 * @author APS Team
 */
@Data
public class TaskAllocation {
    /** 胎胚编码 */
    private String embryoCode;
    /** 成品物料编码 */
    private String materialCode;
    /** 物料描述 */
    private String materialDesc;
    /** 胎胚/主物料描述 */
    private String mainMaterialDesc;
    /** 结构名称 */
    private String structureName;
    /** 计划条数（通常与 endingExtraInventory 一致） */
    private Integer quantity;
    /** 本机分配的硫化机台数（DFS assignedQty / 续作预留 1 台） */
    private Integer vulcanizeMachineCount;
    /** 优先级分值 */
    private Integer priority;
    /** 库存可供硫化时长（小时） */
    private BigDecimal stockHours;
    /** 硫化任务 ID */
    private Long lhId;
    /** 硫化机台编号（续作预扣去重用） */
    private String lhMachineCode;
    /** 施工阶段 00/01/02/03 */
    private String constructionStage;

    /** 是否试制 */
    private Boolean isTrialTask;
    /** 是否量试 */
    private Boolean isProductionTrial;
    /** 是否续作（含 ContinueTaskProcessor 保底预留） */
    private Boolean isContinueTask;
    /** 是否首任务/新开规格 */
    private Boolean isFirstTask;
    /** 是否主销 */
    private Boolean isMainProduct;

    /** 是否收尾任务 */
    private Boolean isEndingTask;
    /** 收尾余量（条） */
    private Integer endingSurplusQty;
    /** 【精排关键】实际待生产条数 */
    private Integer endingExtraInventory;
    /** 是否收尾最后一批 */
    private Boolean isLastEndingBatch;
    /** 收尾是否被舍弃 */
    private Boolean endingAbandoned;
    /** 是否紧急收尾 */
    private Boolean isUrgentEnding;
    /** 是否近期收尾 */
    private Boolean isNearEnding;

    /** 是否开产日任务 */
    private Boolean isOpeningDayTask;
    /** 是否停产日任务 */
    private Boolean isClosingDayTask;
    /** 是否已无需生产（反推满足） */
    private Boolean isEndProduction;
    /** 是否被精度计划扣量 */
    private Boolean precisionDeducted;
}
