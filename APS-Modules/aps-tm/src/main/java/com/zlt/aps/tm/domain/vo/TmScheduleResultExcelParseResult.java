package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果 Excel 解析结果。
 */
@Data
public class TmScheduleResultExcelParseResult {

    /** 从标题解析出的排程日期。 */
    private Date scheduleDate;

    /** Excel 有效明细行。 */
    private List<TmScheduleResultExcelRow> rowList;
}
