package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 精度计划实体
 */
@Data
public class PrecisionPlan {

    /** ID */
    private Long id;

    /** 机台编码 */
    private String machineCode;

    /** 排程日期 */
    private java.util.Date scheduleDate;

    /** 精度持续时长(小时) */
    private BigDecimal accuracyDuration;

    /** 精度类型 */
    private String accuracyType;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
