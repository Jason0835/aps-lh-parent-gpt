package com.zlt.aps.tc.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧排程结果导出的成型需求汇总数据。
 */
@Data
public class TcScheduleResultFormingDataVo {

    /** 胎侧编码。 */
    private String sidewallCode;

    /** 胎侧施工版本。 */
    private String constructionVersion;

    /** 成型计划量。 */
    private BigDecimal cxPlanQty;

    /** 成型余量。 */
    private BigDecimal cxRemainQty;

    /** 物料描述。 */
    private String materialDesc;

    /** 成型机台编码。 */
    private String cxMachineCode;
}
