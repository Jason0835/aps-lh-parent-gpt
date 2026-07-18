package com.zlt.aps.tm.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面自动滚动单规格调整指令。
 */
@Data
public class TmRollingAdjustment {

    /** 胎面编码。 */
    private String treadCode;

    /** 调整前目标班计划总量。 */
    private BigDecimal beforePlanQty;

    /** 调整后目标班计划总量。 */
    private BigDecimal targetPlanQty;

    /** UP 或 DOWN。 */
    private String direction;

    /** 结构化计算证据。 */
    private Map<String, Object> evidence = new LinkedHashMap<>();
}
