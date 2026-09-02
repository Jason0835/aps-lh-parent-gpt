package com.zlt.aps.common.engine.schedule.engine;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.common.engine.schedule.*;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerSnapshot;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * TM/TC 自动排程公共运行态上下文模型。
 *
 * <p>该模型不落库，只统一两侧步骤服务共享的数据总线；产品差异通过领域上下文和边界模型承载。</p>
 */
@Data
public class ScheduleContextModel<T extends ScheduleTaskDraftModel, S, R extends ScheduleRuleTrace, PR,
        F extends ScheduleInventoryForecast, M extends ScheduleMachineCandidateModel,
        L, I, Pred> {

    protected String factoryCode;
    protected String batchNo;
    protected String traceId;
    protected ScheduleProcessTraceBuffer processTraceBuffer = new ScheduleProcessTraceBuffer();
    protected Date scheduleDate;
    protected String operator;
    protected Map<String, ScheduleParamValueModel> paramMap = new HashMap<>();
    protected List<T> taskDraftList = new ArrayList<>();
    protected List<T> sourceTaskDraftList = new ArrayList<>();
    protected Map<String, SchedulePlanTaskGroup<T>> planTaskGroupMap = new LinkedHashMap<>();
    protected MachineShiftTaskChain<T> taskChainGroup = new MachineShiftTaskChain<>();
    protected Map<String, ScheduleTaskNode<T>> taskNodeIndex = new HashMap<>();
    protected Map<String, S> snapshotMap = new HashMap<>();
    protected Map<String, R> ruleTraceMap = new HashMap<>();
    protected Map<String, Integer> processRuleTraceCursorMap = new HashMap<>();
    protected String currentProcessStep;
    protected List<String> completedProcessSteps = new ArrayList<>();
    protected PR persistResult;
    protected Map<String, Object> qualitySummary = new LinkedHashMap<>();
    protected Map<String, F> stockForecastMap = new HashMap<>();
    /** 用于库存预测和工装占用的产品集合，与可排任务集合严格分离。 */
    protected Set<String> inventoryProductCodeSet = new LinkedHashSet<>();
    protected Map<String, BigDecimal> remainingStockMap = new HashMap<>();
    protected Map<String, BigDecimal> initialStockMap = new HashMap<>();
    protected Map<String, BigDecimal> productShiftShortageMap = new LinkedHashMap<>();
    protected List<M> machineCandidateList = new ArrayList<>();
    protected Set<String> configuredMouthPlateCodeSet = new HashSet<>();
    protected Set<String> configuredGlueCodeSet = new HashSet<>();
    protected List<L> lossRuleList = new ArrayList<>();
    protected BigDecimal initialAvailableToolQty;
    protected BigDecimal currentAvailableToolQty;
    protected Map<String, ScheduleToolLedgerSnapshot> toolLedgerSnapshotMap = new LinkedHashMap<>();
    /** 已完成当班成型释放的班次+产品键，避免同一产品重复释放。 */
    protected Set<String> settledToolDemandKeySet = new HashSet<>();
    protected Integer toolLedgerSequence = 0;
    protected Set<String> smallGlueCodeSet = new HashSet<>();
    protected Map<String, String> smallGlueMachineMap = new HashMap<>();
    protected I issueCollector;
    protected ScheduleProgressListener progressListener;
    protected Map<String, Pred> machinePredecessorMap = new HashMap<>();
    protected Map<String, List<M>> candidateTraceMap = new HashMap<>();
    protected Map<Integer, BigDecimal> shiftHoursMap = new HashMap<>();
    protected Set<Integer> startupShiftOrderSet = new HashSet<>();
    protected Set<Integer> workCalendarStoppedShiftOrderSet = new HashSet<>();
    protected Map<Integer, Map<String, Object>> workCalendarStoppedShiftEvidenceMap = new LinkedHashMap<>();
    protected Map<Integer, Map<String, Object>> currentDayShutdownEvidenceMap = new LinkedHashMap<>();
    protected Map<Integer, ScheduleShiftTimeWindowModel> shiftTimeWindowMap = new HashMap<>();
    protected String emptyFormingTaskMessage;
    protected List<ScheduleTaskProcessLogEntry> deferredTaskProcessLogList = new ArrayList<>();
    protected Long deferredTaskProcessLogSequence = 0L;

    /** 创建公共排程上下文。 */
    protected ScheduleContextModel() {
    }

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
     * 追加指定班次和业务分区的胎面过程日志。
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
     * 追加指定班次和业务分区的延后胎面过程日志。
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
     * 追加指定班次和业务分区的胎面 FULL 过程事件。
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
     * 追加指定班次和业务分区的延后胎面 FULL 过程事件。
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
     * 获取本次自动排程已收集的中文过程日志。
     *
     * @return 中文过程日志文本
     */
    public String getProcessLogText() {
        this.flushRemainingDeferredTaskProcessLogs();
        return this.getOrCreateProcessTraceBuffer().render();
    }

    /**
     * 暂存一条任务关联摘要日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args 日志参数
     */
    public void appendDeferredTaskProcessLog(String taskBusinessKey, Integer shiftOrder,
                                             String format, Object... args) {
        this.appendDeferredTaskProcessLogByCategory(taskBusinessKey, shiftOrder,
                ScheduleTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION, format, args);
    }

    /**
     * 暂存一条选机后的工装预校验日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args 日志参数
     */
    public void appendDeferredToolPrecheckProcessLog(String taskBusinessKey, Integer shiftOrder,
                                                     String format, Object... args) {
        this.appendDeferredTaskProcessLogByCategory(taskBusinessKey, shiftOrder,
                ScheduleTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK, format, args);
    }

    /**
     * 暂存一条工装账本结算日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args 日志参数
     */
    public void appendDeferredToolLedgerSettlementProcessLog(String taskBusinessKey, Integer shiftOrder,
                                                              String format, Object... args) {
        this.appendDeferredTaskProcessLogByCategory(taskBusinessKey, shiftOrder,
                ScheduleTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT, format, args);
    }

    /**
     * 兼容历史工装日志入口。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args 日志参数
     */
    public void appendDeferredToolProcessLog(String taskBusinessKey, Integer shiftOrder,
                                              String format, Object... args) {
        this.appendDeferredToolLedgerSettlementProcessLog(taskBusinessKey, shiftOrder, format, args);
    }

    /**
     * 暂存一条任务关联 FULL 事件。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param event 完整过程事件
     */
    public void appendDeferredTaskFullProcessTrace(String taskBusinessKey, Integer shiftOrder,
                                                    ScheduleProcessTraceEvent event) {
        if (event == null) {
            return;
        }
        ScheduleTaskProcessLogEntry entry = this.newDeferredTaskProcessLogEntry(taskBusinessKey, shiftOrder,
                ScheduleTaskProcessLogEntry.CATEGORY_MACHINE_ASSIGN);
        entry.setFullEvent(event);
        this.getOrCreateDeferredTaskProcessLogList().add(entry);
    }

    /**
     * 将指定任务的产能扣减日志写入当前班次日志块。
     *
     * @param task 当前最终任务
     */
    public void flushDeferredTaskProcessLogs(T task) {
        this.flushDeferredTaskProcessLogs(task, ScheduleTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION);
    }

    /**
     * 将指定任务的工装预校验日志写入当前班次日志块。
     *
     * @param task 当前最终任务
     */
    public void flushDeferredToolPrecheckProcessLogs(T task) {
        this.flushDeferredTaskProcessLogs(task, ScheduleTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK);
    }

    /**
     * 将指定任务的工装账本结算日志写入当前班次日志块。
     *
     * @param task 当前最终任务
     */
    public void flushDeferredToolProcessLogs(T task) {
        this.flushDeferredTaskProcessLogs(task, ScheduleTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT);
    }

    /**
     * 按日志类别刷新指定任务的延后日志。
     *
     * @param task 当前最终任务
     * @param logCategory 日志类别
     */
    protected void flushDeferredTaskProcessLogs(T task, String logCategory) {
        if (task == null || StrUtil.isBlank(task.getBusinessKey())) {
            return;
        }
        this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .filter(entry -> task.getBusinessKey().equals(entry.getTaskBusinessKey()))
                .filter(entry -> this.isLogCategory(entry, logCategory))
                .sorted(Comparator.comparing(ScheduleTaskProcessLogEntry::getOccurrenceOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(this::renderDeferredTaskProcessLog);
    }

    /** 输出尚未归入任务块的过程日志。 */
    public void flushRemainingDeferredTaskProcessLogs() {
        this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .sorted(this.buildDeferredTaskProcessLogComparator())
                .forEach(this::renderDeferredTaskProcessLog);
    }

    /** 输出尚未关联到任务列表的工装账本日志。 */
    public void flushRemainingDeferredToolProcessLogs() {
        this.flushRemainingDeferredToolProcessLogs(null);
    }

    /**
     * 输出指定班次尚未关联到任务列表的工装账本日志。
     *
     * @param shiftOrder 班次顺序；为空时输出全部班次
     */
    public void flushRemainingDeferredToolProcessLogs(Integer shiftOrder) {
        this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .filter(entry -> ScheduleTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT
                        .equals(entry.getLogCategory()))
                .filter(entry -> shiftOrder == null || Objects.equals(shiftOrder, entry.getShiftOrder()))
                .sorted(Comparator
                        .comparing((ScheduleTaskProcessLogEntry entry) -> this.resolveTaskToolLedgerOrder(entry),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(this.buildTaskDisplayOrderComparator()))
                .forEach(this::renderDeferredTaskProcessLog);
    }

    /** 输出尚未关联到任务列表的机台分配日志。 */
    public void flushRemainingDeferredMachineProcessLogs() {
        this.flushRemainingDeferredMachineProcessLogs(null);
    }

    /**
     * 输出指定班次尚未关联到任务列表的机台分配日志。
     *
     * @param shiftOrder 班次顺序；为空时输出全部班次
     */
    public void flushRemainingDeferredMachineProcessLogs(Integer shiftOrder) {
        this.getOrCreateDeferredTaskProcessLogList().stream()
                .filter(entry -> !entry.isRendered())
                .filter(entry -> this.isLogCategory(entry, ScheduleTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION))
                .filter(entry -> shiftOrder == null || Objects.equals(shiftOrder, entry.getShiftOrder()))
                .sorted(this.buildTaskDisplayOrderComparator())
                .forEach(this::renderDeferredTaskProcessLog);
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

    /** 创建一条带发生序号的公共任务日志条目。 */
    protected ScheduleTaskProcessLogEntry newDeferredTaskProcessLogEntry(String taskBusinessKey,
                                                                          Integer shiftOrder,
                                                                          String logCategory) {
        ScheduleTaskProcessLogEntry entry = new ScheduleTaskProcessLogEntry();
        entry.setTaskBusinessKey(taskBusinessKey);
        entry.setShiftOrder(shiftOrder);
        entry.setLogCategory(logCategory);
        entry.setOccurrenceOrder(this.nextDeferredTaskProcessLogSequence());
        return entry;
    }

    /**
     * 按类别创建并追加任务日志。
     *
     * @param taskBusinessKey 任务业务键
     * @param shiftOrder 班次顺序
     * @param logCategory 日志类别
     * @param format 日志格式
     * @param args 日志参数
     */
    protected void appendDeferredTaskProcessLogByCategory(String taskBusinessKey, Integer shiftOrder,
                                                          String logCategory, String format, Object... args) {
        ScheduleTaskProcessLogEntry entry = this.newDeferredTaskProcessLogEntry(taskBusinessKey, shiftOrder,
                logCategory);
        entry.setFormat(format);
        entry.setArgs(args == null ? new Object[0] : args.clone());
        this.getOrCreateDeferredTaskProcessLogList().add(entry);
    }

    /** 获取非空的任务关联延后日志集合。 */
    protected List<ScheduleTaskProcessLogEntry> getOrCreateDeferredTaskProcessLogList() {
        if (this.deferredTaskProcessLogList == null) {
            this.deferredTaskProcessLogList = new ArrayList<>();
        }
        return this.deferredTaskProcessLogList;
    }

    /** 获取下一个从 1 开始递增的任务日志发生序号。 */
    protected long nextDeferredTaskProcessLogSequence() {
        this.deferredTaskProcessLogSequence = Optional.ofNullable(this.deferredTaskProcessLogSequence).orElse(0L) + 1L;
        return this.deferredTaskProcessLogSequence;
    }

    /** 构建未输出任务日志的稳定排序器。 */
    private Comparator<ScheduleTaskProcessLogEntry> buildDeferredTaskProcessLogComparator() {
        Comparator<ScheduleTaskProcessLogEntry> toolLedgerComparator = Comparator
                .comparing((ScheduleTaskProcessLogEntry entry) -> this.resolveTaskToolLedgerOrder(entry),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(this.buildTaskDisplayOrderComparator());
        Comparator<ScheduleTaskProcessLogEntry> taskComparator = this.buildTaskDisplayOrderComparator();
        return Comparator.comparingInt((ScheduleTaskProcessLogEntry entry) ->
                        this.resolveTaskProcessLogSection(entry).ordinal())
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

    /** 判断日志是否属于工装阶段。 */
    private boolean isToolLedgerStage(ScheduleTaskProcessLogEntry entry) {
        return entry != null && (ScheduleTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK.equals(entry.getLogCategory())
                || ScheduleTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT.equals(entry.getLogCategory()));
    }

    /** 构建按最终任务链稳定展示的比较器。 */
    private Comparator<ScheduleTaskProcessLogEntry> buildTaskDisplayOrderComparator() {
        return Comparator.comparing(ScheduleTaskProcessLogEntry::getShiftOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(entry -> this.resolveTaskNode(entry) == null ? 1 : 0)
                .thenComparing(entry -> this.resolveTaskNodeMachineCode(entry),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(entry -> this.resolveTaskNodeSequence(entry),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ScheduleTaskProcessLogEntry::getOccurrenceOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /** 获取日志关联的最终任务链节点。 */
    private ScheduleTaskNode<T> resolveTaskNode(ScheduleTaskProcessLogEntry entry) {
        return entry == null ? null : this.getTaskNode(entry.getTaskBusinessKey());
    }

    /** 获取日志关联任务的最终机台编码。 */
    private String resolveTaskNodeMachineCode(ScheduleTaskProcessLogEntry entry) {
        ScheduleTaskNode<T> node = this.resolveTaskNode(entry);
        return node == null ? null : node.getMachineCode();
    }

    /** 获取日志关联任务的最终班内序号。 */
    private Integer resolveTaskNodeSequence(ScheduleTaskProcessLogEntry entry) {
        ScheduleTaskNode<T> node = this.resolveTaskNode(entry);
        return node == null ? null : node.getSequence();
    }

    /** 将公共任务日志条目写入过程日志缓冲。 */
    private void renderDeferredTaskProcessLog(ScheduleTaskProcessLogEntry entry) {
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

    /** 根据日志类别解析过程日志分区。 */
    private ScheduleProcessLogSection resolveTaskProcessLogSection(ScheduleTaskProcessLogEntry entry) {
        if (entry == null) {
            return ScheduleProcessLogSection.MACHINE_SELECTION;
        }
        if (ScheduleTaskProcessLogEntry.CATEGORY_TOOL_PRECHECK.equals(entry.getLogCategory())) {
            return ScheduleProcessLogSection.TOOL_LIMIT;
        }
        if (ScheduleTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION.equals(entry.getLogCategory())) {
            return ScheduleProcessLogSection.CAPACITY_DEDUCTION;
        }
        if (ScheduleTaskProcessLogEntry.CATEGORY_TOOL_LEDGER_SETTLEMENT.equals(entry.getLogCategory())) {
            return ScheduleProcessLogSection.TOOL_LEDGER_SETTLEMENT;
        }
        return ScheduleProcessLogSection.MACHINE_SELECTION;
    }

    /** 获取日志关联任务的全局工装账本序号。 */
    private Integer resolveTaskToolLedgerOrder(ScheduleTaskProcessLogEntry entry) {
        if (entry == null || StrUtil.isBlank(entry.getTaskBusinessKey())) {
            return null;
        }
        return this.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .filter(task -> entry.getTaskBusinessKey().equals(task.getBusinessKey()))
                .map(ScheduleTaskDraftModel::getToolLedgerOrder)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** 判断日志是否属于指定类别，兼容未标记类别的历史机台日志。 */
    private boolean isLogCategory(ScheduleTaskProcessLogEntry entry, String logCategory) {
        if (entry == null) {
            return false;
        }
        if (logCategory.equals(entry.getLogCategory())) {
            return true;
        }
        return ScheduleTaskProcessLogEntry.CATEGORY_CAPACITY_DEDUCTION.equals(logCategory)
                && StrUtil.isBlank(entry.getLogCategory());
    }

/**
     * 获取过程日志缓冲器，避免反序列化或测试注入后出现空引用。
     *
     * @return 过程日志缓冲器
     */
    protected ScheduleProcessTraceBuffer getOrCreateProcessTraceBuffer() {
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
     * 标记指定班次和产品的成型需求是否已经完成工装释放。
     *
     * @param shiftOrder 班次顺序
     * @param productCode 产品编码
     * @return 本次首次标记返回true，已标记或参数无效返回false
     */
    public boolean markToolDemandSettled(Integer shiftOrder, String productCode) {
        if (shiftOrder == null || StrUtil.isBlank(productCode)) {
            return false;
        }
        return this.settledToolDemandKeySet.add(shiftOrder + "#" + StrUtil.trim(productCode));
    }


    public void setParamMap(Map<String, ScheduleParamValueModel> paramMap) {
        this.paramMap = paramMap == null ? new HashMap<>() : paramMap;
    }

    public void setTaskDraftList(List<T> taskDraftList) {
        this.taskDraftList = taskDraftList == null ? new ArrayList<>() : taskDraftList;
    }

    public void setSourceTaskDraftList(List<T> sourceTaskDraftList) {
        this.sourceTaskDraftList = sourceTaskDraftList == null ? new ArrayList<>() : sourceTaskDraftList;
    }

    public void setPlanTaskGroupMap(Map<String, SchedulePlanTaskGroup<T>> planTaskGroupMap) {
        this.planTaskGroupMap = planTaskGroupMap == null ? new LinkedHashMap<>() : planTaskGroupMap;
    }

    public void setTaskChainGroup(MachineShiftTaskChain<T> taskChainGroup) {
        this.taskChainGroup = taskChainGroup == null ? new MachineShiftTaskChain<>() : taskChainGroup;
    }

    public void setTaskNodeIndex(Map<String, ScheduleTaskNode<T>> taskNodeIndex) {
        this.taskNodeIndex = taskNodeIndex == null ? new HashMap<>() : taskNodeIndex;
    }

/**
     * 注册任务链节点索引。
     *
     * @param taskId 任务标识
     * @param node   任务链节点
     */
    public void registerTaskNode(String taskId, ScheduleTaskNode<T> node) {
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
    public ScheduleTaskNode<T> getTaskNode(String taskId) {
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

    public void setSnapshotMap(Map<String, S> snapshotMap) {
        this.snapshotMap = snapshotMap == null ? new HashMap<>() : snapshotMap;
    }

    public void setRuleTraceMap(Map<String, R> ruleTraceMap) {
        this.ruleTraceMap = ruleTraceMap == null ? new HashMap<>() : ruleTraceMap;
    }

    public void setStockForecastMap(Map<String, F> stockForecastMap) {
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

    public void setIssueCollector(I issueCollector) {
        this.issueCollector = issueCollector;
    }

    public void setMachinePredecessorMap(Map<String, Pred> machinePredecessorMap) {
        this.machinePredecessorMap = machinePredecessorMap == null ? new HashMap<>() : machinePredecessorMap;
    }

    public void setCandidateTraceMap(Map<String, List<M>> candidateTraceMap) {
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

    public void setShiftTimeWindowMap(Map<Integer, ScheduleShiftTimeWindowModel> shiftTimeWindowMap) {
        this.shiftTimeWindowMap = shiftTimeWindowMap == null ? new HashMap<>() : shiftTimeWindowMap;
    }

    public void setLossRuleList(List<L> lossRuleList) {
        this.lossRuleList = lossRuleList == null ? new ArrayList<>() : lossRuleList;
    }
}
