package com.zlt.aps.tm.engine.domain;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
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

    /** 排程日期 */
    private Date scheduleDate;

    /** 操作人 */
    private String operator;

    /** 参数快照，key=参数编码 */
    private Map<String, TmParamValue> paramMap = new HashMap<>();

    /** 待排任务草稿列表 */
    private List<TmTaskDraft> taskDraftList = new ArrayList<>();

    /** 机台班次任务链集合 */
    private MachineShiftTaskChain<TmTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();

    /** 任务链节点索引，key=任务ID或业务键 */
    private Map<String, ScheduleTaskNode<TmTaskDraft>> taskNodeIndex = new HashMap<>();

    /** 解释快照，key=任务业务键 */
    private Map<String, TmSnapshotBuildResult> snapshotMap = new HashMap<>();

    /** 规则证据，key=任务业务键 */
    private Map<String, TmRuleTrace> ruleTraceMap = new HashMap<>();

    /** 本次落库转换汇总 */
    private TmPersistResult persistResult;

    /** 库存预测结果，key=胎面编码 */
    private Map<String, TmStockForecast> stockForecastMap = new HashMap<>();

    /** 胎面班初滚动库存状态，key=胎面编码；初值为14点预计库存，任务完成后回写交接班预计库存 */
    private Map<String, BigDecimal> remainingStockMap = new HashMap<>();

    /** 工厂可用机台候选列表，由数据加载层填充，供机台分配步骤过滤评分使用 */
    private List<TmMachineCandidate> machineCandidateList = new ArrayList<>();

    /** 当前排程日一班开始前的同机台前置任务快照，key=机台编码 */
    private Map<String, TmTaskPredecessor> machinePredecessorMap = new HashMap<>();

    /** 单任务候选机台过滤和评分快照，key=任务业务键 */
    private Map<String, List<TmMachineCandidate>> candidateTraceMap = new HashMap<>();

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

    public void setMachinePredecessorMap(Map<String, TmTaskPredecessor> machinePredecessorMap) {
        this.machinePredecessorMap = machinePredecessorMap == null ? new HashMap<>() : machinePredecessorMap;
    }

    public void setCandidateTraceMap(Map<String, List<TmMachineCandidate>> candidateTraceMap) {
        this.candidateTraceMap = candidateTraceMap == null ? new HashMap<>() : candidateTraceMap;
    }
}
