package com.zlt.aps.cxlh.cx.api.domain.dto;

import lombok.Data;

import java.util.Date;

/**
 * 排程请求DTO
 */
@Data
public class ScheduleRequestDTO {

    /** 排程日期 */
    private Date scheduleDate;

    /** 班次编码 */
    private String shiftCode;

    /** 版本号(调整时使用) */
    private String versionNo;

    /** 是否强制重新排程 */
    private Boolean forceReschedule;

    /** 指定机台列表(可选) */
    private String[] machineCodes;

    /** 指定结构列表(可选) */
    private String[] structureCodes;
}
