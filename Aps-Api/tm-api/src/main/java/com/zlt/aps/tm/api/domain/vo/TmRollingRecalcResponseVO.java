package com.zlt.aps.tm.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎面自动滚动重算响应。
 */
@Data
@ApiModel(value = "胎面自动滚动重算响应", description = "返回一次滚动重算的幂等键、调整统计和跳过摘要")
public class TmRollingRecalcResponseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 运行幂等键。 */
    private String runKey;

    /** SUCCESS 或 SKIPPED。 */
    private String status;

    /** 目标排程日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleDate;

    /** 目标逻辑班次。 */
    private Integer targetShiftOrder;

    /** 实际调整胎面数量。 */
    private Integer adjustedTreadCount = 0;

    /** 实际受影响结果行数量。 */
    private Integer affectedResultCount = 0;

    /** 跳过胎面数量。 */
    private Integer skippedTreadCount = 0;

    /** 调整前目标班计划总量。 */
    private BigDecimal beforePlanQty = BigDecimal.ZERO;

    /** 调整后目标班计划总量。 */
    private BigDecimal afterPlanQty = BigDecimal.ZERO;

    /** 按原因汇总的跳过数量。 */
    private Map<String, Integer> skippedReasonSummary = new LinkedHashMap<>();

    /** 低敏追踪标识。 */
    private String traceId;
}
