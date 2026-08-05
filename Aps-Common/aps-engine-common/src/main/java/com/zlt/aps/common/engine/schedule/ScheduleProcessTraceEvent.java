package com.zlt.aps.common.engine.schedule;

/**
 * 自动排程中文过程事件。
 *
 * <p>每个事件必须形成“来源、输入、规则、代入、结果、去向”的闭环，业务模块只负责提供
 * 当次计算的真实证据，公共缓冲器负责统一编号和格式化。</p>
 */
public class ScheduleProcessTraceEvent {

    /** 阶段中文名。 */
    private final String stageName;

    /** 任务业务键；批次级事件使用“批次级”。 */
    private final String taskBusinessKey;

    /** 规则中文名。 */
    private final String ruleName;

    /** 数据来源。 */
    private final String dataSource;

    /** 原始输入。 */
    private final String originalInput;

    /** 规则说明。 */
    private final String ruleDescription;

    /** 代入计算或状态变更过程。 */
    private final String substitutionProcess;

    /** 计算或状态变更结果。 */
    private final String calculationResult;

    /** 结果去向。 */
    private final String resultDestination;

    /**
     * 创建完整中文过程事件。
     *
     * @param stageName          阶段中文名
     * @param taskBusinessKey    任务业务键
     * @param ruleName           规则中文名
     * @param dataSource         数据来源
     * @param originalInput      原始输入
     * @param ruleDescription    规则说明
     * @param substitutionProcess 代入计算或状态变更过程
     * @param calculationResult  计算或状态变更结果
     * @param resultDestination  结果去向
     */
    public ScheduleProcessTraceEvent(String stageName, String taskBusinessKey, String ruleName,
                                     String dataSource, String originalInput, String ruleDescription,
                                     String substitutionProcess, String calculationResult,
                                     String resultDestination) {
        this.stageName = stageName;
        this.taskBusinessKey = taskBusinessKey;
        this.ruleName = ruleName;
        this.dataSource = dataSource;
        this.originalInput = originalInput;
        this.ruleDescription = ruleDescription;
        this.substitutionProcess = substitutionProcess;
        this.calculationResult = calculationResult;
        this.resultDestination = resultDestination;
    }

    public String getStageName() {
        return stageName;
    }

    public String getTaskBusinessKey() {
        return taskBusinessKey;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getDataSource() {
        return dataSource;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public String getSubstitutionProcess() {
        return substitutionProcess;
    }

    public String getCalculationResult() {
        return calculationResult;
    }

    public String getResultDestination() {
        return resultDestination;
    }
}
