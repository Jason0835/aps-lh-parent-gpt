package com.zlt.aps.tq.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleTaskConstraint;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈人工滚动运行态上下文。
 */
@Data
public class TqManualRollingContext {

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

    /** 机台班产定额，key=机台编码 */
    private Map<String, BigDecimal> machineCapacityMap = new LinkedHashMap<>();

    /** 机台班次有效基础产能，key=机台编码|班次顺序（胎圈无单独班次产能时与 machineCapacity 一致） */
    private Map<String, BigDecimal> shiftCapacityMap = new LinkedHashMap<>();

    /** 机台班次维修小时数，key=机台编码|班次顺序 */
    private Map<String, BigDecimal> maintenanceHoursMap = new LinkedHashMap<>();

    /** 机台规格生产速度，key=机台编码|胎圈编码 */
    private Map<String, BigDecimal> machineSpecSpeedMap = new LinkedHashMap<>();

    /** 当前排程日一班开始前的同机台任务，key=机台编码 */
    private Map<String, TqManualTaskDraft> predecessorTaskMap = new LinkedHashMap<>();

    /** 人工滚动任务链约束参数（规格切换时长等） */
    private ScheduleTaskConstraint constraintConfig = new ScheduleTaskConstraint();

    /** 当前全部任务片段 */
    private List<TqManualTaskDraft> taskList = new ArrayList<>();

    /** 最终机台班次任务链 */
    private MachineShiftTaskChain<TqManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
}
