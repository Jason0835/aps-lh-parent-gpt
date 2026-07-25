package com.zlt.aps.tc.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 胎侧排程结果 Excel 解析结果。
 */
@Data
public class TcScheduleResultExcelParseResult {

    /** 从标题解析出的排程日期。 */
    private Date scheduleDate;

    /** Excel 有效明细行。 */
    private List<TcScheduleResultVo> rowList;
}
