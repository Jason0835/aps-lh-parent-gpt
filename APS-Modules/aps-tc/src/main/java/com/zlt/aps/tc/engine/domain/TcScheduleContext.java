package com.zlt.aps.tc.engine.domain;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.engine.service.TcAutoScheduleProgressListener;
import com.zlt.aps.tc.engine.service.collector.TcAutoScheduleIssueCollector;
import lombok.Data;

import java.math.BigDecimal;
import java.text.MessageFormat;
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
    private StringBuilder processLogBuffer = new StringBuilder(4096);

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

    /** 损耗率规则列表，由数据加载层填充，供机台分配步骤解析最终损耗率 */
    private List<TcLossRule> lossRuleList = new ArrayList<>();

    /** 计划量阶段计算出的首个任务工装池初始可用数量 */
    private BigDecimal initialAvailableToolQty;

    /** 机台分配阶段正在滚动使用的全局可用工装数量 */
    private BigDecimal currentAvailableToolQty;

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

    /** 班次小时数映射，key=班次顺序(1~6)，来自 T_TC_SHIFT_CONFIG */
    private Map<Integer, BigDecimal> shiftHoursMap = new HashMap<>();

    /** 整日停产后的首个开班班次集合 */
    private Set<Integer> startupShiftOrderSet = new HashSet<>();

    /** 当前日停产需求重分配证据，key=目标班次 */
    private Map<Integer, Map<String, Object>> currentDayShutdownEvidenceMap = new LinkedHashMap<>();

    /** 班次时间窗口映射，key=班次顺序(1~6)，来自 T_TC_SHIFT_CONFIG */
    private Map<Integer, TcShiftTimeWindow> shiftTimeWindowMap = new HashMap<>();

    /**
     * 追加一条中文自动排程过程日志。
     *
     * @param format 日志格式，使用 MessageFormat 占位符
     * @param args   日志参数
     */
    public void appendProcessLog(String format, Object... args) {
        if (StrUtil.isBlank(format)) {
            return;
        }
        if (processLogBuffer == null) {
            processLogBuffer = new StringBuilder(4096);
        }
        Object[] plainArgs = args == null ? new Object[0] : args;
        for (int index = 0; index < plainArgs.length; index++) {
            if (plainArgs[index] instanceof BigDecimal) {
                if (plainArgs == args) {
                    plainArgs = args.clone();
                }
                plainArgs[index] = ((BigDecimal) plainArgs[index]).toPlainString();
            }
        }
        processLogBuffer.append(MessageFormat.format(format, plainArgs)).append(System.lineSeparator());
    }

    /**
     * 获取本次自动排程已收集的中文过程日志。
     *
     * @return 中文过程日志文本
     */
    public String getProcessLogText() {
        return processLogBuffer == null ? "" : processLogBuffer.toString();
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

    public void setShiftTimeWindowMap(Map<Integer, TcShiftTimeWindow> shiftTimeWindowMap) {
        this.shiftTimeWindowMap = shiftTimeWindowMap == null ? new HashMap<>() : shiftTimeWindowMap;
    }

    public void setLossRuleList(List<TcLossRule> lossRuleList) {
        this.lossRuleList = lossRuleList == null ? new ArrayList<>() : lossRuleList;
    }
}
