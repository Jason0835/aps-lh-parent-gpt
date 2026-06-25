package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 自动排程使用的6点库存窄模型。
 */
@Data
@Builder
public class Cd90StockSource {

    /** 库存快照日期。 */
    private LocalDate stockDate;
    /** 帘布代码。 */
    private String clothCode;
    /** 可用库存数量，单位米。 */
    private BigDecimal stockQuantity;
}
