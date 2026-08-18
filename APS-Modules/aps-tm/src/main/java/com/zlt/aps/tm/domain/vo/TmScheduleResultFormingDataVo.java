package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面排程结果导出的成型计划汇总数据。
 *
 * <p>按胎面编码汇总成型计划量和对应成型机台，供专用导出模板展示使用。</p>
 */
@Data
public class TmScheduleResultFormingDataVo {

    /** 成型计划量。 */
    private BigDecimal cxPlanQty;

    /** 成型机台编码，多个机台使用英文逗号分隔。 */
    private String cxMachineCode;
}
