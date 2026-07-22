package com.zlt.aps.tm.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
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

    /** 当前全部任务片段 */
    private List<TmManualTaskDraft> taskList = new ArrayList<>();

    /** 最终机台班次任务链 */
    private MachineShiftTaskChain<TmManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
}
