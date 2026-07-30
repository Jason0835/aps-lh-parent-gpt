package com.zlt.aps.gsq.engine.context;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.domain.GsqMachineCandidate;
import com.zlt.aps.gsq.engine.domain.GsqRuleTrace;
import com.zlt.aps.gsq.engine.domain.GsqSnapshotBuildResult;
import com.zlt.aps.gsq.engine.vo.GsqMonthSurplusVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTaskNode;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈排程上下文。
 *
 * <p>贯穿一次钢丝圈排程从 S1 到 S6 的可变数据总线。</p>
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
public class GsqScheduleContext {

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

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：GSQ+年月日+3位定长自增序号 */
    private String batchNo;

    /** 对应的胎圈批次号 */
    private String tqBatchNo;

    /** 工序参数 */
    private GsqScheduleParams params;

    /** 外协规格Map，key=钢丝圈代码，value="1" */
    private Map<String, String> assistSpecMap = new HashMap<>();

    /** 限制作业映射，key=钢丝圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyCanMachineMap = new HashMap<>();

    /** 不可作业映射，key=钢丝圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyNotMachineMap = new HashMap<>();

    /** 当日库存（6点MES库存），key=钢丝圈代码，value=库存量 */
    private Map<String, Double> stockMap = new HashMap<>();

    /** 预计库存，key=钢丝圈代码，value=预计库存量 */
    private Map<String, Double> planStockMap = new HashMap<>();

    /** 前日早班计划量，key=钢丝圈代码，value=前日2班计划量（昨日2班剩余库存来源） */
    private Map<String, Double> lastMidPlanMap = new HashMap<>();

    /** 损耗率映射，key=钢丝圈代码，value=损耗率 */
    private Map<String, Double> lossRateMap = new HashMap<>();

    /** 月度剩余，key=钢丝圈代码 */
    private Map<String, GsqMonthSurplusVo> monthSurplusMap = new HashMap<>();

    /** 全部机台列表 */
    private List<GsqMachineInfo> allMachineList = new ArrayList<>();

    /** 机台寸口映射，key=机台编号，value=该机台可做的寸口值列表 */
    private Map<String, List<java.math.BigDecimal>> machineChuckMap = new HashMap<>();

    /** 机台钢丝直径映射（钢丝圈独有），key=机台编号，value=该机台支持的钢丝直径列表 */
    private Map<String, List<String>> machineWireDiameterMap = new HashMap<>();

    /** 机台产线规则映射（钢丝圈独有），key=机台编号，value=产线编号(1/2/3/4) */
    private Map<String, Integer> machineProductionLineMap = new HashMap<>();

    /** 工装车总数（钢丝圈独有） */
    private Integer cartTotalCount;

    /** 工装车整车容量，key=钢丝圈编码, value=整车容量（默认120） */
    private Map<String, Integer> cartCapacityMap = new HashMap<>();

    /** 可用工装车数（库存扣除后剩余） */
    private Integer availableCartCount;

    /** 检修计划机台，key=日期班次(如"2025-01-01|03"), value=该班次检修中的机台编号列表。班次编码使用两位格式：01=夜班,02=早班,03=中班 */
    private Map<String, List<String>> maintenanceMachineMap = new HashMap<>();

    /** 钢丝圈-胎圈关联关系，key=钢丝圈编码, value=关联胎圈编码列表（一个钢丝圈可能对应多个胎圈） */
    private Map<String, List<String>> steelRingTireRingMap = new HashMap<>();

    /** 钢丝圈-胎胚关联关系，key=钢丝圈编码, value=关联胎胚编码列表 */
    private Map<String, List<String>> steelRingEmbryoMap = new HashMap<>();

    /** BOM分解结果，key=钢丝圈编码, value=对应胎圈的BOM用量(默认1) */
    private Map<String, Double> bomDecomposeMap = new HashMap<>();

    /** 钢丝直径映射（钢丝圈独有），key=钢丝圈编码, value=钢丝直径 */
    private Map<String, String> wireDiameterMap = new HashMap<>();

    /** 胎圈6班次排程结果，key=胎圈代码, value=Map<班次号(1~6), 计划量> */
    private Map<String, Map<Integer, Double>> tq6ShiftResultMap = new HashMap<>();

    /** 胎圈停产班次，key=日期班次(如"2025-01-01|03"), value=true表示胎圈停产 */
    private Map<String, Boolean> tqStopShiftMap = new HashMap<>();

    /** 钢丝圈停产班次，key=日期班次(如"2025-01-01|03"), value=true表示钢丝圈停产 */
    private Map<String, Boolean> gsqStopShiftMap = new HashMap<>();

    /** 各规格各班次机台定额总产能，key=钢丝圈编码, value=Map<班次号(1~6), 定额总产能> */
    private Map<String, Map<Integer, Double>> specClassQuotaMap = new HashMap<>();

    // ========== 结构化规则证据（贯穿 S2~S6，S6 持久化时写入解释 JSON 字段） ==========

    /**
     * 规则命中证据，key=钢丝圈编码（steelRingCode），value=该规格的规则证据集合。
     *
     * <p>由各 Handler 在关键决策点（BOM 分解、备库触发、机台过滤命中、停产协调等）追加证据，
     * S6 持久化阶段统一调用 {@link GsqRuleTrace#toExplainJson()} 序列化为 JSON 文本写入排程结果表解释字段。</p>
     */
    private Map<String, GsqRuleTrace> ruleTraceMap = new HashMap<>();

    /**
     * 候选机台追踪，key=钢丝圈编码（steelRingCode），value=该规格最后一次机台分配的候选机台列表（含被过滤机台）。
     *
     * <p>Phase 3 重构新增：由 {@code GsqMachineAssignHandler} 在 S3 阶段写入，
     * 记录每个候选机台的过滤状态、过滤原因、任务链长度和选中评分，供解释快照输出候选机台详情。</p>
     *
     * <p>注意：同一 steelRingCode 在不同班次可能多次分配机台，
     * 后一次写入会覆盖前一次。如需保留所有班次的候选机台历史，应在写入前复制快照。</p>
     */
    private Map<String, List<GsqMachineCandidate>> candidateTraceMap = new HashMap<>();

    /** 任务链，key=机台编号, value=该机台的任务链（按班次顺序排列） */
    private Map<String, LinkedList<GsqTaskNode>> taskChainMap = new HashMap<>();

    /**
     * 机台班次任务链集合（结构化任务链）。
     *
     * <p>Phase 5 重构新增：对齐胎圈 {@code TqScheduleContext.taskChainGroup}，承载所有机台的任务链，
     * 支持追加、前插、插单、删除、转机台、调量等结构化操作，替代 {@link #taskChainMap} 的简单链表场景。</p>
     *
     * <p>与胎圈的差异：钢丝圈按"机台+日期"分组链表（不按班次分链），链内通过
     * {@link ScheduleTaskNode#getShiftOrder()} 区分班次顺序。</p>
     *
     * <p>兼容策略：保留 {@link #taskChainMap} 不删除，原有 {@code GsqMachineAssignHandler.assignMachine}
     * 继续使用旧字段；新代码（如人工插单门面、解释快照）使用本字段。后续可逐步迁移。</p>
     */
    private MachineShiftTaskChain<GsqTaskNode> taskChainGroup = new MachineShiftTaskChain<>();

    /**
     * 任务链节点索引，key=任务标识（businessKey）。
     *
     * <p>Phase 5 重构新增：对齐胎圈 {@code TqScheduleContext.taskNodeIndex}，提供 O(1) 节点查找，
     * 避免任务链操作时遍历所有机台链表。</p>
     *
     * <p>由 {@code GsqTaskChainScheduleService} 在节点加入任务链时通过 {@link #registerTaskNode} 注册，
     * 节点删除时通过 {@link #removeTaskNode} 注销。</p>
     */
    private Map<String, ScheduleTaskNode<GsqTaskNode>> taskNodeIndex = new HashMap<>();

    // ========== Phase 4 重构新增：解释快照、质量汇总 ==========

    /**
     * 解释快照，key=钢丝圈编码（steelRingCode），value=该规格的解释快照。
     *
     * <p>由 {@code GsqSnapshotBuildService} 在 S6 阶段构建，包含规则命中、候选机台、未排证据、
     * 异常等多元字段，序列化为 JSON 后写入 {@code T_GSQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段。</p>
     */
    private Map<String, GsqSnapshotBuildResult> snapshotMap = new HashMap<>();

    /**
     * 本次自动排程质量指标摘要。
     *
     * <p>由 {@code GsqScheduleQualitySummaryService} 在 S6 阶段统一计算，包含核心指标：
     * taskCount/resultCount/unplannedCount/coverageRate/unplannedRate/machineUtilizationRate/
     * switchCount/stockGuaranteeRate/tailCompletionRate/shiftCapacityHitRate。</p>
     */
    private Map<String, Object> qualitySummary = new LinkedHashMap<>();

    /** 排程基础信息Map，key=钢丝圈代码，value=施工表关联信息 */
    private Map<String, GsqScheduleBaseInfoVo> scheduleBaseInfoMap = new HashMap<>();

    /** 施工信息列表（S1校验用） */
    private List<EngineConstructionInfo> constructionInfoList = new ArrayList<>();

    // ========== S1+S2写入 → S3/S4消费 ==========

    /**
     * 排程基础数据列表。
     * S1写入基础数据，S2修改计划量字段，S3修改machineCode字段，S4读取并持久化。
     */
    private List<GsqScheduleResultVo> scheduleList = new ArrayList<>();

    // ========== S2写入 → S3/S4消费 ==========

    /** 总计划量统计（6班次） */
    private GsqTotalPlanQtyVo totalPlanQtyVo = new GsqTotalPlanQtyVo();

    /** 末班估值缓存：胎圈7班消耗量，key=钢丝圈代码，value=估值 */
    private Map<String, Double> lastShiftEstimateMap = new HashMap<>();

    // ========== S4写入 ==========

    /** 已有排程记录（当天已存在的排产记录） */
    private List<GsqScheduleResultVo> existScheduleList = new ArrayList<>();

    /** 插入记录数 */
    private int insertedCount;

    // ========== S3阶段临时传递字段 ==========

    /**
     * 当前正在排产的班次编码（CLASS_NUM_THREE字典值："01"=夜班, "02"=早班, "03"=中班）。
     * 由GsqMachineAssignHandler在策略链过滤前设置，
     * 供MaintenanceFilter等策略按班次精确过滤维修机台使用。
     */
    private String currentClassCode;

    /** 当前正在排产的班次索引(1~6) */
    private int currentClassIndex;

    // ========== 流程控制 ==========

    /** 是否中断排程 */
    private boolean interrupted = false;

    /** 中断原因 */
    private String interruptReason;

    /** 当前执行步骤 */
    private String currentStep;

    /** 校验错误信息集合 */
    private List<String> validationErrors = new ArrayList<>();

    /** 排程开始时间 */
    private Date startTime;

    /** 排程结束时间 */
    private Date endTime;

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
    public void setRuleTraceMap(Map<String, GsqRuleTrace> ruleTraceMap) {
        this.ruleTraceMap = ruleTraceMap == null ? new HashMap<>() : ruleTraceMap;
    }

    /**
     * 设置候选机台追踪 Map（null 保护，避免外部传入 null 污染后续步骤）。
     *
     * <p>Phase 3 重构新增。</p>
     *
     * @param candidateTraceMap 候选机台追踪 Map
     */
    public void setCandidateTraceMap(Map<String, List<GsqMachineCandidate>> candidateTraceMap) {
        this.candidateTraceMap = candidateTraceMap == null ? new HashMap<>() : candidateTraceMap;
    }

    /**
     * 设置解释快照 Map（null 保护，避免外部传入 null 污染后续步骤）。
     *
     * <p>Phase 4 重构新增。</p>
     *
     * @param snapshotMap 解释快照 Map
     */
    public void setSnapshotMap(Map<String, GsqSnapshotBuildResult> snapshotMap) {
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
     * 获取指定钢丝圈规格的规则证据对象，不存在时自动创建并放入 Map。
     *
     * <p>使用方式：</p>
     * <pre>
     * GsqRuleTrace trace = context.getRuleTrace(steelRingCode);
     * trace.addRuleHit(GsqScheduleRuleCodeEnum.MACHINE_FILTER, GsqScheduleRuleResultEnum.HIT, evidenceMap);
     * </pre>
     *
     * @param steelRingCode 钢丝圈编码
     * @return 规则证据对象（永不为 null）
     */
    public GsqRuleTrace getRuleTrace(String steelRingCode) {
        if (StringUtils.isBlank(steelRingCode)) {
            return new GsqRuleTrace();
        }
        return ruleTraceMap.computeIfAbsent(steelRingCode, k -> new GsqRuleTrace());
    }

    // ========== Phase 5 重构新增：任务链节点索引管理 ==========

    /**
     * 注册任务链节点到索引。
     *
     * <p>由 {@code GsqTaskChainScheduleService} 在节点加入任务链时调用，
     * 同一 taskId 重复注册时以最新节点覆盖旧节点引用。</p>
     *
     * @param taskId 任务标识（对应 {@link ScheduleTaskNode#getTaskId()}）
     * @param node  任务链节点
     */
    public void registerTaskNode(String taskId, ScheduleTaskNode<GsqTaskNode> node) {
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
    public ScheduleTaskNode<GsqTaskNode> getTaskNode(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        return taskNodeIndex.get(taskId);
    }
}
