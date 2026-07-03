package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.vo.ScheduleContextVo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 成型排程核心算法契约 + engine 层共享数据结构定义。
 *
 * <h3>职责边界</h3>
 * <ul>
 *   <li>本接口仅声明 {@link #executeSchedule} 入口；编排逻辑在
 *       {@link com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl}。</li>
 *   <li>内嵌 DTO（{@link DailyEmbryoTask} / {@link MachineAllocationResult} / {@link TaskAllocation}）
 *       供 TaskGroupService、各 Processor、BalancingService、ShiftScheduleService 跨类传递，避免循环依赖。</li>
 *   <li>业务调用链：{@link com.zlt.aps.cx.service.impl.ScheduleServiceImpl#executeSchedule}
 *       构建 {@link ScheduleContextVo} 后委托本接口执行。</li>
 * </ul>
 *
 * <h3>单班次流水线（{@code executeShiftSchedule} 内部，每班次执行一次）</h3>
 * <pre>
 * 5.2  TaskGroupService.groupTasks
 *        产出 DailyEmbryoTask × N → 按 continue / trial / new 分队列
 * 5.3.1 ContinueTaskProcessor     → List&lt;MachineAllocationResult&gt; continueAllocations（可选保底预留）
 * 5.3.2 TrialTaskProcessor      → trialAllocations
 * 5.3.3 NewTaskProcessor + BalancingService → newAllocations（DFS 按硫化机台数均衡）
 * 5.3.4 合并三类 MachineAllocationResult
 * 5.3.5 精度计划扣量（修改 TaskAllocation 数量）
 * 5.3.7 ShiftScheduleService    → 按 endingExtraInventory 条数精排到班次
 * </pre>
 *
 * <h3>数据结构生命周期</h3>
 * <pre>
 * LhScheduleResult + context
 *   → TaskGroupService 构建 DailyEmbryoTask（R1/R2/R3，写入 plannedProduction / endingExtraInventory / vulcanizeMachineCount）
 *   → Processor 层转为 MachineAllocationResult + TaskAllocation（机台维度）
 *   → ShiftScheduleService 读 TaskAllocation，重建精简 DailyEmbryoTask 做班次波浪分配
 *   → 汇总为 CxScheduleResult（机台+胎胚+物料，CLASS1~8 八班次排量）
 * </pre>
 *
 * <h3>关键单位（读代码前必读）</h3>
 * <table>
 *   <tr><th>字段</th><th>单位</th><th>消费方</th></tr>
 *   <tr><td>{@code vulcanizeMachineCount}</td><td>硫化机台数</td><td>BalancingService DFS 负荷；ContinueTaskProcessor 扣减剩余 demand</td></tr>
 *   <tr><td>{@code endingExtraInventory}</td><td>条（胎胚）</td><td>Processor 过滤、ShiftScheduleService 下量<b>唯一依据</b></td></tr>
 *   <tr><td>{@code plannedProduction}</td><td>条</td><td>R1 计划量展示/日志；精排<b>不</b>直接以此下量</td></tr>
 *   <tr><td>{@code TaskAllocation.quantity}</td><td>条</td><td>通常 = endingExtraInventory；精排优先读 endingExtraInventory</td></tr>
 *   <tr><td>{@code MachineAllocationResult.usedCapacity}</td><td>硫化机台数（DFS/续作预留）</td><td>机台负荷统计；与条数 dailyCapacity 语义不同层</td></tr>
 * </table>
 *
 * @author APS Team
 * @see TaskGroupService
 * @see com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl
 */
public interface CoreScheduleAlgorithmService {

    /**
     * 执行完整成型排程（多天多班次）。
     *
     * <p><b>外层循环</b>：按排程天/班次迭代（默认约 3 天、8 个班次），每天调用
     * {@code executeShiftSchedule} 完成「分组 → 三类 Processor → 精排」。
     *
     * <p><b>天间滚动</b>：每班次结束后更新 context（库存消耗、成型/硫化余量、在机胎胚映射等），
     * 供下一班次 TaskGroupService 使用。
     *
     * <p><b>输出聚合</b>：将各班次 {@link com.zlt.aps.cx.service.engine.ShiftScheduleService.ShiftProductionResult}
     * 按「机台 + 胎胚 + 物料」维度合并为 {@link CxScheduleResult}，
     * 每条记录的 CLASS1~CLASS8 对应该物料在 8 个班次上的计划条数。
     *
     * @param context 已由 ScheduleServiceImpl 加载完毕的排程上下文（机台/物料/参数/硫化任务/库存等）
     * @return 持久化前的排程结果列表
     */
    List<CxScheduleResult> executeSchedule(ScheduleContextVo context);

    // ==================== DailyEmbryoTask：单班次任务载体 ====================

    /**
     * 日胎胚任务 — 排程流水线中的<b>核心任务对象</b>。
     *
     * <p>语义：某排程日、某硫化任务（lhId）对应的一条成型待排记录；同一胎胚可因多物料/多任务出现多条实例。
     *
     * <p><b>写入方</b>：主要由 {@link TaskGroupService} 在 S5.2 构建与变异（R1 计划量、立库封顶、收尾处理、R2 轮询）。
     * <b>读取方</b>：ContinueTaskProcessor / TrialTaskProcessor / NewTaskProcessor / BalancingService /
     * ShiftScheduleService（精排前由 TaskAllocation 反构精简副本）。
     *
     * <p>字段按业务域分组见下方注释；带「下游关键」标记的字段修改时需同步检查 Processor 与精排逻辑。
     */
    @lombok.Data
    class DailyEmbryoTask {

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
        /** 月计划排产版本（PRODUCTION_VERSION），过滤结构可用机台 */
        private String productionVersion;
        /** 施工阶段：00 无工艺 / 01 试制 / 02 量试 / 03 正式（来自硫化任务） */
        private String constructionStage;

        // --- 需求量与计划量（注意单位）---
        /**
         * 日需求量（条）— 早期净需求字段，部分路径仍作 quantity 回填兜底。
         */
        private Integer demandQuantity;
        /** 已分配量（条，历史字段，均衡阶段较少使用） */
        private Integer assignedQuantity;
        /** 剩余待分配量（条，历史字段） */
        private Integer remainingQuantity;
        /**
         * 待排产量（条）— R1 计算并整车取整后的计划量，主要用于日志与展示。
         * <p>精排下量请使用 {@link #endingExtraInventory}，勿与本字段混用。
         */
        private Integer plannedProduction;
        /**
         * 【下游关键】最终待生产条数 — TaskGroupService 收尾/立库封顶后的<b>实际排产量</b>。
         * NewTaskProcessor/BalancingService 过滤 {@code ≤0}；ShiftScheduleService 按此下量。
         */
        private Integer endingExtraInventory;
        /**
         * 【下游关键】硫化机台数 — DFS 均衡的负荷单位（非条数）。
         * ContinueTaskProcessor 保底预留后递减；NewTaskProcessor 将 demand&gt;0 的续作并入均衡。
         */
        private Integer vulcanizeMachineCount;
        /** 硫化模数（单模/双模，产量换算用） */
        private Integer vulcanizeMoldCount;
        /** 硫化侧计划需求量（条，来自 LhScheduleResult，分组参考） */
        private Integer vulcanizeDemand;
        /** 需要的车数 = 待排条数 / 单车容量（精排波浪分配输入） */
        private Integer requiredCars;
        /**
         * R2 暂存任务剩余需求（条）— 零净需求/补充计划在第二轮一车一车分配时递减；
         * {@code >0} 表示仍参与结构级轮询。
         */
        private Integer deferredRemainingDemand;

        // --- 库存与优先级 ---
        /** 当前库存（条，任务级快照） */
        private Integer currentStock;
        /** 库存可供硫化时长（小时），由库存×单胎时长换算 */
        private BigDecimal stockHours;
        /** 库存是否高预警（&gt;18 小时） */
        private Boolean isStockHighWarning;
        /** 三层优先级体系计算后的分值，越大越优先 */
        private Integer priority;
        /** 月计划优先级（排序辅助） */
        private Integer monthPlanPriority;
        /** 是否主销产品（影响收尾补整车/舍弃阈值） */
        private Boolean isMainProduct;

        // --- 任务类型标志 ---
        /** 是否试制任务（constructionStage=01，受试制约束：SKU 上限/双数/班次） */
        private Boolean isTrialTask;
        /** 试制号 */
        private String trialNo;
        /** 是否量试任务（constructionStage=02；可锁定机台，不走试制数量约束） */
        private Boolean isProductionTrial;
        /** 是否续作任务（机台当前在产该胎胚；由 TaskGroupService/ContinueTaskProcessor 标记） */
        private Boolean isContinueTask;
        /** 是否首任务/新开规格（非续作、非试制、非量试） */
        private Boolean isFirstTask;
        /** 是否新胎胚（无历史生产记录，排序用） */
        private Boolean isNewEmbryo;
        /**
         * 量试约束机台 — 同胎胚已有试制分配时，量试只能上该机台；
         * 由 NewTaskProcessor 从 trialAllocations 写入。
         */
        private String constrainedMachineCode;
        /** 续作机台列表（历史字段，保底逻辑现以 machineOnlineEmbryoMap 为准） */
        private List<String> continueMachineCodes;
        /** 推荐机台列表（结构排产配置，机台产能管控用） */
        private List<String> recommendedMachines;

        // --- 收尾属性（TaskGroupService.calculateEndingInfo / handleEndingRemainder）---
        /** 是否收尾任务（剩余成型余量≤0） */
        private Boolean isEndingTask;
        /** 收尾余量（条）= 硫化余量 − 已分配胎胚库存口径 */
        private Integer endingSurplusQty;
        /** 硫化余量（条，来自动态月计划余量计算） */
        private Integer vulcanizeSurplusQty;
        /** 收尾日（该物料最迟需完成成型的日期） */
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

        // --- 开停产与反推（ScheduleDayTypeHelper + TaskGroupService 停产逻辑）---
        /** 是否开产日相关任务（班次类型 OPEN_START 等） */
        private Boolean isOpeningDayTask;
        /** 是否停产日相关任务 */
        private Boolean isClosingDayTask;
        /** 开产首班产能封顶（条，首班最多 6 小时产能） */
        private Integer openingShiftCapacity;
        /** 是否关键产品且开产首班需跳过（除非结构全为关键产品） */
        private Boolean isKeyProductOnOpening;
        /**
         * 是否结束生产 — 停产反推需求减库存≤0，本班及后续无需再排。
         */
        private Boolean isEndProduction;
        /** 停锅班次序号（dayShiftOrder，由硫化停锅时间与班次窗口计算） */
        private Integer closingShiftOrder;
        /** 停产反推所需胎胚总量（条，停锅前需满足的硫化消耗） */
        private Integer closingRequiredStock;
        /** 硫化开产班次序号 */
        private Integer lhOpeningShiftOrder;
        /** 成型开产班次序号（= 硫化开产 −1，提前一班） */
        private Integer formingOpeningShiftOrder;

        // --- 精排与精度计划辅助 ---
        /** 机台小时产能（条/小时，ShiftScheduleService 换算班次产量） */
        private Integer hourCapacity;
        /** 班次编码 → 计划量（中间结果，部分路径写入） */
        private Map<String, Integer> shiftAllocation;
        /** 是否已被精度计划扣减产量 */
        private Boolean precisionDeducted;
    }

    // ==================== MachineAllocationResult：机台维度分配容器 ====================

    /**
     * 单台成型机的任务分配结果 — Processor 层输出，精排层输入。
     *
     * <p>一台机一条记录，{@link #taskAllocations} 承载该机本班待精排的多个任务（不同胎胚/物料可并存）。
     *
     * <p><b>容量字段语义</b>：
     * <ul>
     *   <li>{@code usedCapacity} / {@code remainingCapacity} — 在续作预留与 DFS 路径上表示<b>硫化机台数</b>占用。</li>
     *   <li>{@code dailyCapacity} — 机台主数据日产能（条/天），ContinueTaskProcessor 初始化 remaining 时使用；
     *       与 DFS 机台 maxCapacity（最大硫化机数）不是同一套单位，阅读时需结合调用方。</li>
     * </ul>
     */
    @lombok.Data
    class MachineAllocationResult {
        /** 成型机台编码 */
        private String machineCode;
        /** 机台名称（展示） */
        private String machineName;
        /** 机型编码 */
        private String machineType;
        /** 日产能（条/天，来自机台主数据） */
        private Integer dailyCapacity;
        /** 已占用容量（硫化机台数或预留负荷，见调用方） */
        private Integer usedCapacity;
        /** 剩余容量 */
        private Integer remainingCapacity;
        /** 已分配胎胚种类数（DFS 种类槽统计） */
        private Integer assignedTypes;
        /** 本机待精排任务列表 */
        private List<TaskAllocation> taskAllocations;
        /** 当前结构（可选，部分路径标记机台所属结构） */
        private String currentStructure;
    }

    // ==================== TaskAllocation：机台上的单任务切片 ====================

    /**
     * 机台级任务分配 — {@link MachineAllocationResult} 内的最小排产单元。
     *
     * <p>由 ContinueTaskProcessor（续作预留）、NewTaskProcessor（DFS 结果转换）、TrialTaskProcessor 构建。
     * {@link com.zlt.aps.cx.service.impl.CoreScheduleAlgorithmServiceImpl} 精排前将其字段拷贝回
     * {@link DailyEmbryoTask}，并优先用 {@link #endingExtraInventory} 作为实际条数。
     *
     * <p><b>quantity 与 vulcanizeMachineCount</b> 不可互换：
     * quantity/endingExtraInventory = 条数；vulcanizeMachineCount = 本机承担该任务的硫化机台数。
     */
    @lombok.Data
    class TaskAllocation {
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
        /** 计划条数（通常与 endingExtraInventory 一致；精排优先读 endingExtraInventory） */
        private Integer quantity;
        /** 本机分配的硫化机台数（DFS assignedQty / 续作预留 1 台） */
        private Integer vulcanizeMachineCount;
        /** 优先级分值 */
        private Integer priority;
        /** 库存可供硫化时长（小时） */
        private BigDecimal stockHours;
        /** 硫化任务 ID */
        private Long lhId;
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

    // ==================== ShiftAllocationResult：班次汇总（扩展用） ====================

    /**
     * 机台班次计划汇总结构。
     *
     * <p>当前实现中精排结果以 {@link com.zlt.aps.cx.service.engine.ShiftScheduleService.ShiftProductionResult}
     * 为主路径输出；本类保留作班次维度聚合的扩展占位，代码库内暂无引用。
     */
    @lombok.Data
    class ShiftAllocationResult {
        /** 机台编码 */
        private String machineCode;
        /** 机台名称 */
        private String machineName;
        /** 班次编码 → 计划条数 */
        private Map<String, Integer> shiftPlanQty;
        /** 关联的任务分配列表 */
        private List<TaskAllocation> tasks;
    }
}
