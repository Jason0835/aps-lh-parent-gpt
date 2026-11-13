package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 自动滚动数量分配使用的单条结果行快照。
 */
@Data
public class AutomaticRollingPlanRow {

    /**
     * 结果行ID，用于稳定排序。
     */
    private Long resultId;

    /**
     * 本行对应的生产机台编码，供滚动审计追溯损耗匹配输入。
     */
    private String machineCode;

    /**
     * 目标班现有计划量，空值按零处理。
     */
    private BigDecimal planQty;

    /**
     * 主侧目标班完成量；同步侧分配时固定传零。
     */
    private BigDecimal finishQty;

    /**
     * 目标班顺序，空值排在有顺序的结果行之后。
     */
    private Integer sequence;

    /**
     * 本次分配后的计划量。
     */
    private BigDecimal allocatedPlanQty;

    /**
     * 完成量下限保护后的基础计划量，用于区分结算前目标与最终计划量。
     */
    private BigDecimal basePlanQty;

    /**
     * 本行对应的规则损耗率，百分比口径。
     */
    private BigDecimal lossRate;

    /**
     * 损耗率命中层级或默认参数来源。
     */
    private String lossMatchLevel;

    /**
     * 本行有效卷长；无效时保留损耗后数量且不取整。
     */
    private BigDecimal curlLength;

    /**
     * 是否收尾行；收尾计划只补损耗、不取整。
     */
    private boolean tailTask;

    /**
     * 本次损耗补偿量。
     */
    private BigDecimal lossAddQty;

    /**
     * 本次向上取整补量。
     */
    private BigDecimal curlRoundAddQty;

    /**
     * 未向上取整时的原因；有效卷长缺失或收尾规则会写入说明码。
     */
    private String curlRoundSkipReason;

    /**
     * 最终结算数量。
     */
    private BigDecimal finalPlanQty;
}
