package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自动排程使用的库存窄模型；全量排程承载原库存，定时滚动承载目标班次库存。
 */
@Data
@Builder
public class Cd90StockSource {

    /** 库存快照日期。 */
    private LocalDate stockDate;
    /** 库存基准时间；班次滚动只扣除此时间之后的成型消耗。 */
    private LocalDateTime snapshotTime;
    /** 帘布代码。 */
    private String clothCode;
    /** 可用库存数量，单位米。 */
    private BigDecimal stockQuantity;
}
