package com.zlt.aps.tq.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 胎圈排程结果 Excel 解析结果。
 *
 * @author APS
 */
@Data
public class TqScheduleResultExcelParseResult {

    /** 从标题解析出的排程日期。 */
    private Date scheduleDate;

    /** Excel 有效明细行。 */
    private List<TqScheduleResultVo> rowList;
}
