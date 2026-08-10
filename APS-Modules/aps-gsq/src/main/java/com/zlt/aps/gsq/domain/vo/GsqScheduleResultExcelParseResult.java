package com.zlt.aps.gsq.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程结果 Excel 解析结果。
 *
 * <p>解析专用模板 {@code excelModel/gsqScheduleResult.xlsx} 后的结果，包含标题中的排程日期
 * 和逐行解析出的有效明细行，对齐胎面 {@code TmScheduleResultExcelParseResult}。</p>
 *
 * @author APS
 */
@Data
public class GsqScheduleResultExcelParseResult {

    /** 从标题解析出的排程日期。 */
    private Date scheduleDate;

    /** Excel 有效明细行。 */
    private List<GsqScheduleResultVo> rowList;
}