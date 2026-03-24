package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

/**
 * 工作日历实体
 */
@Data
public class WorkCalendar {

    /** ID */
    private Long id;

    /** 生产日期 */
    private java.util.Date productionDate;

    /** 机台编码 */
    private String machineCode;

    /** 班次标识 */
    private String shiftFlag;

    /** 日期标识(开产/停产/正常) */
    private String dayFlag;

    /** 创建时间 */
    private java.util.Date createTime;

    /** 更新时间 */
    private java.util.Date updateTime;
}
