package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** CD15排程结果使用的钢带成型来源追溯信息。 */
@Data
@Builder
public class Cd15SteelStripSourceTrace {

    /** 钢带代码。 */
    private String steelStripCode;
    /** 去重排序后的成型批次号。 */
    private String cxBatchNo;
    /** 去重排序后的成型机台编码集合。 */
    private String cxMachineCodes;
    /** 相关胎胚月计划剩余量合计。 */
    private BigDecimal planSurplusQty;
}
