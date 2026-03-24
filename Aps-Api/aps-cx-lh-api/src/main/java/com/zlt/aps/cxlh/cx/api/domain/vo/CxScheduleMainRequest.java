package com.zlt.aps.cxlh.cx.api.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成型排产主请求参数
 * @author 排产系统
 * @date 2026-03-23
 */
@Data
public class CxScheduleMainRequest {
    /** 排产日期 */
    private Date scheduleDate;
    /** 班次编码（关联CxShiftConfig的shiftCode） */
    private String shiftCode;
    /** 已完成量（重排时使用） */
    private BigDecimal hasFinishQty;
    /** 待排产量 */
    private BigDecimal toScheduleQty;
    /** 排产批次号 */
    private String batchNo;
    /** 是否重排：0-否，1-是 */
    private Integer isRearrange;
}