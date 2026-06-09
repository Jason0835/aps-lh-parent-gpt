package com.zlt.aps.tq.engine.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机台寸口对应关系VO（排程引擎专用）。
 *
 * <p>对应 T_TQ_MACHINE_CHUCK 表，用于寸口过滤策略。</p>
 *
 * @author APS
 */
@Data
public class TqMachineChuckVo {

    /** 机台ID */
    private Long machineId;

    /** 英寸尺寸 */
    private BigDecimal inchSize;
}
