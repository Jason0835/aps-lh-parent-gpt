package com.zlt.aps.tc.engine.domain;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.*;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerSnapshot;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.service.TcAutoScheduleProgressListener;
import com.zlt.aps.tc.engine.service.collector.TcAutoScheduleIssueCollector;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * 胎侧排程上下文。
 *
 * <p>贯穿一次胎侧自动排程的运行态数据总线，承载批次、追踪号、排程日期、操作人、
 * 参数快照、待排任务和机台班次任务链。该对象会被步骤服务按流程原地补充数据。</p>
 */
@Data
public class TcScheduleContext {

    /** 本次异步自动排程任务编号 */
    private String taskId;

    /** 工厂编号 */
    private String factoryCode;

    /** 批次号 */
    private String batchNo;

    /** 追踪标识 */
    private String traceId;

    /** 本次自动排程中文过程日志缓冲。 */
    private ScheduleProcessTraceBuffer processTraceBuffer = new ScheduleProcessTraceBuffer();

    /** 排程日期 */
    private Date scheduleDate;

    /** 操作人 */
    private String operator;

    /** 参数快照，key=参数编码 */
    private Map<String, TcParamValue> paramMap = new HashMap<>();

    /** 待排任务草稿列表 */
    private List<TcTaskDraft> taskDraftList = new ArrayList<>();

    /** 计划量汇总前的原始成型来源任务快照列表 */
    private List<TcTaskDraft> sourceTaskDraftList = new ArrayList<>();

    /** 同胎侧同班次计划量汇总组，key=计划量汇总组业务键 */
    private Map<String, TcPlanTaskGroup> planTaskGroupMap = new LinkedHashMap<>();

    /** 机台班次任务链集合 */
    private MachineShiftTaskChain<TcTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();

    /** 任务链节点索引，key=任务ID或业务键 */
    private Map<String, ScheduleTaskNode<TcTaskDraft>> taskNodeIndex = new HashMap<>();

    /** 解释快照，key=任务业务键 */
    private Map<String, TcSnapshotBuildResult> snapshotMap = new HashMap<>();

    /** 规则证据，key=任务业务键 */
    private Map<String, TcRuleTrace> ruleTraceMap = new HashMap<>();

    /** FULL 日志已消费的规则证据游标，key=任务业务键，value=已写入条数。 */
    private Map<String, Integer> processRuleTraceCursorMap = new HashMap<>();

    /** 当前正在执行的中文步骤名，失败日志用于指出中断位置。 */
    private String currentProcessStep;

    /** 已成功完成的中文步骤名。 */
    private List<String> completedProcessSteps = new ArrayList<>();

    /** 本次落库转换汇总 */
    private TcPersistResult persistResult;

    /** 本次自动排程质量指标摘要，由核心落库事务统一生成 */
    private Map<String, Object> qualitySummary = new LinkedHashMap<>();

    /** 库存预测结果，key=胎侧编码 */
    private Map<String, TcStockForecast> stockForecastMap = new HashMap<>();

    /** 胎侧班初滚动库存状态，key=胎侧编码；初值为14点预计库存，任务完成后回写交接班预计库存 */
    private Map<String, BigDecimal> remainingStockMap = new HashMap<>();

    /** 机台分配前的胎侧期初库存快照，key=胎侧编码 */
    private Map<String, BigDecimal> initialStockMap = new HashMap<>();

    /** 胎侧班次实际短缺台账，key=胎侧编码|班次 */
    private Map<String, BigDecimal> productShiftShortageMap = new LinkedHashMap<>();

    /** 工厂可用机台候选列表，由数据加载层填充，供机台分配步骤过滤评分使用 */
    private List<TcMachineCandidate> machineCandidateList = new ArrayList<>();

    /** 工厂已配置的有效口型板编码集合，用于判断任务口型板是否启用机台白名单 */
    private Set<String> configuredMouthPlateCodeSet = new HashSet<>();

    /** 工厂已配置的启用胶料编码集合，用于判断任务主胶料是否启用机台白名单 */
    private Set<String> configuredGlueCodeSet = new HashSet<>();

    /** 损耗率规则列表，由数据加载层填充，供机台分配步骤解析最终损耗率 */
    private List<TcLossRule> lossRuleList = new ArrayList<>();

    /** 计划量阶段计算出的首个任务工装池初始可用数量 */
    private BigDecimal initialAvailableToolQty;

    /** 机台分配阶段正在滚动使用的全局可用工装数量 */
    private BigDecimal currentAvailableToolQty;

    /** 工装账本快照，key=任务业务键。 */
    private Map<String, ScheduleToolLedgerSnapshot> toolLedgerSnapshotMap = new LinkedHashMap<>();

    /** 本批次全局工装账本稳定序号 */
    private Integer toolLedgerSequence = 0;

    /** 小胶种编码集合，来源于本次参数快照 */
    private Set<String> smallGlueCodeSet = new HashSet<>();

    /** 小胶种本次排程内绑定机台，key=主胶料编码，value=机台编码 */
    private Map<String, String> smallGlueMachineMap = new HashMap<>();

    /** 本次自动排程异常收集器 */
    private TcAutoScheduleIssueCollector issueCollector = new TcAutoScheduleIssueCollector();

    /** 本次自动排程进度监听器 */
    private TcAutoScheduleProgressListener progressListener;
    /** 当前排程日一班开始前的同机台前置任务快照，key=机台编码 */
    private Map<String, TcTaskPredecessor> machinePredecessorMap = new HashMap<>();

    /** 单任务候选机台过滤和评分快照，key=任务业务键 */
    private Map<String, List<TcMachineCandidate>> candidateTraceMap = new HashMap<>();

    /** 待按最终任务链输出的任务关联过程日志。 */
    private List<TcTaskProcessLogEntry> deferredTaskProcessLogList = new ArrayList<>();

    /** 任务关联过程日志的原始发生序号。 */
    private Long deferredTaskProcessLogSequence = 0L;

    /** 班次小时数映射，key=班次顺序(1~6)，来自 T_TC_SHIFT_CONFIG */
    private Map<Integer, BigDecimal> shiftHoursMap = new HashMap<>();

    /** 整日停产后的首个开班班次集合 */
    private Set<Integer> startupShiftOrderSet = new HashSet<>();

    /** 工作日历停产的结果班次集合，任何分配路径均不得写入正计划量。 */
    private Set<Integer> workCalendarStoppedShiftOrderSet = new HashSet<>();

    /** 工作日历停产证据，key=结果班次顺序。 */
    private Map<Integer, Map<String, Object>> workCalendarStoppedShiftEvidenceMap = new LinkedHashMap<>();

    /** 当前日停产需求重分配证据，key=目标班次 */
    private Map<Integer, Map<String, Object>> currentDayShutdownEvidenceMap = new LinkedHashMap<>();

    /** 班次时间窗口映射，key=班次顺序(1~6)，来自 T_TC_SHIFT_CONFIG */
    private Map<Integer, TcShiftTimeWindow> shiftTimeWindowMap = new HashMap<>();

    /** 成型计划已加载但按 TC_FORMING_SHIFT_OFFSET 偏移后无可排程班次时的细化提示，供响应阶段直接使用；为空表示未触发 */
    private String emptyFormingTaskMessage;

    /**
     * 追加一条中文自动排程过程日志。
     *
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args   日志参数
     */
    public void appendProcessLog(String format, Object... args) {
        this.getOrCreateProcessTraceBuffer().appendSummary(format, args);
    }

    /**
     * 追加指定班次的中文自动排程过程日志。
     *
     * @param shiftOrder 班次顺序
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendShiftProcessLog(Integer shiftOrder, String format, Object... args) {
        this.getOrCreateProcessTraceBuffer().appendShiftSummary(shiftOrder, format, args);
    }

    /**
     * 追加指定班次和业务分区的胎侧过程日志。
     *
     * @param shiftOrder 班次顺序
     * @param section    过程日志分区
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendShiftProcessLog(Integer shiftOrder, ScheduleProcessLogSection section,
                                      String format, Object... args) {
        this.getOrCreateProcessTraceBuffer().appendShiftSummary(shiftOrder, section, format, args);
    }

    /**
     * 追加指定班次的延后过程日志，渲染时位于库存、计划量和机台评分日志之后。
     *
     * @param shiftOrder 班次顺序
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendDeferredShiftProcessLog(Integer shiftOrder, String format, Object... args) {
        this.getOrCreateProcessTraceBuffer().appendDeferredShiftSummary(shiftOrder, format, args);
    }

    /**
     * 追加指定班次和业务分区的延后胎侧过程日志。
     *
     * @param shiftOrder 班次顺序
     * @param section    过程日志分区
     * @param format     日志格式，使用 MessageFormat 占位符
     * @param args       日志参数
     */
    public void appendDeferredShiftProcessLog(Integer shiftOrder, ScheduleProcessLogSection section,
                                               String format, Object... args) {
        this.getOrCreateProcessTraceBuffer().appendDeferredShiftSummary(shiftOrder, section, format, args);
    }

    /**
     * 追加一条必须在所有班次日志之后输出的批次尾部日志。
     *
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args   日志参数
     */
    public void appendTailProcessLog(String format, Object... args) {
        this.getOrCreateProcessTraceBuffer().appendTailSummary(format, args);
    }

    /**
     * 追加一条完整中文过程事件。
     *
     * @param event 完整过程事件
     */
    public void appendFullProcessTrace(ScheduleProcessTraceEvent event) {
        this.getOrCreateProcessTraceBuffer().appendFull(event);
    }

    /**
     * 追加指定班次的完整中文过程事件。
     *
     * @param shiftOrder 班次顺序
     * @param event      完整过程事件
     */
    public void appendShiftFullProcessTrace(Integer shiftOrder, ScheduleProcessTraceEvent event) {
        this.getOrCreateProcessTraceBuffer().appendShiftFull(shiftOrder, event);
    }

    /**
     * 追加指定班次和业务分区的胎侧 FULL 过程事件。
     *
     * @param shiftOrder 班次顺序
     * @param section    过程日志分区
     * @param event      完整过程事件
     */
    public void appendShiftFullProcessTrace(Integer shiftOrder, ScheduleProcessLogSection section,
                                            ScheduleProcessTraceEvent event) {
        this.getOrCreateProcessTraceBuffer().appendShiftFull(shiftOrder, section, event);
    }

    /**
     * 追加指定班次的延后完整过程事件，渲染时位于该班次普通事件之后。
     *
     * @param shiftOrder 班次顺序
     * @param event      完整过程事件
     */
    public void appendDeferredShiftFullProcessTrace(Integer shiftOrder, ScheduleProcessTraceEvent event) {
        this.getOrCreateProcessTraceBuffer().appendDeferredShiftFull(shiftOrder, event);
    }

    /**
     * 追加指定班次和业务分区的延后胎侧 FULL 过程事件。
     *
     * @param shiftOrder 班次顺序
     * @param section    过程日志分区
     * @param event      完整过程事件
     */
    public void appendDeferredShiftFullProcessTrace(Integer shiftOrder, ScheduleProcessLogSection section,
                                                    ScheduleProcessTraceEvent event) {
        this.getOrCreateProcessTraceBuffer().appendDeferredShiftFull(shiftOrder, section, event);
    }

    /**
     * 暂存一条任务关联摘要日志，待机台分配完成后按最终任务链输出。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder      班次顺序
     * @param format          日志格式，使用 MessageFormat 占位符
     * @param args            日志参数
     */
    public void appendDeferredTaskProcessLog(String taskBusinessKey, Integer shiftOrder,
                                             String format, Object... args) {
        this.appendDeferredTaskProcessLogByCategory(taskBusinessKey, shiftOrder,
                TcTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION, format, args);
    }

    /**
     * 暂存一条指定类别的任务关联摘要日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder      班次顺序
     * @param logCategory     日志类别
     * @param format           日志格式，使用 MessageFormat 占位符
     * @param args             日志参数
     */
    private void appendDeferredTaskProcessLogByCategory(String taskBusinessKey, Integer shiftOrder,
                                                        String logCategory, String format, Object... args) {
        TcTaskProcessLogEntry entry = new TcTaskProcessLogEntry();
        entry.setTaskBusinessKey(taskBusinessKey);
        entry.setShiftOrder(shiftOrder);
        entry.setLogCategory(logCategory);
        entry.setOccurrenceOrder(this.nextDeferredTaskProcessLogSequence());
        entry.setFormat(format);
        entry.setArgs(args == null ? new Object[0] : args.clone());
        this.getOrCreateDeferredTaskProcessLogList().add(entry);
    }

    /**
     * 暂存一条选机后的工装预校验任务关联摘要日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder      班次顺序
     * @param format           日志格式，使用 MessageFormat 占位符
     * @param args             日志参数
     */
    public void appendDeferredToolPrecheckProcessLog(String taskBusinessKey, Integer shiftOrder,
                                                     String format, Object... args) {
        this.appendDeferredTaskProcessLogByCategory(taskBusinessKey, shiftOrder,
                TcTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK, format, args);
    }

    /**
     * 暂存一条实际任务承接后的工装账本结算任务关联摘要日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder      班次顺序
     * @param format          日志格式，使用 MessageFormat 占位符
     * @param args            日志参数
     */
    public void appendDeferredToolLedgerSettlementProcessLog(String taskBusinessKey, Integer shiftOrder,
                                                             String format, Object... args) {
        this.appendDeferredTaskProcessLogByCategory(taskBusinessKey, shiftOrder,
                TcTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT, format, args);
    }

    /**
     * 兼容历史测试的工装账本日志入口，统一按工装账本结算阶段输出。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder      班次顺序
     * @param format          日志格式，使用 MessageFormat 占位符
     * @param args            日志参数
     */
    public void appendDeferredToolProcessLog(String taskBusinessKey, Integer shiftOrder,
                                             String format, Object... args) {
        this.appendDeferredToolLedgerSettlementProcessLog(taskBusinessKey, shiftOrder, format, args);
    }

    /**
     * 暂存一条任务关联 FULL 事件，待机台分配完成后按最终任务链输出。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder      班次顺序
     * @param event           完整过程事件
     */
    public void appendDeferredTaskFullProcessTrace(String taskBusinessKey, Integer shiftOrder,
                                                   ScheduleProcessTraceEvent event) {
        if (event == null) {
            return;
        }
        TcTaskProcessLogEntry entry = new TcTaskProcessLogEntry();
        entry.setTaskBusinessKey(taskBusinessKey);
        entry.setShiftOrder(shiftOrder);
        entry.setLogCategory(TcTaskProcessLogEntry.CATEGORY_MACHINE_ASSIGN);
        entry.setOccurrenceOrder(this.nextDeferredTaskProcessLogSequence());
        entry.setFullEvent(event);
        this.getOrCreateDeferredTaskProcessLogList().add(entry);
    }

    /**
     * 将指定任务的待输出日志按原始发生顺序写入当前班次日志块。
     *
     * @param task 当前最终任务
     */
    public void flushDeferredTaskProcessLogs(TcTaskDraft task) {
        if (task == null || StrUtil.isBlank(task.getBusinessKey())) {
            return;
        }
        this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .filter(entry -> task.getBusinessKey().equals(entry.getTaskBusinessKey()))
                .sorted(Comparator.comparing(TcTaskProcessLogEntry::getOccurrenceOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(this::renderDeferredTaskProcessLog);
    }

    /**
     * 输出尚未归入任务块的过程日志。
     *
     * <p>正常完成时用于保留未排或无法关联节点的日志；失败保存时按已经形成的最终任务链排序，
     * 无任务链节点的日志按原始发生顺序稳定兜底，确保异常前已发生过程不丢失。</p>
     */
    public void flushRemainingDeferredTaskProcessLogs() {
        this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .sorted(this.buildDeferredTaskProcessLogComparator())
                .forEach(this::renderDeferredTaskProcessLog);
    }

    /**
     * 按参数配置过程日志级别。
     *
     * @param configuredValue 日志级别参数值
     */
    public void configureProcessLogLevel(String configuredValue) {
        this.getOrCreateProcessTraceBuffer().configure(configuredValue);
    }

    /**
     * 获取当前有效过程日志级别。
     *
     * @return 有效日志级别
     */
    public ScheduleProcessLogLevel getProcessLogLevel() {
        return this.getOrCreateProcessTraceBuffer().getLevel();
    }

    /**
     * 获取当前已收集的有效过程事件数量。
     *
     * @return 过程事件数量
     */
    public int getProcessLogEventCount() {
        int bufferedEventCount = this.getOrCreateProcessTraceBuffer().getEventCount();
        if (ScheduleProcessLogLevel.OFF == this.getProcessLogLevel()) {
            return bufferedEventCount;
        }
        long pendingEventCount = this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .filter(entry -> ScheduleProcessLogLevel.FULL == this.getProcessLogLevel()
                        || entry.getFullEvent() == null)
                .count();
        return bufferedEventCount + (int) pendingEventCount;
    }

    /**
     * 获取本次自动排程已收集的中文过程日志。
     *
     * @return 中文过程日志文本
     */
    public String getProcessLogText() {
        this.flushRemainingDeferredTaskProcessLogs();
        return this.getOrCreateProcessTraceBuffer().render();
    }

    /**
     * 获取任务关联过程日志集合，兼容测试注入或反序列化后的空集合。
     *
     * @return 非空任务关联过程日志集合
     */
    private List<TcTaskProcessLogEntry> getOrCreateDeferredTaskProcessLogList() {
        if (this.deferredTaskProcessLogList == null) {
            this.deferredTaskProcessLogList = new ArrayList<>();
        }
        return this.deferredTaskProcessLogList;
    }

    /**
     * 获取下一个任务关联日志发生序号。
     *
     * @return 从 1 开始递增的发生序号
     */
    private long nextDeferredTaskProcessLogSequence() {
        this.deferredTaskProcessLogSequence = Optional.ofNullable(this.deferredTaskProcessLogSequence).orElse(0L) + 1L;
        return this.deferredTaskProcessLogSequence;
    }

    /**
     * 构建未输出任务日志的稳定排序器。
     *
     * @return 按班次、是否已排、机台、任务链序号和发生顺序排序的比较器
     */
    private Comparator<TcTaskProcessLogEntry> buildDeferredTaskProcessLogComparator() {
        Comparator<TcTaskProcessLogEntry> toolLedgerComparator = Comparator
                .comparing((TcTaskProcessLogEntry entry) -> this.resolveTaskToolLedgerOrder(entry),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(this.buildTaskDisplayOrderComparator());
        Comparator<TcTaskProcessLogEntry> taskComparator = this.buildTaskDisplayOrderComparator();
        return Comparator.comparingInt((TcTaskProcessLogEntry entry) -> this.resolveTaskProcessLogSection(entry).ordinal())
                .thenComparing((left, right) -> {
                    boolean leftIsTool = this.isToolLedgerStage(left);
                    boolean rightIsTool = this.isToolLedgerStage(right);
                    if (leftIsTool && rightIsTool) {
                        return toolLedgerComparator.compare(left, right);
                    }
                    if (!leftIsTool && !rightIsTool) {
                        return taskComparator.compare(left, right);
                    }
                    return 0;
                });
    }

    /**
     * 判断日志是否属于按全局工装账本序号展示的阶段。
     *
     * @param entry 任务关联过程日志
     * @return true 表示工装预校验或工装账本结算阶段
     */
    private boolean isToolLedgerStage(TcTaskProcessLogEntry entry) {
        if (entry == null) {
            return false;
        }
        return TcTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK.equals(entry.getLogCategory())
                || TcTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT.equals(entry.getLogCategory());
    }

    /**
     * 构建按最终任务链稳定展示的比较器。
     *
     * @return 任务链稳定排序器
     */
    private Comparator<TcTaskProcessLogEntry> buildTaskDisplayOrderComparator() {
        return Comparator.comparing(TcTaskProcessLogEntry::getShiftOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(entry -> this.resolveTaskNode(entry) == null ? 1 : 0)
                .thenComparing(entry -> this.resolveTaskNodeMachineCode(entry),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(entry -> this.resolveTaskNodeSequence(entry),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TcTaskProcessLogEntry::getOccurrenceOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * 获取日志关联的最终任务链节点。
     *
     * @param entry 任务关联日志
     * @return 最终任务链节点；不存在时返回 null
     */
    private ScheduleTaskNode<TcTaskDraft> resolveTaskNode(TcTaskProcessLogEntry entry) {
        return entry == null ? null : this.getTaskNode(entry.getTaskBusinessKey());
    }

    /**
     * 获取日志关联任务的最终机台。
     *
     * @param entry 任务关联日志
     * @return 最终机台编码
     */
    private String resolveTaskNodeMachineCode(TcTaskProcessLogEntry entry) {
        ScheduleTaskNode<TcTaskDraft> node = this.resolveTaskNode(entry);
        return node == null ? null : node.getMachineCode();
    }

    /**
     * 获取日志关联任务的最终班内序号。
     *
     * @param entry 任务关联日志
     * @return 最终班内序号
     */
    private Integer resolveTaskNodeSequence(TcTaskProcessLogEntry entry) {
        ScheduleTaskNode<TcTaskDraft> node = this.resolveTaskNode(entry);
        return node == null ? null : node.getSequence();
    }

    /**
     * 将一条任务关联日志写入通用过程日志缓冲。
     *
     * @param entry 待输出日志
     */
    private void renderDeferredTaskProcessLog(TcTaskProcessLogEntry entry) {
        if (entry == null || entry.isRendered()) {
            return;
        }
        if (entry.getFullEvent() != null) {
            this.getOrCreateProcessTraceBuffer().appendShiftFull(entry.getShiftOrder(),
                    this.resolveTaskProcessLogSection(entry), entry.getFullEvent());
        } else {
            this.getOrCreateProcessTraceBuffer().appendShiftSummary(entry.getShiftOrder(),
                    this.resolveTaskProcessLogSection(entry), entry.getFormat(), entry.getArgs());
        }
        entry.setRendered(true);
    }

    /**
     * 根据任务关联日志类别解析业务分区。
     *
     * @param entry 任务关联日志
     * @return 过程日志分区
     */
    private ScheduleProcessLogSection resolveTaskProcessLogSection(TcTaskProcessLogEntry entry) {
        if (entry == null) {
            return ScheduleProcessLogSection.MACHINE_SELECTION;
        }
        if (TcTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK.equals(entry.getLogCategory())) {
            return ScheduleProcessLogSection.TOOL_LIMIT;
        }
        if (TcTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION.equals(entry.getLogCategory())) {
            return ScheduleProcessLogSection.CAPACITY_DEDUCTION;
        }
        if (TcTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT.equals(entry.getLogCategory())) {
            return ScheduleProcessLogSection.TOOL_LEDGER_SETTLEMENT;
        }
        return ScheduleProcessLogSection.MACHINE_SELECTION;
    }

    /**
     * 获取任务关联日志的全局工装账本序号。
     *
     * @param entry 任务关联过程日志
     * @return 工装账本序号；未配置时返回 null
     */
    private Integer resolveTaskToolLedgerOrder(TcTaskProcessLogEntry entry) {
        if (entry == null || StrUtil.isBlank(entry.getTaskBusinessKey())) {
            return null;
        }
        return this.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .filter(task -> entry.getTaskBusinessKey().equals(task.getBusinessKey()))
                .map(TcTaskDraft::getToolLedgerOrder)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取过程日志缓冲器，避免反序列化或测试注入后出现空引用。
     *
     * @return 过程日志缓冲器
     */
    private ScheduleProcessTraceBuffer getOrCreateProcessTraceBuffer() {
        if (this.processTraceBuffer == null) {
            this.processTraceBuffer = new ScheduleProcessTraceBuffer();
        }
        return this.processTraceBuffer;
    }

    /**
     * 获取下一个全局工装账本序号。
     *
     * @return 从 1 开始递增的账本序号
     */
    public int nextToolLedgerOrder() {
        this.toolLedgerSequence = Optional.ofNullable(this.toolLedgerSequence).orElse(0) + 1;
        return this.toolLedgerSequence;
    }

    /**
     * 按参数编码读取本次排程参数快照。
     *
     * @param paramCode 参数编码
     * @return 参数快照值
     * @throws ServiceException 参数不存在或没有有效值时抛出
     */
    public TcParamValue getParam(String paramCode) {
        TcParamValue paramValue = paramMap.get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_PARAM_EMPTY.getDefaultMessage() + ":" + paramCode);
        }
        return paramValue;
    }

    /**
     * 获取指定机台班次任务链。
     *
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     * @return 已存在任务链；不存在时返回空
     */
    public ScheduleTaskLinkedList<TcTaskDraft> getTaskChain(String machineCode, Integer shiftOrder) {
        if (scheduleDate == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }
        return taskChainGroup.get(machineCode, DateUtil.toLocalDateTime(scheduleDate).toLocalDate(), shiftOrder);
    }

    public void setParamMap(Map<String, TcParamValue> paramMap) {
        this.paramMap = paramMap == null ? new HashMap<>() : paramMap;
    }

    public void setTaskDraftList(List<TcTaskDraft> taskDraftList) {
        this.taskDraftList = taskDraftList == null ? new ArrayList<>() : taskDraftList;
    }

    public void setSourceTaskDraftList(List<TcTaskDraft> sourceTaskDraftList) {
        this.sourceTaskDraftList = sourceTaskDraftList == null ? new ArrayList<>() : sourceTaskDraftList;
    }

    public void setPlanTaskGroupMap(Map<String, TcPlanTaskGroup> planTaskGroupMap) {
        this.planTaskGroupMap = planTaskGroupMap == null ? new LinkedHashMap<>() : planTaskGroupMap;
    }

    public void setTaskChainGroup(MachineShiftTaskChain<TcTaskDraft> taskChainGroup) {
        this.taskChainGroup = taskChainGroup == null ? new MachineShiftTaskChain<>() : taskChainGroup;
    }

    public void setTaskNodeIndex(Map<String, ScheduleTaskNode<TcTaskDraft>> taskNodeIndex) {
        this.taskNodeIndex = taskNodeIndex == null ? new HashMap<>() : taskNodeIndex;
    }

    /**
     * 注册任务链节点索引。
     *
     * @param taskId 任务标识
     * @param node   任务链节点
     */
    public void registerTaskNode(String taskId, ScheduleTaskNode<TcTaskDraft> node) {
        if (StrUtil.isBlank(taskId) || node == null) {
            return;
        }
        taskNodeIndex.put(taskId, node);
    }

    /**
     * 根据任务标识获取任务链节点。
     *
     * @param taskId 任务标识
     * @return 任务链节点，不存在时返回 null
     */
    public ScheduleTaskNode<TcTaskDraft> getTaskNode(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return taskNodeIndex.get(taskId);
    }

    /**
     * 移除任务链节点索引。
     *
     * @param taskId 任务标识
     */
    public void removeTaskNode(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return;
        }
        taskNodeIndex.remove(taskId);
    }

    public void setSnapshotMap(Map<String, TcSnapshotBuildResult> snapshotMap) {
        this.snapshotMap = snapshotMap == null ? new HashMap<>() : snapshotMap;
    }

    public void setRuleTraceMap(Map<String, TcRuleTrace> ruleTraceMap) {
        this.ruleTraceMap = ruleTraceMap == null ? new HashMap<>() : ruleTraceMap;
    }

    public void setQualitySummary(Map<String, Object> qualitySummary) {
        this.qualitySummary = qualitySummary == null ? new LinkedHashMap<>() : qualitySummary;
    }

    public void setStockForecastMap(Map<String, TcStockForecast> stockForecastMap) {
        this.stockForecastMap = stockForecastMap == null ? new HashMap<>() : stockForecastMap;
    }

    public void setRemainingStockMap(Map<String, BigDecimal> remainingStockMap) {
        this.remainingStockMap = remainingStockMap == null ? new HashMap<>() : remainingStockMap;
    }

    public void setSmallGlueCodeSet(Set<String> smallGlueCodeSet) {
        this.smallGlueCodeSet = smallGlueCodeSet == null ? new HashSet<>() : smallGlueCodeSet;
    }

    public void setSmallGlueMachineMap(Map<String, String> smallGlueMachineMap) {
        this.smallGlueMachineMap = smallGlueMachineMap == null ? new HashMap<>() : smallGlueMachineMap;
    }

    public void setIssueCollector(TcAutoScheduleIssueCollector issueCollector) {
        this.issueCollector = issueCollector == null ? new TcAutoScheduleIssueCollector() : issueCollector;
    }
    public void setMachinePredecessorMap(Map<String, TcTaskPredecessor> machinePredecessorMap) {
        this.machinePredecessorMap = machinePredecessorMap == null ? new HashMap<>() : machinePredecessorMap;
    }

    public void setCandidateTraceMap(Map<String, List<TcMachineCandidate>> candidateTraceMap) {
        this.candidateTraceMap = candidateTraceMap == null ? new HashMap<>() : candidateTraceMap;
    }

    public void setShiftHoursMap(Map<Integer, BigDecimal> shiftHoursMap) {
        this.shiftHoursMap = shiftHoursMap == null ? new HashMap<>() : shiftHoursMap;
    }

    public void setStartupShiftOrderSet(Set<Integer> startupShiftOrderSet) {
        this.startupShiftOrderSet = startupShiftOrderSet == null ? new HashSet<>() : startupShiftOrderSet;
    }

    public void setWorkCalendarStoppedShiftOrderSet(Set<Integer> workCalendarStoppedShiftOrderSet) {
        this.workCalendarStoppedShiftOrderSet = workCalendarStoppedShiftOrderSet == null
                ? new HashSet<>() : workCalendarStoppedShiftOrderSet;
    }

    public void setWorkCalendarStoppedShiftEvidenceMap(
            Map<Integer, Map<String, Object>> workCalendarStoppedShiftEvidenceMap) {
        this.workCalendarStoppedShiftEvidenceMap = workCalendarStoppedShiftEvidenceMap == null
                ? new LinkedHashMap<>() : workCalendarStoppedShiftEvidenceMap;
    }

    /**
     * 将工作日历停机证据追加到批次级过程日志。
     *
     * @throws NullPointerException 不抛出；停机证据为空时直接跳过
     */
    public void appendWorkCalendarStoppedShiftProcessLogs() {
        if (this.workCalendarStoppedShiftEvidenceMap == null
                || this.workCalendarStoppedShiftEvidenceMap.isEmpty()) {
            return;
        }
        this.workCalendarStoppedShiftEvidenceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> {
                    Map<String, Object> evidence = entry.getValue();
                    String calendarField = String.valueOf(evidence.get("calendarField"));
                    String stopReason = "DAY_FLAG".equals(calendarField)
                            ? "工作日历整日停产（DAY_FLAG=0）"
                            : "工作日历指定班次停产（" + calendarField + "=0）";
                    this.appendProcessLog("工作日历停机：工序={0}，生产日期={1}，排程班次={2}，日历字段={3}，字段值={4}，停机原因={5}",
                            evidence.get("procCode"), evidence.get("productionDate"),
                            evidence.get("shiftOrder"), calendarField,
                            evidence.get("calendarFieldValue"), stopReason);
                });
    }

    public void setShiftTimeWindowMap(Map<Integer, TcShiftTimeWindow> shiftTimeWindowMap) {
        this.shiftTimeWindowMap = shiftTimeWindowMap == null ? new HashMap<>() : shiftTimeWindowMap;
    }

    public void setLossRuleList(List<TcLossRule> lossRuleList) {
        this.lossRuleList = lossRuleList == null ? new ArrayList<>() : lossRuleList;
    }
}
