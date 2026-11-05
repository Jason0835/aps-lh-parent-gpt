package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 只读备库需求解析结果，保存需求与来源证据，不携带数据库实体。
 */
@Data
public class RollingStockUpDemandSnapshot {
    /**
     * 当前需求已消耗后，待保障的未来窗口需求米数。
     */
    private BigDecimal demandQty = BigDecimal.ZERO;
    /**
     * 来源及请求窗口是否完整，允许有明确规则的末三班外推。
     */
    private boolean complete = false;
    /**
     * 缺失或异常原因，不抛给原滚动保供状态。
     */
    private String skipReason;
    /**
     * 来源班次、估算标记、备库深度和数量封顶证据。
     */
    private Map<String, Object> evidence = new LinkedHashMap<>();
}
