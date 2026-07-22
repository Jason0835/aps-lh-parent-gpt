package com.zlt.aps.tc.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
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
    /** 最终机台班次任务链。 */
    private MachineShiftTaskChain<TcManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
}
