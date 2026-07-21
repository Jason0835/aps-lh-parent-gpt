package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 新增规格提前生产规则解析结果。
 */
@Data
@Builder
public class Cd15NewSpecAdvanceResult {

    /** 已排除提前需求的计划需求视图，不改变原始成型需求。 */
    private List<Cd15DemandShift> adjustedDemandShifts;
    /** 命中新增规格的提前生产证据，按钢带代号分组。 */
    private Map<String, Cd15NewSpecAdvanceInfo> advanceInfoBySteelStrip;
}
