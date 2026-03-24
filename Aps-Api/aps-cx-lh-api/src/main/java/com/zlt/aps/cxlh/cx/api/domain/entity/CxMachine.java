package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成型机实体
 */
@Data
public class CxMachine {

    /** 机台编码 */
    private String machineCode;

    /** 机台类型编码 */
    private String machineTypeCode;

    /** 机台名称 */
    private String machineName;

    /** 最大日产能 */
    private BigDecimal maxDayCapacity;

    /** 可用产能 */
    private BigDecimal availableCapacity;

    /** 精度计划标识 */
    private Boolean precisionFlag;

    /** 精度持续时长(小时) */
    private BigDecimal accuracyDuration;

    /** 开产后首班标识 */
    private Boolean firstShiftAfterOpen;

    /** 状态(0-停用 1-启用) */
    private Integer status;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
