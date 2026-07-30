package com.zlt.aps.tc.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintConfig;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * 胎侧人工滚动运行态上下文。
 */
@Data
public class TcManualRollingContext {
    /** 工厂编码。 */
    private String factoryCode;
    /** 排程日期。 */
    private Date scheduleDate;
    /** 批次号。 */
    private String batchNo;
    /** 当前任务片段。 */
    private List<TcManualTaskDraft> taskList = new ArrayList<>();
    /** 班次产能，键格式为 machineCode|shiftOrder。 */
    private Map<String, BigDecimal> shiftCapacityMap = new LinkedHashMap<>();
    /** 人工滚动任务链约束参数。 */
    private ScheduleConstraintConfig constraintConfig = new ScheduleConstraintConfig();
    /** 机台规格生产速度，key=机台编码|胎侧编码。 */
    private Map<String, BigDecimal> machineSpecSpeedMap = new LinkedHashMap<>();
    /** 当前批次人工重算前的全局可用工装数量。 */
    private BigDecimal initialAvailableToolQty;
    /** 当前批次工装池上限。 */
    private BigDecimal totalToolQty;
    /** 本次人工命令结算后的全局可用工装数量。 */
    private BigDecimal currentAvailableToolQty;
    /** 当前排程日一班开始前的同机台任务，key=机台编码。 */
    private Map<String, TcManualTaskDraft> predecessorTaskMap = new LinkedHashMap<>();
    /** 最终机台班次任务链。 */
    private MachineShiftTaskChain<TcManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
}
