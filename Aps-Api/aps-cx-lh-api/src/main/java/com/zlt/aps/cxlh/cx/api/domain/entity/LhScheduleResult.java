package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 硫化排程结果实体
 */
@Data
public class LhScheduleResult {

    /** 计划号 */
    private String planNo;

    /** 排程日期 */
    private java.util.Date scheduleDate;

    /** 硫化机编码 */
    private String lhMachineCode;

    /** 胎胚编码 */
    private String embryoCode;

    /** 物料编码 */
    private String materialCode;

    /** 结构编码 */
    private String structureCode;

    /** 需求数量 */
    private BigDecimal demandQty;

    /** 机台产能 */
    private BigDecimal machineCapacity;

    /** 班次编码 */
    private String shiftCode;

    /** 计划状态 */
    private String planStatus;
}
