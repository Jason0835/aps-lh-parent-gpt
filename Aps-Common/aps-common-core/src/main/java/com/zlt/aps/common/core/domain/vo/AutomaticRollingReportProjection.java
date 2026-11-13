package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 从计划分析报表八班视图提取的自动滚动库存及未来逐班需求。
 */
@Data
public class AutomaticRollingReportProjection {

    /**
     * 被调整物理班次的报表预计库存。
     */
    private BigDecimal expectedStock;

    /**
     * 用于投影的计划分析报表排程日期。
     */
    private String reportScheduleDate;

    /**
     * 目标物理班次在八班报表中的列序号，从 0 开始。
     */
    private Integer targetColumnIndex;

    /**
     * 未来需求窗口在八班报表中的起始列序号，从 0 开始，对应 futureDemandQtyList 的第 0 项。
     */
    private Integer demandStartColumnIndex;

    /**
     * 报表使用的前一日六点净库存。
     */
    private BigDecimal sixClockStockQty;

    /**
     * 预计库存递推中从该物理班次扣减的实际需求。
     */
    private BigDecimal targetDemandQty;

    /**
     * 预计库存递推中结转到目标物理班次的前一班计划量。
     */
    private BigDecimal previousShiftPlanQty;

    /**
     * 从下一物理班开始的实际需求；缺失数量补零，来源状态另外保留用于审计。
     */
    private List<BigDecimal> futureDemandQtyList = new ArrayList<>();

    /**
     * 目标物料在本次报表输入中存在有效需求来源。
     */
    private boolean demandSourcePresent;

    /**
     * 报表中存在该物料的需求行；某班无成型计划时该班为null，但仍属于有需求数据。
     */
    private boolean demandRowPresent;

    /**
     * 报表六点净库存映射中存在该物料；缺失时按零基数递推，但此标记保持false。
     */
    private boolean stockSourcePresent;

    /**
     * 原始需求缺失或部分来源数量为空的报表列索引（从0开始），对应缺失部分按零计算。
     */
    private List<Integer> zeroDefaultDemandColumnIndexes = new ArrayList<>();

    /**
     * 报表是否同时包含目标班与触发边界班。
     */
    private boolean physicalWindowMapped;
}
