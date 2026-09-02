package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * TM/TC 库存预测产品工装参数查询行。
 */
@Data
public class InventoryPredictToolRow {

    /** 参与排程的工序产品编码。 */
    private String processCode;

    /** 产品配置的卷曲长度，单位米/套。 */
    private BigDecimal curlLength;
}
