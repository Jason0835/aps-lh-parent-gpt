package com.zlt.aps.lh.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 硫化精度保养可行性判定结果。
 *
 * <p>由精度保养判定方法返回：能做精度时携带占用时长和开始/结束时间，
 * 不能做精度时占用时长为0、时间字段为null。主流程依据机台编码和起止时间
 * 自行判断与换模、清洗、停机的重叠。</p>
 *
 * @author APS
 */
@Data
public class PrecisionMaintenanceJudgeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 机台编码（单控L/R为运行态机台编码，主流程重叠判断用） */
    private String machineCode;

    /** 精度保养占用时长（小时）；0=不能做精度。2小时首件首检不含在内，由排程主流程后续追加 */
    private int durationHours;

    /** 精度保养开始时间（执行日固定保养开始时刻，默认08:00）；不能做精度时为null */
    private Date precisionStartTime;

    /** 精度保养结束时间（开始时间+占用时长）；不能做精度时为null */
    private Date precisionEndTime;

    /** 命中的硫化精度计划主键（排程完成后回填SCHEDULE_DATE用）；无计划或不能做精度时为null */
    private Long precisionPlanId;

    /** 是否强制下机（长期在机超过30天且进入3天检查期、无法自然收尾的场景） */
    private boolean forceDown;
}
