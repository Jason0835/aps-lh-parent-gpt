package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 当前班次机台试算基础数据只读快照。
 */
@Data
@Builder
public class Cd15MachineResourceSnapshot {

    private List<Cd15MachineResource> machines;
    private List<Cd15MachineRollBinding> bindings;
    private List<Cd15MachineRestriction> restrictions;
    private List<Cd15LossRateRule> lossRateRules;
    /** 各裁断角度支持的最大分裁总宽度。 */
    private Map<String, BigDecimal> angleWidthMaxByAngle;
}
