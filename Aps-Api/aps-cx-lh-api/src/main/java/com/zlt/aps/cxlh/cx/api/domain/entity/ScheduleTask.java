package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 排程任务实体
 */
@Data
public class ScheduleTask {

    /** 任务ID */
    private Long taskId;

    /** 结构编码 */
    private String structureCode;

    /** 物料编码 */
    private String materialCode;

    /** 胎胚编码 */
    private String embryoCode;

    /** 硫化需求数量 */
    private BigDecimal demandQty;

    /** 余量 */
    private BigDecimal surplusQty;

    /** 任务类型(CONTINUE-续作 NEW-新增) */
    private String taskType;

    /** 试制标识 */
    private Boolean trialFlag;

    /** 优先级 */
    private Integer priority;

    /** 原机台编码 */
    private String originalMachineCode;

    /** 当前机台编码 */
    private String currentMachineCode;

    /** 按计划收尾标识 */
    private Boolean finishAsPlanned;

    /** 可处理异常量 */
    private BigDecimal abnormalCapacity;

    /** 库存时长 */
    private BigDecimal inventoryDuration;

    public ScheduleTask() {
        this.trialFlag = false;
        this.finishAsPlanned = false;
    }
}
