package com.zlt.aps.tm.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintConfig;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;

/**
 * 胎面人工滚动运行态上下文。
 */
@Data
public class TmManualRollingContext {

    /** 工厂编码 */
    private String factoryCode;

    /** 批次号 */
    private String batchNo;

    /** 排程日期 */
    private Date scheduleDate;

    /** 追踪标识 */
    private String traceId;

    /** 操作人 */
    private String operator;

    /** 机台班产，key=机台编码 */
    private Map<String, BigDecimal> machineCapacityMap = new LinkedHashMap<>();

    /** 机台班次有效基础产能，key=机台编码|班次顺序 */
    private Map<String, BigDecimal> shiftCapacityMap = new LinkedHashMap<>();

    /** 机台班次维修小时数，key=机台编码|班次顺序 */
    private Map<String, BigDecimal> maintenanceHoursMap = new LinkedHashMap<>();

    /** 机台规格生产速度，key=机台编码|胎面编码 */
    private Map<String, BigDecimal> machineSpecSpeedMap = new LinkedHashMap<>();

    /** 当前排程日一班开始前的同机台任务，key=机台编码 */
    private Map<String, TmManualTaskDraft> predecessorTaskMap = new LinkedHashMap<>();

    /** 人工滚动任务链约束参数 */
    private ScheduleConstraintConfig constraintConfig = new ScheduleConstraintConfig();

    /** 当前批次人工重算前的全局可用工装数量 */
    private BigDecimal initialAvailableToolQty;

    /** 当前全部任务片段 */
    private List<TmManualTaskDraft> taskList = new ArrayList<>();

    /** 最终机台班次任务链 */
    private MachineShiftTaskChain<TmManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
}
