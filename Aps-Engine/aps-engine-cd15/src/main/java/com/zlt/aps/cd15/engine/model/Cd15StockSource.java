package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自动排程使用的6点库存窄模型。
 */
@Data
@Builder
public class Cd15StockSource {

    /** 库存快照日期。 */
    private LocalDate stockDate;
    /** 库存基准时间；班次滚动只扣除此时间之后的成型消耗。 */
    private LocalDateTime snapshotTime;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 可用库存数量，单位米。 */
    private BigDecimal stockQuantity;
}
