package com.zlt.aps.tm.engine.domain;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.*;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerSnapshot;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.engine.service.TmAutoScheduleProgressListener;
import com.zlt.aps.tm.engine.service.collector.TmAutoScheduleIssueCollector;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * 胎面排程上下文。
 *
 * <p>贯穿一次胎面自动排程的运行态数据总线，承载批次、追踪号、排程日期、操作人、
 * 参数快照、待排任务和机台班次任务链。该对象会被步骤服务按流程原地补充数据。</p>
 */
@Data
public class TmScheduleContext {

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
    private Map<String, TmParamValue> paramMap = new HashMap<>();

    /** 待排任务草稿列表 */
    private List<TmTaskDraft> taskDraftList = new ArrayList<>();

    /** 计划量汇总前的原始成型来源任务快照列表 */
    private List<TmTaskDraft> sourceTaskDraftList = new ArrayList<>();

    /** 同胎面同班次计划量汇总组，key=计划量汇总组业务键 */
    private Map<String, TmPlanTaskGroup> planTaskGroupMap = new LinkedHashMap<>();

    /** 机台班次任务链集合 */
    private MachineShiftTaskChain<TmTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();

    /** 任务链节点索引，key=任务ID或业务键 */
    private Map<String, ScheduleTaskNode<TmTaskDraft>> taskNodeIndex = new HashMap<>();

    /** 解释快照，key=任务业务键 */
    private Map<String, TmSnapshotBuildResult> snapshotMap = new HashMap<>();

    /** 规则证据，key=任务业务键 */
    private Map<String, TmRuleTrace> ruleTraceMap = new HashMap<>();

    /** FULL 日志已消费的规则证据游标，key=任务业务键，value=已写入条数。 */
    private Map<String, Integer> processRuleTraceCursorMap = new HashMap<>();

    /** 当前正在执行的中文步骤名，失败日志用于指出中断位置。 */
    private String currentProcessStep;

    /** 已成功完成的中文步骤名。 */
    private List<String> completedProcessSteps = new ArrayList<>();

    /** 本次落库转换汇总 */
    private TmPersistResult persistResult;

    /** 本次自动排程内部质量快照，用于回归和算法对比 */
    private Map<String, Object> qualitySummary = new LinkedHashMap<>();

    /** 库存预测结果，key=胎面编码 */
    private Map<String, TmStockForecast> stockForecastMap = new HashMap<>();

    /** 胎面班初滚动库存状态，key=胎面编码；初值为14点预计库存，任务完成后回写交接班预计库存 */
    private Map<String, BigDecimal> remainingStockMap = new HashMap<>();

    /** 机台分配前的胎面期初库存快照，key=胎面编码 */
    private Map<String, BigDecimal> initialStockMap = new HashMap<>();

    /** 胎面班次实际短缺台账，key=胎面编码|班次 */
    private Map<String, BigDecimal> productShiftShortageMap = new LinkedHashMap<>();

    /** 工厂可用机台候选列表，由数据加载层填充，供机台分配步骤过滤评分使用 */
    private List<TmMachineCandidate> machineCandidateList = new ArrayList<>();

    /** 工厂已配置的有效口型板编码集合，用于判断任务口型板是否启用机台白名单 */
    private Set<String> configuredMouthPlateCodeSet = new HashSet<>();

    /** 工厂已配置的启用胶料编码集合，用于判断任务主胶料是否启用机台白名单 */
    private Set<String> configuredGlueCodeSet = new HashSet<>();

    /** 损耗率规则列表，由数据加载层填充，供机台分配步骤解析最终损耗率 */
    private List<TmLossRule> lossRuleList = new ArrayList<>();

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
    private TmAutoScheduleIssueCollector issueCollector = new TmAutoScheduleIssueCollector();

    /** 本次自动排程进度监听器 */
    private TmAutoScheduleProgressListener progressListener;
    /** 当前排程日一班开始前的同机台前置任务快照，key=机台编码 */
    private Map<String, TmTaskPredecessor> machinePredecessorMap = new HashMap<>();

    /** 单任务候选机台过滤和评分快照，key=任务业务键 */
    private Map<String, List<TmMachineCandidate>> candidateTraceMap = new HashMap<>();

    /** 班次小时数映射，key=班次顺序(1~6)，来自 T_TM_SHIFT_CONFIG */
    private Map<Integer, BigDecimal> shiftHoursMap = new HashMap<>();

    /** 整日停产后的首个开班班次集合 */
    private Set<Integer> startupShiftOrderSet = new HashSet<>();

    /** 当前日停产需求重分配证据，key=目标班次 */
    private Map<Integer, Map<String, Object>> currentDayShutdownEvidenceMap = new LinkedHashMap<>();

    /** 班次时间窗口映射，key=班次顺序(1~6)，来自 T_TM_SHIFT_CONFIG */
    private Map<Integer, TmShiftTimeWindow> shiftTimeWindowMap = new HashMap<>();

    /** 成型计划已加载但按 TM_FORMING_SHIFT_OFFSET 偏移后无可排程班次时的细化提示，供响应阶段直接使用；为空表示未触发 */
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
     * 追加指定班次的延后完整过程事件，渲染时位于该班次普通事件之后。
     *
     * @param shiftOrder 班次顺序
     * @param event      完整过程事件
     */
    public void appendDeferredShiftFullProcessTrace(Integer shiftOrder, ScheduleProcessTraceEvent event) {
        this.getOrCreateProcessTraceBuffer().appendDeferredShiftFull(shiftOrder, event);
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
        return this.getOrCreateProcessTraceBuffer().getEventCount();
    }

    /**
     * 获取本次自动排程已收集的中文过程日志。
     *
     * @return 中文过程日志文本
     */
    public String getProcessLogText() {
        return this.getOrCreateProcessTraceBuffer().render();
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
    public TmParamValue getParam(String paramCode) {
        TmParamValue paramValue = paramMap.get(paramCode);
        if (paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_PARAM_EMPTY.getDefaultMessage() + ":" + paramCode);
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
    public ScheduleTaskLinkedList<TmTaskDraft> getTaskChain(String machineCode, Integer shiftOrder) {
        if (scheduleDate == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }
        return taskChainGroup.get(machineCode, DateUtil.toLocalDateTime(scheduleDate).toLocalDate(), shiftOrder);
    }

    public void setParamMap(Map<String, TmParamValue> paramMap) {
        this.paramMap = paramMap == null ? new HashMap<>() : paramMap;
    }

    public void setTaskDraftList(List<TmTaskDraft> taskDraftList) {
        this.taskDraftList = taskDraftList == null ? new ArrayList<>() : taskDraftList;
    }

    public void setSourceTaskDraftList(List<TmTaskDraft> sourceTaskDraftList) {
        this.sourceTaskDraftList = sourceTaskDraftList == null ? new ArrayList<>() : sourceTaskDraftList;
    }

    public void setPlanTaskGroupMap(Map<String, TmPlanTaskGroup> planTaskGroupMap) {
        this.planTaskGroupMap = planTaskGroupMap == null ? new LinkedHashMap<>() : planTaskGroupMap;
    }

    public void setTaskChainGroup(MachineShiftTaskChain<TmTaskDraft> taskChainGroup) {
        this.taskChainGroup = taskChainGroup == null ? new MachineShiftTaskChain<>() : taskChainGroup;
    }

    public void setTaskNodeIndex(Map<String, ScheduleTaskNode<TmTaskDraft>> taskNodeIndex) {
        this.taskNodeIndex = taskNodeIndex == null ? new HashMap<>() : taskNodeIndex;
    }

    /**
     * 注册任务链节点索引。
     *
     * @param taskId 任务标识
     * @param node   任务链节点
     */
    public void registerTaskNode(String taskId, ScheduleTaskNode<TmTaskDraft> node) {
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
    public ScheduleTaskNode<TmTaskDraft> getTaskNode(String taskId) {
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

    public void setSnapshotMap(Map<String, TmSnapshotBuildResult> snapshotMap) {
        this.snapshotMap = snapshotMap == null ? new HashMap<>() : snapshotMap;
    }

    public void setRuleTraceMap(Map<String, TmRuleTrace> ruleTraceMap) {
        this.ruleTraceMap = ruleTraceMap == null ? new HashMap<>() : ruleTraceMap;
    }

    public void setStockForecastMap(Map<String, TmStockForecast> stockForecastMap) {
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

    public void setIssueCollector(TmAutoScheduleIssueCollector issueCollector) {
        this.issueCollector = issueCollector == null ? new TmAutoScheduleIssueCollector() : issueCollector;
    }
    public void setMachinePredecessorMap(Map<String, TmTaskPredecessor> machinePredecessorMap) {
        this.machinePredecessorMap = machinePredecessorMap == null ? new HashMap<>() : machinePredecessorMap;
    }

    public void setCandidateTraceMap(Map<String, List<TmMachineCandidate>> candidateTraceMap) {
        this.candidateTraceMap = candidateTraceMap == null ? new HashMap<>() : candidateTraceMap;
    }

    public void setShiftHoursMap(Map<Integer, BigDecimal> shiftHoursMap) {
        this.shiftHoursMap = shiftHoursMap == null ? new HashMap<>() : shiftHoursMap;
    }

    public void setStartupShiftOrderSet(Set<Integer> startupShiftOrderSet) {
        this.startupShiftOrderSet = startupShiftOrderSet == null ? new HashSet<>() : startupShiftOrderSet;
    }

    public void setShiftTimeWindowMap(Map<Integer, TmShiftTimeWindow> shiftTimeWindowMap) {
        this.shiftTimeWindowMap = shiftTimeWindowMap == null ? new HashMap<>() : shiftTimeWindowMap;
    }

    public void setLossRuleList(List<TmLossRule> lossRuleList) {
        this.lossRuleList = lossRuleList == null ? new ArrayList<>() : lossRuleList;
    }
}
