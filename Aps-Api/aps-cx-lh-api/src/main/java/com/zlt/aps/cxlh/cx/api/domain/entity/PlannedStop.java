package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

/**
 * 计划停机实体
 */
@Data
public class PlannedStop {

    /** ID */
    private Long id;

    /** 机台编码 */
    private String machineCode;

    /** 停机日期 */
    private java.util.Date stopDate;

    /** 班次编码 */
    private String shiftCode;

    /** 停机开始时间 */
    private java.util.Date startTime;

    /** 停机结束时间 */
    private java.util.Date endTime;

    /** 停机原因 */
    private String stopReason;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
