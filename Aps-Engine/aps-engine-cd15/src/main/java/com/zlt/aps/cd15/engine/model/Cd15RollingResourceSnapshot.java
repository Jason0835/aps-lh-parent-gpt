package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CD15 逐班滚动资源快照，记录会随试排过程扣减的库存与大卷资源。
 */
@Data
@Builder
public class Cd15RollingResourceSnapshot {

    /** 按钢带代码汇总的 6 点库存米数。 */
    private Map<String, BigDecimal> stockMetersBySteelStrip;
    /** 按大卷代码分组的 GDYY 成熟库存。 */
    private Map<String, List<GdyyStock>> gdyyStocksByBigRoll;
}