package com.zlt.aps.tq.engine.context;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.engine.domain.TqMachineCandidate;
import com.zlt.aps.tq.engine.domain.TqPersistResult;
import com.zlt.aps.tq.engine.domain.TqRuleTrace;
import com.zlt.aps.tq.engine.domain.TqSnapshotBuildResult;
import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTaskNode;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈排程上下文。
 *
 * <p>贯穿一次胎圈排程从 S1 到 S6 的可变数据总线。</p>
 *
 * <p>字段按阶段标注数据流向：</p>
 * <ul>
 *   <li>S1写入 → S2/S3/S4/S5/S6消费：基础数据（库存、机台、损耗率等）</li>
 *   <li>S2写入 → S3/S4/S5/S6消费：中间计算结果（计划量、供应时长等）</li>
 *   <li>S3写入 → S4/S5/S6消费：机台分配结果（直接修改scheduleList中的machineCode）</li>
 *   <li>S4写入 → S5/S6消费：停产协调结果</li>
 *   <li>S5写入 → S6消费：均衡调整结果</li>
 *   <li>S6写入：持久化结果</li>
 * </ul>
 *
 * <p>注意：该对象会被多个Handler原地修改。新增字段时必须同时确认
 * 初始化入口和消费位置，避免字段只有写入没有消费。</p>
 *
 * @author APS
 */
@Data
public class TqScheduleContext {

    // ========== 排程入参（外部传入） ==========

    /** 排程日期，格式：yyyy-MM-dd */
    private String scheduleDate;

    /** 分厂编码 */
    private String factoryCode;

    /** 操作人 */
    private String operator;

    /**
     * 排程追踪标识。
     *
     * <p>Phase 5 重构新增：用于任务链操作日志串联，与 {@link MachineShiftTaskChain} 中
     * 的 {@code ScheduleOperationContext.traceId} 共享，便于跨服务追踪同一次排程的任务链变更。</p>
     */
    private String traceId;

    // ========== S1写入 → S2/S3/S4消费 ==========

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    private String batchNo;

    /** 对应的成型批次号 */
    private String cxBatchNo;

    /** 工序参数（13项） */
    private TqScheduleParams params;

    /**
     * 外协规格Map，key=胎圈代码，value="1"
     *
     * @deprecated 外协规格逻辑已废弃（2026-06-27），6班次排程不再区分外协/非外协。
     *             字段保留仅为兼容已有 getter/setter 调用，不再被写入和读取。
     */
    @Deprecated
    private Map<String, String> assistSpecMap = new HashMap<>();

    /** 口型板→机台映射，key=口型板代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> mouthPlateMachineMap = new HashMap<>();

    /** 限制作业映射，key=胎圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyCanMachineMap = new HashMap<>();

    /** 不可作业映射，key=胎圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyNotMachineMap = new HashMap<>();

    /** 当日库存，key=胎圈代码，value=库存量 */
    private Map<String, Double> stockMap = new HashMap<>();

    /** 预计库存，key=胎圈代码，value=预计库存量 */
    private Map<String, Double> planStockMap = new HashMap<>();

    /** 当天早班(D日早班)计划量，key=胎圈代码，value=计划量（昨天已排的、属于今天早班的胎圈计划量） */
    private Map<String, Double> todayMorningPlanMap = new HashMap<>();

    /** 损耗率映射，key=机台#胎圈代码，value=损耗率（LOSS_RATE字段原值，如0.01表示1%） */
    private Map<String, Double> lossRateMap = new HashMap<>();

    /**
     * 按胎圈代码聚合的损耗率映射，key=胎圈代码，value=损耗率（LOSS_RATE字段原值，如0.01表示1%）
     * <p>S2阶段（计划量计算）机台尚未分配，无法按 机台#胎圈 精确查询，
     * 需用此Map按胎圈代码查询。聚合规则：同一胎圈在多机台损耗率一致时直接取，不一致时取平均值。</p>
     */
    private Map<String, Double> beadLossRateMap = new HashMap<>();

    /** 月度剩余，key=胎圈代码 */
    private Map<String, TqMonthSurplusVo> monthSurplusMap = new HashMap<>();

    /** 全部机台列表 */
    private List<TqMachineInfo> allMachineList = new ArrayList<>();

    /** 机台寸口映射，key=机台编号，value=该机台可做的寸口值列表（来自TqMachineChuck） */
    private Map<String, List<java.math.BigDecimal>> machineChuckMap = new HashMap<>();

    /** 工装车整车容量，key=胎圈编码, value=整车容量 */
    private Map<String, Integer> cartCapacityMap = new HashMap<>();

    /** 检修计划机台，key=日期班次(如"2025-01-01|03"), value=该班次检修中的机台编号列表。班次编码使用两位格式：01=夜班,02=早班,03=中班 */
    private Map<String, List<String>> maintenanceMachineMap = new HashMap<>();

    /** 胎圈-胎胚关联关系，key=胎圈编码, value=关联胎胚编码列表（一个胎圈可能对应多个胎胚） */
    private Map<String, List<String>> beadEmbryoMap = new HashMap<>();

    /** 胎圈备库班数配置列表（按工厂过滤），S2阶段匹配用 */
    private List<TqStockShiftConfig> stockShiftConfigList = new ArrayList<>();

    /** 胎圈规格→成型机台数映射，key=胎圈编码, value=正在生产该胎圈规格的成型机台数量 */
    private Map<String, Integer> beadMachineCountMap = new HashMap<>();

    /** 成型停产班次，key=日期班次(如"2025-01-01|中班"), value=true表示成型停产 */
    private Map<String, Boolean> cxStopShiftMap = new HashMap<>();

    /** 胎圈停产班次（区别于成型停产），key=日期班次(如"2025-01-01|中班"), value=true表示胎圈停产 */
    private Map<String, Boolean> tqStopShiftMap = new HashMap<>();

    /** 各规格各班次机台定额总产能，key=胎圈编码, value=Map<班次号(1~6), 定额总产能> */
    private Map<String, Map<Integer, Double>> specClassQuotaMap = new HashMap<>();

    /** 任务链，key=机台编号, value=该机台的任务链（按班次顺序排列） */
    private Map<String, java.util.LinkedList<TqTaskNode>> taskChainMap = new HashMap<>();

    /**
     * 机台班次任务链集合（结构化任务链）。
     *
     * <p>Phase 5 重构新增：对齐胎侧 {@code TcScheduleContext.taskChainGroup}，承载所有机台的任务链，
     * 支持追加、前插、插单、删除、转机台、调量等结构化操作，替代 {@link #taskChainMap} 的简单链表场景。</p>
     *
     * <p>与胎侧的差异：胎圈按"机台+日期"分组链表（不按班次分链），链内通过
     * {@link ScheduleTaskNode#getShiftOrder()} 区分班次顺序；胎侧按"机台+日期+班次"分组链表，
     * 每个班次一条独立链表。</p>
     *
     * <p>兼容策略：保留 {@link #taskChainMap} 不删除，原有 {@code TqMachineAssignHandler.buildTaskChain}
     * 继续使用旧字段；新代码（如人工插单门面、解释快照）使用本字段。后续可逐步迁移。</p>
     */
    private MachineShiftTaskChain<TqTaskNode> taskChainGroup = new MachineShiftTaskChain<>();

    /**
     * 任务链节点索引，key=任务标识（businessKey）。
     *
     * <p>Phase 5 重构新增：对齐胎侧 {@code TcScheduleContext.taskNodeIndex}，提供 O(1) 节点查找，
     * 避免任务链操作时遍历所有机台链表。</p>
     *
     * <p>由 {@code TqTaskChainScheduleService} 在节点加入任务链时通过 {@link #registerTaskNode} 注册，
     * 节点删除时通过 {@link #removeTaskNode} 注销。</p>
     */
    private Map<String, ScheduleTaskNode<TqTaskNode>> taskNodeIndex = new HashMap<>();

    // ========== S1+S2写入 → S3/S4消费 ==========

    /**
     * 排程基础数据列表。
     * S1写入基础数据，S2修改计划量字段，S3修改machineCode字段，S4读取并持久化。
     */
    private List<TqScheduleResultVo> scheduleList = new ArrayList<>();

    // ========== S2写入 → S3/S4消费 ==========

    /** 总计划量统计（中班/夜班/白班/次日中班） */
    private TqTotalPlanQtyVo totalPlanQtyVo = new TqTotalPlanQtyVo();

    // ========== 结构化规则证据（贯穿 S2~S6，S6 持久化时写入解释 JSON 字段） ==========

    /**
     * 规则命中证据，key=胎圈编码（beadCode），value=该规格的规则证据集合。
     *
     * <p>由各 Handler 在关键决策点（备库触发、收尾判断、计划量计算、机台过滤命中、停产协调等）追加证据，
     * S6 持久化阶段统一调用 {@link TqRuleTrace#toExplainJson()} 序列化为 JSON 文本写入排程结果表解释字段。</p>
     */
    private Map<String, TqRuleTrace> ruleTraceMap = new HashMap<>();

    /**
     * 候选机台追踪，key=胎圈编码（beadCode），value=该规格最后一次机台分配的候选机台列表（含被过滤机台）。
     *
     * <p>Phase 3 重构新增：由 {@code TqMachineAssignHandler.searchOptionalMachineList} 在 S3 阶段写入，
     * 记录每个候选机台的过滤状态、过滤原因、剩余产能和选中评分，供 Phase 4 解释快照输出候选机台详情。</p>
     *
     * <p>注意：同一 beadCode 在不同班次可能多次调用 {@code searchOptionalMachineList}，
     * 后一次写入会覆盖前一次。如需保留所有班次的候选机台历史，应在写入前复制快照。</p>
     */
    private Map<String, List<TqMachineCandidate>> candidateTraceMap = new HashMap<>();

    // ========== Phase 4 重构新增：解释快照、质量汇总、持久化汇总 ==========

    /**
     * 解释快照，key=胎圈编码（beadCode），value=该规格的解释快照。
     *
     * <p>由 {@code TqSnapshotBuildService} 在 S6 阶段构建，包含规则命中、候选机台、未排证据、
     * 异常等多元字段，序列化为 JSON 后写入 {@code T_TQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段。</p>
     */
    private Map<String, TqSnapshotBuildResult> snapshotMap = new HashMap<>();

    /**
     * 本次自动排程质量指标摘要。
     *
     * <p>由 {@code TqScheduleQualitySummaryService} 在 S6 阶段统一计算，包含 10 项核心指标：
     * taskCount/resultCount/unplannedCount/coverageRate/unplannedRate/machineUtilizationRate/
     * switchCount/stockGuaranteeRate/tailCompletionRate/shiftCapacityHitRate。</p>
     */
    private Map<String, Object> qualitySummary = new LinkedHashMap<>();

    /**
     * 本次落库汇总结果。
     *
     * <p>由 {@code TqResultPersistHandler} 在 S6 阶段填充，承载结果数、解释数、未排数和异常数等汇总信息。</p>
     */
    private TqPersistResult persistResult;

    // ========== S4写入 ==========

    /**
     * 外协排程数据
     *
     * @deprecated 外协规格逻辑已废弃（2026-06-27），6班次排程不再区分外协/非外协。
     *             字段保留仅为兼容已有 getter/setter 调用，不再被写入和读取。
     */
    @Deprecated
    private List<TqScheduleResultVo> assistScheduleList = new ArrayList<>();

    /**
     * 非外协排程数据
     *
     * @deprecated 外协规格逻辑已废弃（2026-06-27），6班次排程不再区分外协/非外协。
     *             字段保留仅为兼容已有 getter/setter 调用，不再被写入和读取。
     */
    @Deprecated
    private List<TqScheduleResultVo> normalScheduleList = new ArrayList<>();

    /** 已有排程记录（当天已存在的排产记录） */
    private List<TqScheduleResultVo> existScheduleList = new ArrayList<>();

    /** 插入记录数 */
    private int insertedCount;

    // ========== S3阶段临时传递字段 ==========

    /**
     * 当前正在排产的班次编码（CLASS_NUM_THREE字典值："01"=夜班, "02"=早班, "03"=中班）。
     * 由TqMachineAssignHandler.searchOptionalMachineList在策略链过滤前设置，
     * 供MaintenanceFilter等策略按班次精确过滤维修机台使用。
     */
    private String currentClassCode;

    // ========== 流程控制 ==========

    /** 是否中断排程 */
    private boolean interrupted = false;

    /** 中断原因 */
    private String interruptReason;

    /** 当前执行步骤 */
    private String currentStep;

    /** 校验错误信息集合 */
    private List<String> validationErrors = new ArrayList<>();

    /**
     * 中断排程流程
     *
     * @param reason 中断原因
     */
    public void interruptSchedule(String reason) {
        this.interrupted = true;
        this.interruptReason = reason;
    }

    /**
     * 追加一条校验错误信息（空串或null将被忽略）
     *
     * @param message 错误描述
     */
    public void addValidationError(String message) {
        if (StringUtils.isNotEmpty(message)) {
            this.validationErrors.add(message);
        }
    }

    /**
     * 设置规则证据 Map（null 保护，避免外部传入 null 污染后续步骤）。
     *
     * @param ruleTraceMap 规则证据 Map
     */
    public void setRuleTraceMap(Map<String, TqRuleTrace> ruleTraceMap) {
        this.ruleTraceMap = ruleTraceMap == null ? new HashMap<>() : ruleTraceMap;
    }

    /**
     * 设置候选机台追踪 Map（null 保护，避免外部传入 null 污染后续步骤）。
     *
     * <p>Phase 3 重构新增。</p>
     *
     * @param candidateTraceMap 候选机台追踪 Map
     */
    public void setCandidateTraceMap(Map<String, List<TqMachineCandidate>> candidateTraceMap) {
        this.candidateTraceMap = candidateTraceMap == null ? new HashMap<>() : candidateTraceMap;
    }

    /**
     * 设置解释快照 Map（null 保护，避免外部传入 null 污染后续步骤）。
     *
     * <p>Phase 4 重构新增。</p>
     *
     * @param snapshotMap 解释快照 Map
     */
    public void setSnapshotMap(Map<String, TqSnapshotBuildResult> snapshotMap) {
        this.snapshotMap = snapshotMap == null ? new HashMap<>() : snapshotMap;
    }

    /**
     * 设置质量指标摘要 Map（null 保护，避免外部传入 null 污染后续步骤）。
     *
     * <p>Phase 4 重构新增。</p>
     *
     * @param qualitySummary 质量指标摘要 Map
     */
    public void setQualitySummary(Map<String, Object> qualitySummary) {
        this.qualitySummary = qualitySummary == null ? new LinkedHashMap<>() : qualitySummary;
    }

    /**
     * 获取指定胎圈规格的规则证据对象，不存在时自动创建并放入 Map。
     *
     * <p>使用方式：</p>
     * <pre>
     * TqRuleTrace trace = context.getRuleTrace(beadCode);
     * trace.addRuleHit(TqScheduleRuleCodeEnum.BACKUP_TRIGGER, TqScheduleRuleResultEnum.TRIGGER, evidenceMap);
     * </pre>
     *
     * @param beadCode 胎圈编码
     * @return 规则证据对象（永不为 null）
     */
    public TqRuleTrace getRuleTrace(String beadCode) {
        if (StringUtils.isBlank(beadCode)) {
            // 无效 beadCode 时返回临时证据对象（不放入 Map，避免污染）
            return new TqRuleTrace();
        }
        return ruleTraceMap.computeIfAbsent(beadCode, k -> new TqRuleTrace());
    }

    // ========== Phase 5 重构新增：任务链节点索引管理 ==========

    /**
     * 注册任务链节点到索引。
     *
     * <p>由 {@code TqTaskChainScheduleService} 在节点加入任务链时调用，
     * 同一 taskId 重复注册时以最新节点覆盖旧节点引用。</p>
     *
     * @param taskId 任务标识（对应 {@link ScheduleTaskNode#getTaskId()}）
     * @param node  任务链节点
     */
    public void registerTaskNode(String taskId, ScheduleTaskNode<TqTaskNode> node) {
        if (StringUtils.isBlank(taskId) || node == null) {
            return;
        }
        taskNodeIndex.put(taskId, node);
    }

    /**
     * 从索引中注销任务链节点。
     *
     * @param taskId 任务标识
     */
    public void removeTaskNode(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        taskNodeIndex.remove(taskId);
    }

    /**
     * 根据任务标识获取任务链节点。
     *
     * @param taskId 任务标识
     * @return 任务链节点；未注册时返回 null
     */
    public ScheduleTaskNode<TqTaskNode> getTaskNode(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        return taskNodeIndex.get(taskId);
    }
}
